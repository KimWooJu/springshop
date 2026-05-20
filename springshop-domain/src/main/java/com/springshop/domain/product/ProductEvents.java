package com.springshop.domain.product;

import com.springshop.domain.base.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 상품 도메인 이벤트들.
 *
 * <p>상품 등록/게시/품절/단종 시 발행되는 record 기반 이벤트들을 모은다.
 * 각 이벤트는 알림 발송, 검색 인덱스 갱신, 분석 시스템 전달의 트리거가 된다.</p>
 *
 * @author SpringShop Domain Team
 */
public final class ProductEvents {

    private ProductEvents() {
        throw new UnsupportedOperationException("인스턴스화 금지");
    }

    /** 새 상품 생성됨. */
    public record ProductCreatedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String productName,
            Long categoryId
    ) implements DomainEvent {

        public ProductCreatedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            Objects.requireNonNull(productName, "상품명 필수");
        }

        public static ProductCreatedEvent of(Long productId, String name, Long categoryId) {
            return new ProductCreatedEvent(UUID.randomUUID(), Instant.now(), productId, name, categoryId);
        }
    }

    /** 상품이 게시되어 판매 시작됨. */
    public record ProductPublishedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String productName
    ) implements DomainEvent {

        public ProductPublishedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
        }

        public static ProductPublishedEvent of(Long productId, String name) {
            return new ProductPublishedEvent(UUID.randomUUID(), Instant.now(), productId, name);
        }
    }

    /** 상품 품절. */
    public record ProductOutOfStockEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String productName
    ) implements DomainEvent {

        public ProductOutOfStockEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
        }

        public static ProductOutOfStockEvent of(Long productId, String name) {
            return new ProductOutOfStockEvent(UUID.randomUUID(), Instant.now(), productId, name);
        }
    }

    /** 상품 단종. */
    public record ProductDiscontinuedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String productName,
            String reason
    ) implements DomainEvent {

        public ProductDiscontinuedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (reason == null) reason = "사유 없음";
        }

        public static ProductDiscontinuedEvent of(Long productId, String name, String reason) {
            return new ProductDiscontinuedEvent(UUID.randomUUID(), Instant.now(), productId, name, reason);
        }
    }

    /** 상품 가격 변경됨. */
    public record ProductPriceChangedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            BigDecimal oldPrice,
            BigDecimal newPrice
    ) implements DomainEvent {

        public ProductPriceChangedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            Objects.requireNonNull(oldPrice, "oldPrice");
            Objects.requireNonNull(newPrice, "newPrice");
        }

        public static ProductPriceChangedEvent of(Long productId, BigDecimal oldPrice, BigDecimal newPrice) {
            return new ProductPriceChangedEvent(UUID.randomUUID(), Instant.now(), productId, oldPrice, newPrice);
        }
    }
}
