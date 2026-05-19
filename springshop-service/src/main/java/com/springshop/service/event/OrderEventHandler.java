package com.springshop.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;

/**
 * 주문 도메인 이벤트 핸들러.
 *
 * <p>주문 생성·취소·완료 이벤트를 수신하여 재고 조정, 알림 발송,
 * 쿠폰 상태 업데이트, 포인트 적립 등의 부수 작업을 처리한다.
 *
 * <p>트랜잭션 이벤트 리스너({@link TransactionalEventListener})를 사용하여
 * 주문 커밋 성공 후에만 부수 작업을 실행함으로써 데이터 정합성을 보장한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventHandler {

    // 실제 구현에서는 아래 서비스를 주입한다.
    // private final InventoryService inventoryService;
    // private final NotificationService notificationService;
    // private final CouponService couponService;

    /**
     * 주문 생성 이벤트 — 재고 예약 및 알림 발송.
     * 트랜잭션 커밋 후에 실행되어 재고 예약 일관성을 보장한다.
     *
     * @param event 주문 생성 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("주문 생성 이벤트 처리 - orderId={}, userId={}", event.orderId(), event.userId());

        try {
            // 1. 재고 예약 (역: 재고 감소)
            reserveInventory(event);
            // 2. 주문 확인 알림 발송
            sendOrderPlacedNotification(event);
            // 3. 주문 확인 이메일 발송
            sendOrderConfirmationEmail(event);

            log.info("주문 생성 이벤트 처리 완료 - orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("주문 생성 이벤트 처리 실패 - orderId={}, error={}",
                    event.orderId(), ex.getMessage(), ex);
        }
    }

    /**
     * 주문 취소 이벤트 — 재고 복구 및 결제 환불 트리거.
     *
     * @param event 주문 취소 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 처리 - orderId={}, reason={}", event.orderId(), event.reason());

        try {
            // 1. 재고 복구
            restoreInventory(event);
            // 2. 쿠폰 사용 취소 (적용된 경우)
            restoreCoupon(event);
            // 3. 취소 알림 발송
            sendOrderCancelledNotification(event);

            log.info("주문 취소 이벤트 처리 완료 - orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("주문 취소 이벤트 처리 실패 - orderId={}, error={}",
                    event.orderId(), ex.getMessage(), ex);
        }
    }

    /**
     * 주문 완료(배송 완료 후 구매 확정) 이벤트 — 포인트 적립 및 리뷰 작성 유도 알림.
     *
     * @param event 주문 완료 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onOrderCompleted(OrderCompletedEvent event) {
        log.info("주문 완료 이벤트 처리 - orderId={}, userId={}", event.orderId(), event.userId());

        try {
            // 1. 포인트 적립
            accumulatePoints(event);
            // 2. 리뷰 작성 유도 알림
            sendReviewRequestNotification(event);

            log.info("주문 완료 이벤트 처리 완료 - orderId={}", event.orderId());
        } catch (Exception ex) {
            log.error("주문 완료 이벤트 처리 실패 - orderId={}, error={}",
                    event.orderId(), ex.getMessage(), ex);
        }
    }

    /**
     * 주문 상태 변경 이벤트 — 배송 추적 알림.
     *
     * @param event 주문 상태 변경 이벤트
     */
    @EventListener
    @Async
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("주문 상태 변경 이벤트 - orderId={}, status={}", event.orderId(), event.newStatus());

        try {
            sendOrderStatusNotification(event);
        } catch (Exception ex) {
            log.warn("주문 상태 변경 알림 실패 - orderId={}, error={}",
                    event.orderId(), ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private action methods
    // -------------------------------------------------------------------------

    private void reserveInventory(OrderPlacedEvent event) {
        log.debug("재고 예약 - orderId={}", event.orderId());
        // 실제: event.items().forEach(item -> inventoryService.reserveStock(item.productId(), item.quantity()))
    }

    private void sendOrderPlacedNotification(OrderPlacedEvent event) {
        log.debug("주문 생성 알림 발송 - orderId={}", event.orderId());
        // 실제: notificationService.sendOrderStatusNotification(event.userId(), ...)
    }

    private void sendOrderConfirmationEmail(OrderPlacedEvent event) {
        log.debug("주문 확인 이메일 발송 - orderId={}", event.orderId());
        // 실제: emailService.sendOrderConfirmation(...)
    }

    private void restoreInventory(OrderCancelledEvent event) {
        log.debug("재고 복구 - orderId={}", event.orderId());
    }

    private void restoreCoupon(OrderCancelledEvent event) {
        log.debug("쿠폰 복구 - orderId={}", event.orderId());
    }

    private void sendOrderCancelledNotification(OrderCancelledEvent event) {
        log.debug("취소 알림 발송 - orderId={}", event.orderId());
    }

    private void accumulatePoints(OrderCompletedEvent event) {
        log.debug("포인트 적립 - orderId={}, amount={}", event.orderId(), event.totalAmount());
    }

    private void sendReviewRequestNotification(OrderCompletedEvent event) {
        log.debug("리뷰 작성 요청 알림 - orderId={}", event.orderId());
    }

    private void sendOrderStatusNotification(OrderStatusChangedEvent event) {
        log.debug("배송 추적 알림 - orderId={}, status={}", event.orderId(), event.newStatus());
    }

    // -------------------------------------------------------------------------
    // Event record types (도메인 이벤트)
    // -------------------------------------------------------------------------

    /**
     * 주문 생성 이벤트.
     */
    public record OrderPlacedEvent(
            Long orderId,
            String orderNo,
            Long userId,
            String userEmail,
            java.util.List<OrderItemSnapshot> items,
            java.math.BigDecimal totalAmount,
            LocalDateTime occurredAt
    ) {
        public record OrderItemSnapshot(Long productId, String productName, int quantity,
                                        java.math.BigDecimal price) {}
    }

    /**
     * 주문 취소 이벤트.
     */
    public record OrderCancelledEvent(
            Long orderId,
            String orderNo,
            Long userId,
            String reason,
            String couponCode,
            java.math.BigDecimal refundAmount,
            LocalDateTime occurredAt
    ) {}

    /**
     * 주문 완료 이벤트.
     */
    public record OrderCompletedEvent(
            Long orderId,
            String orderNo,
            Long userId,
            java.math.BigDecimal totalAmount,
            LocalDateTime occurredAt
    ) {}

    /**
     * 주문 상태 변경 이벤트.
     */
    public record OrderStatusChangedEvent(
            Long orderId,
            String orderNo,
            Long userId,
            String previousStatus,
            String newStatus,
            String trackingNumber,
            LocalDateTime occurredAt
    ) {}
}
