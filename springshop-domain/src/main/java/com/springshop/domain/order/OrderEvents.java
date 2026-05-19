package com.springshop.domain.order;

import com.springshop.domain.base.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 주문 도메인 이벤트들.
 *
 * <p>주문 생성/확정/발송/배송/취소 시 발행되는 record 기반 이벤트들을 모은다.
 * 이메일/푸시 알림, 재고 차감/복원, 결제 PG 호출 등이 후행 처리된다.</p>
 *
 * @author SpringShop Domain Team
 */
public final class OrderEvents {

    private OrderEvents() {
        throw new UnsupportedOperationException("인스턴스화 금지");
    }

    public record OrderPlacedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            Long userId,
            BigDecimal totalAmount
    ) implements DomainEvent {

        public OrderPlacedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(totalAmount, "totalAmount");
        }

        public static OrderPlacedEvent of(Long orderId, Long userId, BigDecimal amount) {
            return new OrderPlacedEvent(UUID.randomUUID(), Instant.now(), orderId, userId, amount);
        }
    }

    public record OrderConfirmedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String confirmedBy
    ) implements DomainEvent {

        public OrderConfirmedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (confirmedBy == null) confirmedBy = "SYSTEM";
        }

        public static OrderConfirmedEvent of(Long orderId, String by) {
            return new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(), orderId, by);
        }
    }

    public record OrderShippedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String trackingNumber
    ) implements DomainEvent {

        public OrderShippedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            Objects.requireNonNull(trackingNumber, "trackingNumber");
        }

        public static OrderShippedEvent of(Long orderId, String tracking) {
            return new OrderShippedEvent(UUID.randomUUID(), Instant.now(), orderId, tracking);
        }
    }

    public record OrderDeliveredEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId
    ) implements DomainEvent {

        public OrderDeliveredEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
        }

        public static OrderDeliveredEvent of(Long orderId) {
            return new OrderDeliveredEvent(UUID.randomUUID(), Instant.now(), orderId);
        }
    }

    public record OrderCancelledEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String reason
    ) implements DomainEvent {

        public OrderCancelledEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (reason == null) reason = "사유 없음";
        }

        public static OrderCancelledEvent of(Long orderId, String reason) {
            return new OrderCancelledEvent(UUID.randomUUID(), Instant.now(), orderId, reason);
        }
    }
}
