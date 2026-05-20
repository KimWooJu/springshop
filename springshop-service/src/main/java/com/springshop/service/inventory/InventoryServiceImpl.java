package com.springshop.service.inventory;

import com.springshop.common.exception.ErrorCode;
import com.springshop.common.exception.InventoryException;
import com.springshop.common.exception.ResourceNotFoundException;
import com.springshop.domain.inventory.Inventory;
import com.springshop.domain.inventory.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 재고 서비스 구현.
 *
 * <p>주요 동시성 전략:</p>
 * <ul>
 *   <li>예약/해제는 낙관적 락(JPA @Version) 기반. 충돌 시 최대 3 회 재시도.</li>
 *   <li>조정/입고와 같은 단발성 작업은 비관적 락 활용 ({@code findWithPessimisticLockById}).</li>
 *   <li>다수 상품 예약 확정은 {@link StructuredTaskScope} 로 병렬 처리.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceImpl.class);

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 50L;

    private final InventoryRepository inventoryRepository;
    private final StockAlertService stockAlertService;

    @Autowired
    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                StockAlertService stockAlertService) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.stockAlertService = Objects.requireNonNull(stockAlertService);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(Long productId, Long variantId, int quantity) {
        validateQuantity(quantity);
        Inventory inventory = findInventory(productId, variantId);

        executeWithRetry(() -> {
            Inventory current = inventoryRepository.findById(inventory.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", inventory.getId()));
            try {
                current.reserve(quantity);
            } catch (Inventory.InsufficientStockException e) {
                throw InventoryException.insufficient(productId, quantity, current.getAvailableQuantity());
            }
            inventoryRepository.save(current);
            log.info("재고 예약 성공: productId={}, qty={}, 남은가용={}",
                    productId, quantity, current.getAvailableQuantity());

            // 임계값 미만 알림
            checkAndAlertLowStock(current);
        }, "reserve(productId=" + productId + ", qty=" + quantity + ")");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(Long productId, Long variantId, int quantity) {
        validateQuantity(quantity);
        Inventory inventory = findInventory(productId, variantId);

        executeWithRetry(() -> {
            Inventory current = inventoryRepository.findById(inventory.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", inventory.getId()));
            try {
                current.release(quantity);
            } catch (IllegalArgumentException e) {
                throw InventoryException.negativeQuantity(quantity);
            }
            inventoryRepository.save(current);
            log.info("재고 예약 해제: productId={}, qty={}", productId, quantity);
        }, "release(productId=" + productId + ", qty=" + quantity + ")");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int confirmReservation(Long orderId, Map<Long, Integer> productQuantityMap) {
        Objects.requireNonNull(orderId, "orderId 필수");
        if (productQuantityMap == null || productQuantityMap.isEmpty()) {
            log.debug("재고 확정 대상 없음: orderId={}", orderId);
            return 0;
        }

        AtomicInteger confirmed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        try (var scope = StructuredTaskScope.open()) {
            for (Map.Entry<Long, Integer> entry : productQuantityMap.entrySet()) {
                Long productId = entry.getKey();
                Integer qty = entry.getValue();
                if (qty == null || qty <= 0) continue;

                scope.fork(() -> {
                    try {
                        confirmSingleProduct(productId, qty);
                        confirmed.incrementAndGet();
                    } catch (RuntimeException e) {
                        failed.incrementAndGet();
                        log.error("재고 확정 실패: orderId={}, productId={}, qty={}",
                                orderId, productId, qty, e);
                        throw e;
                    }
                    return null;
                });
            }
            scope.join();
            if (failed.get() > 0) {
                throw new InventoryException(ErrorCode.INVENTORY_LOCK_TIMEOUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InventoryException(ErrorCode.INVENTORY_LOCK_TIMEOUT);
        }

        log.info("재고 확정 완료: orderId={}, 성공={}, 실패={}", orderId, confirmed.get(), failed.get());
        return confirmed.get();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Inventory receiveStock(Long productId, Long variantId, Long warehouseId,
                                  int quantity, String reason, String operator) {
        validateQuantity(quantity);
        Optional<Inventory> existing = inventoryRepository.findUnique(productId, variantId, warehouseId);
        Inventory inventory = existing.orElseGet(() -> {
            Inventory created = Inventory.create(productId, variantId, warehouseId, 0, /* location */ null);
            return inventoryRepository.save(created);
        });

        int before = inventory.getTotalQuantity();
        inventory.receive(quantity, reason);
        Inventory saved = inventoryRepository.save(inventory);

        log.info("입고 완료: productId={}, variantId={}, warehouseId={}, before={}, after={}, by={}",
                productId, variantId, warehouseId, before, saved.getTotalQuantity(), operator);
        return saved;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Inventory adjustStock(Long inventoryId, int newTotal, String reason, String operator) {
        if (newTotal < 0) {
            throw InventoryException.negativeQuantity(newTotal);
        }
        Inventory inventory = inventoryRepository.findWithPessimisticLockById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", inventoryId));

        int before = inventory.getTotalQuantity();
        int delta = inventory.adjust(newTotal, reason);
        Inventory saved = inventoryRepository.save(inventory);

        log.info("재고 조정: inventoryId={}, before={}, after={}, delta={}, reason={}, by={}",
                inventoryId, before, saved.getTotalQuantity(), delta, reason, operator);
        checkAndAlertLowStock(saved);
        return saved;
    }

    @Override
    public boolean isAvailable(Long productId, Long variantId, int quantity) {
        if (quantity <= 0) return true;
        try {
            Inventory inventory = findInventory(productId, variantId);
            return inventory.isAvailable(quantity);
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    @Override
    public List<Inventory> getLowStockItems(int threshold) {
        if (threshold <= 0) {
            threshold = Inventory.DEFAULT_LOW_STOCK_THRESHOLD;
        }
        return inventoryRepository.findLowStock(threshold);
    }

    @Override
    public List<Inventory> getCriticalStockItems() {
        return inventoryRepository.findCriticalStock();
    }

    @Override
    public Inventory getInventory(Long inventoryId) {
        return inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", inventoryId));
    }

    @Override
    public long getAvailableStock(Long productId) {
        if (productId == null) return 0L;
        return inventoryRepository.sumAvailableByProductId(productId);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void confirmSingleProduct(Long productId, int quantity) {
        List<Inventory> inventories = inventoryRepository.findByProductId(productId);
        if (inventories.isEmpty()) {
            throw new ResourceNotFoundException("Inventory", productId);
        }
        // 예약량을 충당하는 인벤토리 들에서 확정 처리
        int remaining = quantity;
        for (Inventory inv : inventories) {
            if (remaining <= 0) break;
            int reservedHere = inv.getReservedQuantity();
            if (reservedHere <= 0) continue;
            int confirmHere = Math.min(reservedHere, remaining);
            try {
                inv.confirm(confirmHere);
                inventoryRepository.save(inv);
                remaining -= confirmHere;
            } catch (Exception e) {
                log.warn("재고 확정 일부 실패: invId={}, {}", inv.getId(), e.getMessage());
            }
        }
        if (remaining > 0) {
            log.warn("재고 확정 부족: productId={}, 잔여 미확정={}", productId, remaining);
        }
    }

    private Inventory findInventory(Long productId, Long variantId) {
        Objects.requireNonNull(productId, "productId 필수");
        List<Inventory> list = inventoryRepository.findByProductIdAndVariantId(productId, variantId);
        if (list.isEmpty()) {
            throw new ResourceNotFoundException("Inventory",
                    "product=" + productId + ", variant=" + variantId);
        }
        return list.get(0);
    }

    private void executeWithRetry(Runnable action, String context) {
        int attempt = 0;
        while (true) {
            try {
                action.run();
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= MAX_RETRY) {
                    log.error("낙관적 락 재시도 초과: {} (시도={})", context, attempt);
                    throw new InventoryException(ErrorCode.INVENTORY_LOCK_TIMEOUT);
                }
                log.warn("낙관적 락 충돌, 재시도: {} (시도={})", context, attempt);
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new InventoryException(ErrorCode.INVENTORY_LOCK_TIMEOUT);
                }
            }
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 합니다: " + quantity);
        }
    }

    private void checkAndAlertLowStock(Inventory inventory) {
        try {
            if (inventory.isCriticalStock()) {
                stockAlertService.sendCriticalAlert(inventory);
            } else if (inventory.isLowStock()) {
                stockAlertService.sendLowStockAlert(inventory);
            }
        } catch (Exception e) {
            log.warn("재고 부족 알림 실패 (무시): inventoryId={}, {}", inventory.getId(), e.getMessage());
        }
    }
}
