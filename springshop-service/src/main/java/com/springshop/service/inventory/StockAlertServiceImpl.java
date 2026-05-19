package com.springshop.service.inventory;

import com.springshop.domain.inventory.Inventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 재고 알림 서비스 구현.
 *
 * <p>알림은 도메인 이벤트로 발행하여, 알림 채널별 핸들러가 이를 구독한다.
 * 동일 인벤토리에 대해 짧은 시간 내 반복되는 알림은 in-memory 캐시로 억제하며,
 * 운영자가 임시 조정 시 별도의 suppression 등록을 통해 의도적으로 차단할 수 있다.</p>
 */
@Service
public class StockAlertServiceImpl implements StockAlertService {

    private static final Logger log = LoggerFactory.getLogger(StockAlertServiceImpl.class);

    /** 동일 알림 억제 윈도우. */
    private static final long DEFAULT_DEBOUNCE_SECONDS = 300L;

    private final ApplicationEventPublisher eventPublisher;

    /** inventoryId -> 마지막 알림 발송 시각. */
    private final Map<Long, Instant> lastSent = new ConcurrentHashMap<>();

    /** inventoryId -> 억제 만료 시각. */
    private final Map<Long, Instant> suppressedUntil = new ConcurrentHashMap<>();

    /** 통계 카운터. */
    private final AtomicInteger lowStockCount = new AtomicInteger();
    private final AtomicInteger criticalCount = new AtomicInteger();
    private final AtomicInteger suppressedCount = new AtomicInteger();

    @Autowired
    public StockAlertServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public void sendLowStockAlert(Inventory inventory) {
        if (inventory == null) return;
        if (isSuppressed(inventory.getId())) {
            suppressedCount.incrementAndGet();
            log.debug("LOW 알림 억제됨: inventoryId={}", inventory.getId());
            return;
        }
        if (!debounce(inventory.getId(), DEFAULT_DEBOUNCE_SECONDS)) {
            log.debug("LOW 알림 디바운스: inventoryId={}", inventory.getId());
            return;
        }

        var event = new StockAlertEvent(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                AlertLevel.WARN,
                "재고 부족: 가용=" + inventory.getAvailableQuantity()
        );
        eventPublisher.publishEvent(event);
        lowStockCount.incrementAndGet();
        log.warn("재고 부족 알림 발송: inventoryId={}, productId={}, 가용={}",
                inventory.getId(), inventory.getProductId(), inventory.getAvailableQuantity());
    }

    @Override
    public void sendCriticalAlert(Inventory inventory) {
        if (inventory == null) return;
        // CRITICAL은 디바운스 시간을 절반으로 줄여 빠르게 발송
        if (isSuppressed(inventory.getId())) {
            suppressedCount.incrementAndGet();
            log.debug("CRITICAL 알림 억제됨: inventoryId={}", inventory.getId());
            return;
        }
        if (!debounce(inventory.getId(), DEFAULT_DEBOUNCE_SECONDS / 3)) {
            log.debug("CRITICAL 알림 디바운스: inventoryId={}", inventory.getId());
            return;
        }

        var event = new StockAlertEvent(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getAvailableQuantity(),
                AlertLevel.CRITICAL,
                "위급 재고: 가용=" + inventory.getAvailableQuantity()
        );
        eventPublisher.publishEvent(event);
        criticalCount.incrementAndGet();
        log.error("위급 재고 알림 발송: inventoryId={}, productId={}, 가용={}",
                inventory.getId(), inventory.getProductId(), inventory.getAvailableQuantity());
    }

    @Override
    public int sendBatchAlerts(List<Inventory> inventories) {
        if (inventories == null || inventories.isEmpty()) return 0;

        int sent = 0;
        for (Inventory inventory : inventories) {
            try {
                if (inventory.isCriticalStock()) {
                    sendCriticalAlert(inventory);
                    sent++;
                } else if (inventory.isLowStock()) {
                    sendLowStockAlert(inventory);
                    sent++;
                }
            } catch (Exception e) {
                log.warn("재고 알림 발송 실패: inventoryId={}, {}", inventory.getId(), e.getMessage());
            }
        }
        log.info("재고 알림 일괄 발송 완료: 대상={}, 발송={}", inventories.size(), sent);
        return sent;
    }

    @Override
    public void suppressAlerts(Long inventoryId, long minutes) {
        if (inventoryId == null) return;
        if (minutes <= 0) {
            suppressedUntil.remove(inventoryId);
            log.info("알림 억제 해제: inventoryId={}", inventoryId);
            return;
        }
        Instant until = Instant.now().plusSeconds(minutes * 60L);
        suppressedUntil.put(inventoryId, until);
        log.info("알림 억제 설정: inventoryId={}, until={}", inventoryId, until);
    }

    @Override
    public AlertStatistics getDailyStatistics() {
        return new AlertStatistics(
                lowStockCount.get() + criticalCount.get(),
                lowStockCount.get(),
                criticalCount.get(),
                suppressedCount.get()
        );
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private boolean isSuppressed(Long inventoryId) {
        if (inventoryId == null) return false;
        Instant until = suppressedUntil.get(inventoryId);
        if (until == null) return false;
        if (Instant.now().isAfter(until)) {
            suppressedUntil.remove(inventoryId);
            return false;
        }
        return true;
    }

    private boolean debounce(Long inventoryId, long debounceSeconds) {
        if (inventoryId == null) return true;
        Instant now = Instant.now();
        Instant last = lastSent.get(inventoryId);
        if (last != null && now.minusSeconds(debounceSeconds).isBefore(last)) {
            return false;
        }
        lastSent.put(inventoryId, now);
        return true;
    }

    /** 알림 레벨. */
    public enum AlertLevel { WARN, CRITICAL }

    /** 재고 알림 이벤트. */
    public record StockAlertEvent(Long inventoryId,
                                  Long productId,
                                  int availableQuantity,
                                  AlertLevel level,
                                  String message) {}
}
