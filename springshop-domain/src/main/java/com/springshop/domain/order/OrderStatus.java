package com.springshop.domain.order;

import java.time.LocalDateTime;

/**
 * 주문 상태를 sealed interface로 표현한다.
 *
 * <p>상태 흐름:
 * Pending → PaymentPending → Confirmed → Processing → Shipped → Delivered
 *                                          ↓                          ↓
 *                                       Cancelled              ReturnRequested → Returned</p>
 *
 * @author SpringShop Domain Team
 */
public sealed interface OrderStatus
        permits OrderStatus.Pending,
                OrderStatus.PaymentPending,
                OrderStatus.Confirmed,
                OrderStatus.Processing,
                OrderStatus.Shipped,
                OrderStatus.Delivered,
                OrderStatus.Cancelled,
                OrderStatus.ReturnRequested,
                OrderStatus.Returned {

    record Pending(LocalDateTime createdAt) implements OrderStatus {
        public Pending {
            if (createdAt == null) createdAt = LocalDateTime.now();
        }
    }

    record PaymentPending(LocalDateTime requestedAt) implements OrderStatus {
        public PaymentPending {
            if (requestedAt == null) requestedAt = LocalDateTime.now();
        }
    }

    record Confirmed(LocalDateTime confirmedAt, String confirmedBy) implements OrderStatus {
        public Confirmed {
            if (confirmedAt == null) confirmedAt = LocalDateTime.now();
            if (confirmedBy == null || confirmedBy.isBlank()) confirmedBy = "SYSTEM";
        }
    }

    record Processing(LocalDateTime processedAt) implements OrderStatus {
        public Processing {
            if (processedAt == null) processedAt = LocalDateTime.now();
        }
    }

    record Shipped(String trackingNumber, LocalDateTime shippedAt) implements OrderStatus {
        public Shipped {
            if (trackingNumber == null || trackingNumber.isBlank()) {
                throw new IllegalArgumentException("운송장 번호 필수");
            }
            if (shippedAt == null) shippedAt = LocalDateTime.now();
        }
    }

    record Delivered(LocalDateTime deliveredAt) implements OrderStatus {
        public Delivered {
            if (deliveredAt == null) deliveredAt = LocalDateTime.now();
        }
    }

    record Cancelled(String reason, LocalDateTime cancelledAt) implements OrderStatus {
        public Cancelled {
            if (cancelledAt == null) cancelledAt = LocalDateTime.now();
            if (reason == null) reason = "사유 없음";
        }
    }

    record ReturnRequested(String reason, LocalDateTime requestedAt) implements OrderStatus {
        public ReturnRequested {
            if (requestedAt == null) requestedAt = LocalDateTime.now();
            if (reason == null) reason = "사유 없음";
        }
    }

    record Returned(LocalDateTime returnedAt) implements OrderStatus {
        public Returned {
            if (returnedAt == null) returnedAt = LocalDateTime.now();
        }
    }

    default String label() {
        return switch (this) {
            case Pending p -> "PENDING";
            case PaymentPending pp -> "PAYMENT_PENDING";
            case Confirmed c -> "CONFIRMED";
            case Processing pr -> "PROCESSING";
            case Shipped s -> "SHIPPED";
            case Delivered d -> "DELIVERED";
            case Cancelled cl -> "CANCELLED";
            case ReturnRequested rr -> "RETURN_REQUESTED";
            case Returned r -> "RETURNED";
        };
    }

    default boolean isTerminal() {
        return this instanceof Delivered || this instanceof Cancelled || this instanceof Returned;
    }

    default boolean isCancellable() {
        return switch (this) {
            case Pending p -> true;
            case PaymentPending pp -> true;
            case Confirmed c -> true;
            default -> false;
        };
    }
}
