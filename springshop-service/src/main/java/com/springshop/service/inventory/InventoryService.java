package com.springshop.service.inventory;

import com.springshop.domain.inventory.Inventory;

import java.util.List;
import java.util.Map;

/**
 * 재고 관리 핵심 서비스.
 *
 * <p>주문 생성/취소 시 재고 예약·해제, 입고/조정, 임계값 기반 부족 재고 조회 등을 제공한다.
 * 동시성 충돌은 낙관적 락 + 최대 3 회 재시도로 처리하며, 임계값 미달 시
 * 자동으로 알림 이벤트가 발행되도록 {@link StockAlertService} 와 협업한다.</p>
 *
 * <p>여러 상품에 대한 일괄 처리는 {@code StructuredTaskScope} 로 병렬화하여
 * 응답 지연을 최소화한다.</p>
 */
public interface InventoryService {

    /**
     * 재고 예약. 주문 생성 시 호출되며 동시성 충돌 시 재시도한다.
     */
    void reserve(Long productId, Long variantId, int quantity);

    /**
     * 재고 예약 해제. 주문 취소 시 호출된다.
     */
    void release(Long productId, Long variantId, int quantity);

    /**
     * 주문에 포함된 모든 재고 예약을 일괄 확정한다. 결제 완료 시 호출된다.
     */
    int confirmReservation(Long orderId, Map<Long, Integer> productQuantityMap);

    /**
     * 입고 처리. 입고 로그를 함께 기록한다.
     */
    Inventory receiveStock(Long productId, Long variantId, Long warehouseId,
                           int quantity, String reason, String operator);

    /**
     * 재고 조정. 실사 결과 반영 등.
     */
    Inventory adjustStock(Long inventoryId, int newTotal, String reason, String operator);

    /**
     * 사용 가능 여부 검증.
     */
    boolean isAvailable(Long productId, Long variantId, int quantity);

    /**
     * 임계값 이하 재고 목록.
     */
    List<Inventory> getLowStockItems(int threshold);

    /**
     * 위급 재고(임계값 미만) 목록.
     */
    List<Inventory> getCriticalStockItems();

    /**
     * 단일 인벤토리 조회.
     */
    Inventory getInventory(Long inventoryId);

    /**
     * 상품 ID 로 가용 재고 합산.
     */
    long getAvailableStock(Long productId);
}
