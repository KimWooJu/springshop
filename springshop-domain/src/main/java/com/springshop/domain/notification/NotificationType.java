package com.springshop.domain.notification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 알림 유형 sealed interface.
 *
 * <p>각 알림은 도메인 이벤트로부터 변환되며, 카테고리/우선순위/표시 메시지 생성을 위한
 * 컨텍스트를 보유한다. JPA 매핑이 까다로우므로 엔티티에는 typeCode 와 payload(JSON)
 * 만 저장하고 런타임에 sealed record 로 복원한다.</p>
 *
 * @author SpringShop Domain Team
 */
public sealed interface NotificationType
        permits NotificationType.OrderStatusChanged,
                NotificationType.PaymentCompleted,
                NotificationType.StockAlert,
                NotificationType.ReviewApproved,
                NotificationType.CouponIssued,
                NotificationType.SystemNotice,
                NotificationType.PriceDropAlert,
                NotificationType.DeliveryUpdate {

    record OrderStatusChanged(Long orderId, String status, String message) implements NotificationType {
        public OrderStatusChanged {
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(status, "status");
            if (message == null) message = "주문 상태가 변경되었습니다";
        }
    }

    record PaymentCompleted(Long orderId, BigDecimal amount) implements NotificationType {
        public PaymentCompleted {
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(amount, "amount");
        }
    }

    record StockAlert(Long productId, String productName, int remainingQuantity) implements NotificationType {
        public StockAlert {
            Objects.requireNonNull(productId, "productId");
            if (productName == null) productName = "상품";
            if (remainingQuantity < 0) {
                throw new IllegalArgumentException("remainingQuantity는 0 이상이어야 합니다");
            }
        }
    }

    record ReviewApproved(Long reviewId, Long productId) implements NotificationType {
        public ReviewApproved {
            Objects.requireNonNull(reviewId, "reviewId");
            Objects.requireNonNull(productId, "productId");
        }
    }

    record CouponIssued(String couponCode, String couponName, LocalDateTime expiryDate) implements NotificationType {
        public CouponIssued {
            Objects.requireNonNull(couponCode, "couponCode");
            if (couponName == null) couponName = couponCode;
            Objects.requireNonNull(expiryDate, "expiryDate");
        }
    }

    record SystemNotice(String message, String actionUrl) implements NotificationType {
        public SystemNotice {
            Objects.requireNonNull(message, "message");
        }
    }

    record PriceDropAlert(Long productId, String productName, BigDecimal oldPrice, BigDecimal newPrice) implements NotificationType {
        public PriceDropAlert {
            Objects.requireNonNull(productId, "productId");
            Objects.requireNonNull(oldPrice, "oldPrice");
            Objects.requireNonNull(newPrice, "newPrice");
            if (newPrice.compareTo(oldPrice) >= 0) {
                throw new IllegalArgumentException("newPrice는 oldPrice보다 작아야 합니다");
            }
        }

        public BigDecimal getDiscountAmount() {
            return oldPrice.subtract(newPrice);
        }
    }

    record DeliveryUpdate(Long orderId, String trackingNumber, String carrier, String status) implements NotificationType {
        public DeliveryUpdate {
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(trackingNumber, "trackingNumber");
            Objects.requireNonNull(carrier, "carrier");
            if (status == null) status = "IN_TRANSIT";
        }
    }

    /**
     * 카테고리명 (UI 그룹핑용).
     */
    default String getCategoryName() {
        return switch (this) {
            case OrderStatusChanged o -> "주문";
            case PaymentCompleted p -> "결제";
            case StockAlert s -> "재고";
            case ReviewApproved r -> "리뷰";
            case CouponIssued c -> "쿠폰";
            case PriceDropAlert d -> "가격";
            case DeliveryUpdate d -> "배송";
            case SystemNotice s -> "시스템";
        };
    }

    /**
     * 카테고리 코드.
     */
    default String getCategoryCode() {
        return switch (this) {
            case OrderStatusChanged o -> "ORDER";
            case PaymentCompleted p -> "PAYMENT";
            case StockAlert s -> "STOCK";
            case ReviewApproved r -> "REVIEW";
            case CouponIssued c -> "COUPON";
            case PriceDropAlert d -> "PRICE";
            case DeliveryUpdate d -> "DELIVERY";
            case SystemNotice s -> "SYSTEM";
        };
    }

    /**
     * 권장 우선순위.
     */
    default Notification.Priority recommendedPriority() {
        return switch (this) {
            case PaymentCompleted p -> Notification.Priority.HIGH;
            case DeliveryUpdate d -> Notification.Priority.HIGH;
            case OrderStatusChanged o -> Notification.Priority.NORMAL;
            case StockAlert s -> Notification.Priority.NORMAL;
            case ReviewApproved r -> Notification.Priority.LOW;
            case CouponIssued c -> Notification.Priority.NORMAL;
            case PriceDropAlert p -> Notification.Priority.LOW;
            case SystemNotice s -> Notification.Priority.URGENT;
        };
    }
}
