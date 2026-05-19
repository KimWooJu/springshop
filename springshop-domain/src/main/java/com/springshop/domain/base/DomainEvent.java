package com.springshop.domain.base;

import java.time.Instant;
import java.util.UUID;

/**
 * 모든 도메인 이벤트의 최상위 타입.
 *
 * <p>sealed interface로 정의되어 컴파일러가 도메인 이벤트의 모든 종류를 추적할 수 있다.
 * 패턴 매칭 switch 문에서 누락 사례를 컴파일 타임에 검증 가능하다.</p>
 *
 * <p>모든 도메인 이벤트는 다음 메타데이터를 가진다.
 * <ul>
 *   <li>eventId: 이벤트 식별자(UUID)</li>
 *   <li>occurredAt: 발생 시각</li>
 *   <li>aggregateId: 이벤트를 발생시킨 애그리거트의 ID</li>
 * </ul>
 *
 * <p>구체적인 이벤트는 각 도메인 패키지 내 *Events.java 파일에 record 로 정의된다.</p>
 *
 * @author SpringShop Domain Team
 */
public sealed interface DomainEvent
        permits DomainEvent.GenericEvent,
                com.springshop.domain.user.UserEvents.UserRegisteredEvent,
                com.springshop.domain.user.UserEvents.UserActivatedEvent,
                com.springshop.domain.user.UserEvents.UserLockedEvent,
                com.springshop.domain.user.UserEvents.UserWithdrawnEvent,
                com.springshop.domain.product.ProductEvents.ProductCreatedEvent,
                com.springshop.domain.product.ProductEvents.ProductPublishedEvent,
                com.springshop.domain.product.ProductEvents.ProductOutOfStockEvent,
                com.springshop.domain.product.ProductEvents.ProductDiscontinuedEvent,
                com.springshop.domain.order.OrderEvents.OrderPlacedEvent,
                com.springshop.domain.order.OrderEvents.OrderConfirmedEvent,
                com.springshop.domain.order.OrderEvents.OrderShippedEvent,
                com.springshop.domain.order.OrderEvents.OrderDeliveredEvent,
                com.springshop.domain.order.OrderEvents.OrderCancelledEvent,
                com.springshop.domain.payment.PaymentEvents.PaymentCompletedEvent,
                com.springshop.domain.payment.PaymentEvents.PaymentFailedEvent,
                com.springshop.domain.payment.PaymentEvents.RefundCompletedEvent {

    /**
     * 이벤트의 고유 식별자.
     */
    UUID eventId();

    /**
     * 이벤트 발생 시각.
     */
    Instant occurredAt();

    /**
     * 이벤트의 주체가 되는 애그리거트 식별자.
     */
    Long aggregateId();

    /**
     * 이벤트 타입 이름을 반환한다(기본 구현은 단순 클래스명).
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }

    /**
     * 시스템에서 발행하는 일반 이벤트의 표준 구현체.
     * 특수 분류가 어려운 임시 이벤트에 사용한다.
     */
    record GenericEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String description
    ) implements DomainEvent {

        public GenericEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (description == null) description = "";
        }

        public static GenericEvent of(Long aggregateId, String description) {
            return new GenericEvent(UUID.randomUUID(), Instant.now(), aggregateId, description);
        }

        @Override
        public String eventType() {
            return "GenericEvent";
        }
    }

    /**
     * 헬퍼: 패턴 매칭 기반의 사람이 읽을 수 있는 이벤트 설명을 생성한다.
     */
    static String describe(DomainEvent event) {
        return switch (event) {
            case GenericEvent g -> "Generic: " + g.description();
            case com.springshop.domain.user.UserEvents.UserRegisteredEvent r ->
                    "User registered: id=" + r.aggregateId() + ", email=" + r.email();
            case com.springshop.domain.user.UserEvents.UserActivatedEvent a ->
                    "User activated: id=" + a.aggregateId();
            case com.springshop.domain.user.UserEvents.UserLockedEvent l ->
                    "User locked: id=" + l.aggregateId() + ", reason=" + l.reason();
            case com.springshop.domain.user.UserEvents.UserWithdrawnEvent w ->
                    "User withdrawn: id=" + w.aggregateId();
            case com.springshop.domain.product.ProductEvents.ProductCreatedEvent pc ->
                    "Product created: id=" + pc.aggregateId();
            case com.springshop.domain.product.ProductEvents.ProductPublishedEvent pp ->
                    "Product published: id=" + pp.aggregateId();
            case com.springshop.domain.product.ProductEvents.ProductOutOfStockEvent po ->
                    "Product out of stock: id=" + po.aggregateId();
            case com.springshop.domain.product.ProductEvents.ProductDiscontinuedEvent pd ->
                    "Product discontinued: id=" + pd.aggregateId();
            case com.springshop.domain.order.OrderEvents.OrderPlacedEvent op ->
                    "Order placed: id=" + op.aggregateId();
            case com.springshop.domain.order.OrderEvents.OrderConfirmedEvent oc ->
                    "Order confirmed: id=" + oc.aggregateId();
            case com.springshop.domain.order.OrderEvents.OrderShippedEvent os ->
                    "Order shipped: id=" + os.aggregateId();
            case com.springshop.domain.order.OrderEvents.OrderDeliveredEvent od ->
                    "Order delivered: id=" + od.aggregateId();
            case com.springshop.domain.order.OrderEvents.OrderCancelledEvent ocl ->
                    "Order cancelled: id=" + ocl.aggregateId() + ", reason=" + ocl.reason();
            case com.springshop.domain.payment.PaymentEvents.PaymentCompletedEvent pmc ->
                    "Payment completed: id=" + pmc.aggregateId();
            case com.springshop.domain.payment.PaymentEvents.PaymentFailedEvent pmf ->
                    "Payment failed: id=" + pmf.aggregateId() + ", reason=" + pmf.reason();
            case com.springshop.domain.payment.PaymentEvents.RefundCompletedEvent rc ->
                    "Refund completed: id=" + rc.aggregateId();
        };
    }
}
