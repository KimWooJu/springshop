package com.springshop.domain.product;

import java.time.LocalDateTime;

/**
 * 상품의 생명주기 상태를 sealed interface로 표현한다.
 *
 * <p>상태 흐름:
 * Draft → UnderReview → Active → (OutOfStock ↔ Active) → Discontinued → Deleted</p>
 *
 * @author SpringShop Domain Team
 */
public sealed interface ProductStatus
        permits ProductStatus.Draft,
                ProductStatus.UnderReview,
                ProductStatus.Active,
                ProductStatus.OutOfStock,
                ProductStatus.Discontinued,
                ProductStatus.Deleted {

    /** 작성 중. 외부에 노출되지 않음. */
    record Draft(LocalDateTime createdAt) implements ProductStatus {
        public Draft {
            if (createdAt == null) createdAt = LocalDateTime.now();
        }
    }

    /** 관리자 검토 대기. */
    record UnderReview(LocalDateTime submittedAt) implements ProductStatus {
        public UnderReview {
            if (submittedAt == null) throw new IllegalArgumentException("제출 시각 필수");
        }
    }

    /** 정상 판매중. */
    record Active(LocalDateTime publishedAt) implements ProductStatus {
        public Active {
            if (publishedAt == null) throw new IllegalArgumentException("게시 시각 필수");
        }
    }

    /** 일시 품절. */
    record OutOfStock(LocalDateTime outAt) implements ProductStatus {
        public OutOfStock {
            if (outAt == null) outAt = LocalDateTime.now();
        }
    }

    /** 단종. */
    record Discontinued(LocalDateTime discontinuedAt) implements ProductStatus {
        public Discontinued {
            if (discontinuedAt == null) discontinuedAt = LocalDateTime.now();
        }
    }

    /** 삭제됨(소프트 삭제). */
    record Deleted(LocalDateTime deletedAt, String reason) implements ProductStatus {
        public Deleted {
            if (deletedAt == null) deletedAt = LocalDateTime.now();
            if (reason == null) reason = "사유 없음";
        }
    }

    /**
     * 고객에게 노출되는 상태인지 검사.
     */
    default boolean isVisibleToCustomer() {
        return switch (this) {
            case Active a -> true;
            case OutOfStock o -> true;
            default -> false;
        };
    }

    /**
     * 구매 가능한 상태인지 검사.
     */
    default boolean isPurchasable() {
        return this instanceof Active;
    }

    /**
     * 상태 라벨 반환.
     */
    default String label() {
        return switch (this) {
            case Draft d -> "DRAFT";
            case UnderReview u -> "UNDER_REVIEW";
            case Active a -> "ACTIVE";
            case OutOfStock o -> "OUT_OF_STOCK";
            case Discontinued d -> "DISCONTINUED";
            case Deleted del -> "DELETED";
        };
    }
}
