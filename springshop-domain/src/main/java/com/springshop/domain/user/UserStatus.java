package com.springshop.domain.user;

import java.time.LocalDateTime;

/**
 * 사용자 계정 상태를 표현하는 sealed interface.
 *
 * <p>가능한 상태:
 * <ul>
 *   <li>{@link Active} — 정상 활동 중</li>
 *   <li>{@link Inactive} — 휴면 또는 비활성</li>
 *   <li>{@link Locked} — 보안 사유로 일시 잠금</li>
 *   <li>{@link Withdrawn} — 탈퇴 처리됨</li>
 * </ul>
 *
 * <p>각 상태는 자체 메타데이터(가입일, 잠금 해제 시각 등)를 record로 가진다.
 * 패턴 매칭 switch로 분기 처리 시 컴파일 타임 완전성 검사가 적용된다.</p>
 *
 * @author SpringShop Domain Team
 */
public sealed interface UserStatus
        permits UserStatus.Active,
                UserStatus.Inactive,
                UserStatus.Locked,
                UserStatus.Withdrawn {

    /** 정상 활동 중 상태. */
    record Active(LocalDateTime joinedAt) implements UserStatus {
        public Active {
            if (joinedAt == null) {
                throw new IllegalArgumentException("가입 시각은 null일 수 없습니다");
            }
        }
    }

    /** 비활성/휴면 상태. */
    record Inactive(String reason, LocalDateTime deactivatedAt) implements UserStatus {
        public Inactive {
            if (deactivatedAt == null) {
                throw new IllegalArgumentException("비활성화 시각은 null일 수 없습니다");
            }
            if (reason == null) reason = "사유 없음";
        }
    }

    /** 잠금 상태. */
    record Locked(String reason, LocalDateTime lockedAt, LocalDateTime unlockAt) implements UserStatus {
        public Locked {
            if (lockedAt == null) {
                throw new IllegalArgumentException("잠금 시각은 null일 수 없습니다");
            }
            if (unlockAt != null && unlockAt.isBefore(lockedAt)) {
                throw new IllegalArgumentException("해제 시각이 잠금 시각보다 빠릅니다");
            }
            if (reason == null) reason = "보안 정책";
        }

        public boolean isAutoUnlockable(LocalDateTime now) {
            return unlockAt != null && !now.isBefore(unlockAt);
        }
    }

    /** 탈퇴 상태. */
    record Withdrawn(LocalDateTime withdrawnAt) implements UserStatus {
        public Withdrawn {
            if (withdrawnAt == null) {
                throw new IllegalArgumentException("탈퇴 시각은 null일 수 없습니다");
            }
        }
    }

    /**
     * 상태가 로그인을 허용하는지 검사한다.
     */
    default boolean canLogin() {
        return switch (this) {
            case Active a -> true;
            case Inactive i -> false;
            case Locked l -> false;
            case Withdrawn w -> false;
        };
    }

    /**
     * 사람이 읽을 수 있는 상태 라벨을 반환한다.
     */
    default String label() {
        return switch (this) {
            case Active a -> "활성";
            case Inactive i -> "비활성(" + i.reason() + ")";
            case Locked l -> "잠금(" + l.reason() + ")";
            case Withdrawn w -> "탈퇴(" + w.withdrawnAt() + ")";
        };
    }

    /**
     * 활성화 가능한 상태인지 검사한다(Inactive, Locked만 가능).
     */
    default boolean isReactivatable() {
        return this instanceof Inactive || this instanceof Locked;
    }
}
