package com.springshop.domain.review;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 리뷰 상태를 sealed interface로 표현한다.
 *
 * <p>상태 흐름:
 * <pre>
 * Pending ─→ Approved ─→ Hidden
 *    └─────→ Rejected
 *           Approved ─→ Hidden (자동/수동)
 * </pre>
 *
 * @author SpringShop Domain Team
 */
public sealed interface ReviewStatus
        permits ReviewStatus.Pending,
                ReviewStatus.Approved,
                ReviewStatus.Rejected,
                ReviewStatus.Hidden {

    /**
     * 검토 대기.
     */
    record Pending(LocalDateTime submittedAt) implements ReviewStatus {
        public Pending {
            if (submittedAt == null) submittedAt = LocalDateTime.now();
        }

        /**
         * 검토 SLA(3일)를 초과하였는지.
         */
        public boolean isOverdue() {
            return submittedAt.plusDays(3).isBefore(LocalDateTime.now());
        }
    }

    /**
     * 승인됨. 일반 사용자에게 노출된다.
     */
    record Approved(LocalDateTime approvedAt, String approvedBy) implements ReviewStatus {
        public Approved {
            if (approvedAt == null) approvedAt = LocalDateTime.now();
            if (approvedBy == null || approvedBy.isBlank()) approvedBy = "SYSTEM";
        }
    }

    /**
     * 거부됨. 부적절한 콘텐츠 등으로 거부된 경우.
     */
    record Rejected(String reason, LocalDateTime rejectedAt, String rejectedBy) implements ReviewStatus {
        public Rejected {
            Objects.requireNonNull(reason, "거부 사유는 null일 수 없습니다");
            if (rejectedAt == null) rejectedAt = LocalDateTime.now();
            if (rejectedBy == null || rejectedBy.isBlank()) rejectedBy = "SYSTEM";
        }
    }

    /**
     * 숨김 처리됨. 신고 누적 또는 정책 위반.
     */
    record Hidden(String reason, LocalDateTime hiddenAt) implements ReviewStatus {
        public Hidden {
            Objects.requireNonNull(reason, "숨김 사유는 null일 수 없습니다");
            if (hiddenAt == null) hiddenAt = LocalDateTime.now();
        }
    }

    /**
     * 일반 사용자에게 공개되는 상태인지.
     */
    default boolean isPublic() {
        return this instanceof Approved;
    }

    /**
     * 검토 대기 중인지.
     */
    default boolean isPending() {
        return this instanceof Pending;
    }

    /**
     * 종료 상태인지.
     */
    default boolean isTerminal() {
        return switch (this) {
            case Rejected r -> true;
            case Hidden h -> true;
            case Approved a -> false;
            case Pending p -> false;
        };
    }

    /**
     * 상태 라벨.
     */
    default String label() {
        return switch (this) {
            case Pending p -> "PENDING";
            case Approved a -> "APPROVED";
            case Rejected r -> "REJECTED";
            case Hidden h -> "HIDDEN";
        };
    }
}
