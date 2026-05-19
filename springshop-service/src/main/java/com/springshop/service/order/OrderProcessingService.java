package com.springshop.service.order;

import java.time.LocalDateTime;

/**
 * 주문 처리 협업 서비스.
 *
 * <p>{@link OrderService}는 주문 자체의 상태 전이를 담당하고, 본 서비스는 결제 완료 후
 * 후속 처리를 담당한다:
 * <ol>
 *   <li>재고 예약 확정 ({@code InventoryService.confirmReservation})</li>
 *   <li>배송 준비 알림 ({@code NotificationService.sendOrderConfirmed})</li>
 *   <li>주문 상태 → CONFIRMED 전이</li>
 * </ol>
 *
 * <p>병렬화는 {@code StructuredTaskScope}로 처리되어 실패 시 즉시 cancel-all로 롤백된다.
 */
public interface OrderProcessingService {

    /**
     * 주문 처리 결과 record.
     *
     * @param orderId             처리된 주문 ID
     * @param inventoryConfirmed  재고 예약 확정 여부
     * @param shippingArranged    배송 준비 완료 여부
     * @param processedAt         처리 완료 시각
     */
    record OrderProcessingResult(
        Long orderId,
        boolean inventoryConfirmed,
        boolean shippingArranged,
        LocalDateTime processedAt
    ) {
        public boolean isFullySuccessful() {
            return inventoryConfirmed && shippingArranged;
        }
    }

    /**
     * 주문 처리 실행.
     *
     * @param orderId 처리할 주문 ID
     * @return 처리 결과
     */
    OrderProcessingResult processOrder(Long orderId);

    /**
     * 주문 처리 취소 — 실패한 처리를 롤백한다.
     *
     * @param orderId 취소할 주문 ID
     * @param reason  취소 사유
     */
    void cancelOrderProcessing(Long orderId, String reason);

    /**
     * 실패한 주문 처리 재시도.
     *
     * @param orderId 재시도할 주문 ID
     * @return 재시도 성공 시 true
     */
    boolean retryFailedProcessing(Long orderId);

    /**
     * 주문 처리 상태 조회.
     *
     * @param orderId 조회할 주문 ID
     * @return 현재 처리 단계
     */
    ProcessingStage getCurrentStage(Long orderId);

    /**
     * 주문 처리 단계.
     */
    enum ProcessingStage {
        /** 아직 처리 시작 전 */
        NOT_STARTED,
        /** 재고 확정 중 */
        CONFIRMING_INVENTORY,
        /** 배송 준비 중 */
        ARRANGING_SHIPPING,
        /** 처리 완료 */
        COMPLETED,
        /** 처리 실패 */
        FAILED
    }
}
