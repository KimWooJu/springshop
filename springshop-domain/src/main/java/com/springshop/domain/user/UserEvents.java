package com.springshop.domain.user;

import com.springshop.domain.base.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 사용자 도메인에서 발행되는 이벤트들을 모은 컨테이너.
 *
 * <p>각 이벤트는 {@link DomainEvent}의 sealed 계층 일원으로, record로 정의된다.
 * 트랜잭션 커밋 후 ApplicationEventPublisher 를 통해 외부로 전달된다.</p>
 *
 * @author SpringShop Domain Team
 */
public final class UserEvents {

    private UserEvents() {
        throw new UnsupportedOperationException("이벤트 컨테이너는 인스턴스화하지 않습니다");
    }

    /**
     * 사용자가 신규 가입 완료 시 발행.
     */
    public record UserRegisteredEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String email,
            String name,
            UserRole role
    ) implements DomainEvent {

        public UserRegisteredEvent {
            Objects.requireNonNull(aggregateId, "aggregateId");
            Objects.requireNonNull(email, "email");
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (role == null) role = UserRole.CUSTOMER;
        }

        public static UserRegisteredEvent of(Long userId, String email, String name, UserRole role) {
            return new UserRegisteredEvent(UUID.randomUUID(), Instant.now(), userId, email, name, role);
        }
    }

    /**
     * 사용자 활성화 이벤트(비활성 → 활성, 잠금 해제 등).
     */
    public record UserActivatedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String activatedBy
    ) implements DomainEvent {

        public UserActivatedEvent {
            Objects.requireNonNull(aggregateId, "aggregateId");
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (activatedBy == null) activatedBy = "SYSTEM";
        }

        public static UserActivatedEvent of(Long userId, String activatedBy) {
            return new UserActivatedEvent(UUID.randomUUID(), Instant.now(), userId, activatedBy);
        }
    }

    /**
     * 사용자 잠금 이벤트(로그인 실패 횟수 초과, 보안 의심 등).
     */
    public record UserLockedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String reason,
            int failedAttempts
    ) implements DomainEvent {

        public UserLockedEvent {
            Objects.requireNonNull(aggregateId, "aggregateId");
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (reason == null) reason = "보안 정책";
            if (failedAttempts < 0) failedAttempts = 0;
        }

        public static UserLockedEvent of(Long userId, String reason, int attempts) {
            return new UserLockedEvent(UUID.randomUUID(), Instant.now(), userId, reason, attempts);
        }
    }

    /**
     * 사용자 탈퇴 이벤트.
     */
    public record UserWithdrawnEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String reason
    ) implements DomainEvent {

        public UserWithdrawnEvent {
            Objects.requireNonNull(aggregateId, "aggregateId");
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (reason == null) reason = "사용자 요청";
        }

        public static UserWithdrawnEvent of(Long userId, String reason) {
            return new UserWithdrawnEvent(UUID.randomUUID(), Instant.now(), userId, reason);
        }
    }

    /**
     * 비밀번호 변경 이벤트.
     */
    public record PasswordChangedEvent(
            UUID eventId,
            Instant occurredAt,
            Long aggregateId,
            String changedBy
    ) implements DomainEvent {

        public PasswordChangedEvent {
            Objects.requireNonNull(aggregateId, "aggregateId");
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = Instant.now();
            if (changedBy == null) changedBy = "SELF";
        }

        public static PasswordChangedEvent of(Long userId, String changedBy) {
            return new PasswordChangedEvent(UUID.randomUUID(), Instant.now(), userId, changedBy);
        }
    }
}
