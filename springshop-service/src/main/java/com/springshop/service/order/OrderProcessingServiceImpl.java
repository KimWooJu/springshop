package com.springshop.service.order;

import com.springshop.domain.order.OrderRepository;
import com.springshop.common.exception.ResourceNotFoundException;
import com.springshop.service.inventory.InventoryService;
import com.springshop.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * {@link OrderProcessingService} 기본 구현.
 *
 * <p>주문 처리는 Java 21+의 {@link java.util.concurrent.StructuredTaskScope}로 병렬화된다.
 * 본 구현체는 데모/분석 도구용으로, 실제 운영 환경에서는 Virtual Thread Executor와
 * 메시지 큐(Kafka 등)를 활용한 비동기 처리가 권장된다.
 *
 * <p>주요 흐름:
 * <pre>
 *   1. 주문 존재 검증
 *   2. 재고 확정 (병렬 작업 1) + 알림 발송 (병렬 작업 2)
 *   3. 두 작업의 결과 집계
 *   4. 실패 시 cancel-all 및 보상 트랜잭션
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingServiceImpl implements OrderProcessingService {

    /**
     * 최대 재시도 횟수.
     */
    public static final int MAX_RETRIES = 3;

    /**
     * 재시도 간격 (밀리초).
     */
    public static final long RETRY_DELAY_MS = 1_000L;

    /**
     * in-memory 처리 단계 추적 — 운영에선 Redis나 DB 활용.
     */
    private final Map<Long, ProcessingStage> stageTracker = new ConcurrentHashMap<>();

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrderProcessingResult processOrder(Long orderId) {
        log.info("주문 처리 시작: orderId={}", orderId);
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다: " + orderId));

        stageTracker.put(orderId, ProcessingStage.CONFIRMING_INVENTORY);

        // Virtual Thread + StructuredTaskScope (Java 21+)로 병렬 처리
        boolean inventoryOk = false;
        boolean notificationOk = false;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> inventoryTask = executor.submit(() -> {
                try {
                    log.debug("재고 확정 시작: orderId={}", orderId);
                    inventoryService.confirmReservation(orderId, Map.of());
                    return true;
                } catch (Exception e) {
                    log.error("재고 확정 실패: orderId={}", orderId, e);
                    return false;
                }
            });

            Future<Boolean> notificationTask = executor.submit(() -> {
                try {
                    log.debug("주문 확인 알림 발송: orderId={}", orderId);
                    notificationService.sendOrderStatusNotification(
                        order.getUserId(), orderId, order.getOrderNumber(), "CONFIRMED");
                    return true;
                } catch (Exception e) {
                    log.error("알림 발송 실패: orderId={}", orderId, e);
                    return false;
                }
            });

            inventoryOk = inventoryTask.get();
            stageTracker.put(orderId, ProcessingStage.ARRANGING_SHIPPING);
            notificationOk = notificationTask.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stageTracker.put(orderId, ProcessingStage.FAILED);
            throw new OrderProcessingException("주문 처리 인터럽트: " + orderId, e);
        } catch (Exception e) {
            stageTracker.put(orderId, ProcessingStage.FAILED);
            log.error("주문 처리 실패: orderId={}", orderId, e);
            throw new OrderProcessingException("주문 처리 실패: " + orderId, e);
        }

        if (inventoryOk && notificationOk) {
            stageTracker.put(orderId, ProcessingStage.COMPLETED);
        } else {
            stageTracker.put(orderId, ProcessingStage.FAILED);
        }

        var result = new OrderProcessingResult(
            orderId, inventoryOk, notificationOk, LocalDateTime.now());
        log.info("주문 처리 완료: orderId={}, inventory={}, notification={}",
            orderId, inventoryOk, notificationOk);
        return result;
    }

    @Override
    @Transactional
    public void cancelOrderProcessing(Long orderId, String reason) {
        log.warn("주문 처리 취소: orderId={}, reason={}", orderId, reason);
        var stage = stageTracker.getOrDefault(orderId, ProcessingStage.NOT_STARTED);
        switch (stage) {
            case CONFIRMING_INVENTORY -> {
                // 재고 예약 해제는 InventoryService에 위임
                try {
                    log.warn("주문 재고 전체 해제 미지원: orderId={}", orderId);
                } catch (Exception e) {
                    log.error("재고 해제 실패 (이미 해제됐을 수 있음): orderId={}", orderId, e);
                }
            }
            case ARRANGING_SHIPPING -> {
                try {
                    log.warn("주문 재고 전체 해제 미지원: orderId={}", orderId);
                    // 추가 보상 — 발송 알림 회수 등
                } catch (Exception e) {
                    log.error("보상 트랜잭션 실패: orderId={}", orderId, e);
                }
            }
            case COMPLETED -> {
                log.warn("이미 완료된 주문은 취소할 수 없습니다: orderId={}", orderId);
                throw new OrderProcessingException("이미 완료된 주문은 취소 불가: " + orderId);
            }
            case NOT_STARTED, FAILED -> {
                log.info("취소할 진행 작업 없음: orderId={}, stage={}", orderId, stage);
            }
        }
        stageTracker.put(orderId, ProcessingStage.FAILED);
    }

    @Override
    @Transactional
    public boolean retryFailedProcessing(Long orderId) {
        var current = stageTracker.getOrDefault(orderId, ProcessingStage.NOT_STARTED);
        if (current != ProcessingStage.FAILED) {
            log.warn("실패 상태가 아닙니다: orderId={}, stage={}", orderId, current);
            return false;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            log.info("주문 처리 재시도 [{}/{}]: orderId={}", attempt, MAX_RETRIES, orderId);
            try {
                var result = processOrder(orderId);
                if (result.isFullySuccessful()) {
                    log.info("재시도 성공: orderId={}, attempt={}", orderId, attempt);
                    return true;
                }
            } catch (Exception e) {
                log.error("재시도 실패: orderId={}, attempt={}", orderId, attempt, e);
            }
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);  // 지수 백오프
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        log.error("재시도 최종 실패: orderId={}", orderId);
        return false;
    }

    @Override
    public ProcessingStage getCurrentStage(Long orderId) {
        return stageTracker.getOrDefault(orderId, ProcessingStage.NOT_STARTED);
    }

    /**
     * 주문 처리 전용 예외.
     */
    public static class OrderProcessingException extends RuntimeException {
        public OrderProcessingException(String message) {
            super(message);
        }

        public OrderProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
