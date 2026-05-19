package com.springshop.domain.user;

import com.springshop.domain.base.BaseAuditEntity;
import com.springshop.domain.vo.Email;
import com.springshop.domain.vo.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 시스템 사용자 엔티티 (애그리거트 루트).
 *
 * <p>고객, 판매자, 관리자 등을 통합 관리한다. 비밀번호는 BCrypt 해시 형태로
 * 저장되며, 본 클래스에서 직접 암호화하지 않고 서비스 레이어에서 처리한다.</p>
 *
 * <p>{@link UserStatus} sealed interface 기반 상태 관리: 활성/비활성/잠금/탈퇴.
 * 상태 전이는 비즈니스 메서드(activate/deactivate/lock/withdraw)로만 가능하다.</p>
 *
 * <p>로그인 시도 실패 횟수가 임계값(5회)을 초과하면 자동 잠금된다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email_value")
        },
        indexes = {
                @Index(name = "idx_user_status", columnList = "status_label"),
                @Index(name = "idx_user_role", columnList = "role")
        }
)
public class User extends BaseAuditEntity {

    public static final int MAX_FAILED_LOGIN_BEFORE_LOCK = 5;
    public static final int LOCK_DURATION_MINUTES = 30;

    @Embedded
    private Email email;

    @Column(name = "password_hash", length = 200, nullable = false)
    private String passwordHash;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Embedded
    private PhoneNumber phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role;

    /** sealed interface는 직접 JPA 매핑이 어려워 라벨/메타로 분리 저장한다. */
    @Column(name = "status_label", length = 20, nullable = false)
    private String statusLabel = "ACTIVE";

    @Column(name = "status_reason", length = 200)
    private String statusReason;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "status_unlock_at")
    private LocalDateTime statusUnlockAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent = false;

    @Transient
    private UserStatus cachedStatus;

    protected User() {
        super();
    }

    private User(Email email, String passwordHash, String name, PhoneNumber phone, UserRole role) {
        super();
        this.email = Objects.requireNonNull(email, "이메일은 필수입니다");
        this.passwordHash = Objects.requireNonNull(passwordHash, "비밀번호 해시는 필수입니다");
        this.name = Objects.requireNonNull(name, "이름은 필수입니다");
        this.phone = phone;
        this.role = Objects.requireNonNull(role, "역할은 필수입니다");
        LocalDateTime now = LocalDateTime.now();
        this.statusLabel = "ACTIVE";
        this.statusChangedAt = now;
        registerEvent(UserEvents.UserRegisteredEvent.of(null, email.getValue(), name, role));
    }

    public static User register(Email email, String passwordHash, String name, PhoneNumber phone, UserRole role) {
        return new User(email, passwordHash, name, phone, role);
    }

    public static User registerCustomer(Email email, String passwordHash, String name, PhoneNumber phone) {
        return new User(email, passwordHash, name, phone, UserRole.CUSTOMER);
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public PhoneNumber getPhone() {
        return phone;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    /**
     * 현재 상태를 sealed interface 형태로 복원한다.
     */
    public UserStatus getStatus() {
        if (cachedStatus != null) return cachedStatus;
        UserStatus s = switch (statusLabel) {
            case "ACTIVE" -> new UserStatus.Active(statusChangedAt != null ? statusChangedAt : getCreatedAt());
            case "INACTIVE" -> new UserStatus.Inactive(statusReason, statusChangedAt);
            case "LOCKED" -> new UserStatus.Locked(statusReason, statusChangedAt, statusUnlockAt);
            case "WITHDRAWN" -> new UserStatus.Withdrawn(statusChangedAt);
            default -> throw new IllegalStateException("알 수 없는 상태: " + statusLabel);
        };
        this.cachedStatus = s;
        return s;
    }

    public void activate(String by) {
        UserStatus current = getStatus();
        if (current instanceof UserStatus.Active) {
            return; // 이미 활성
        }
        if (!current.isReactivatable() && !(current instanceof UserStatus.Withdrawn)) {
            throw new IllegalStateException("활성화 불가 상태: " + current.label());
        }
        if (current instanceof UserStatus.Withdrawn) {
            throw new IllegalStateException("탈퇴한 사용자는 활성화할 수 없습니다");
        }
        applyStatus("ACTIVE", null, null);
        this.failedLoginCount = 0;
        registerEvent(UserEvents.UserActivatedEvent.of(getId(), by));
    }

    public void deactivate(String reason) {
        if (getStatus() instanceof UserStatus.Withdrawn) {
            throw new IllegalStateException("탈퇴 사용자는 비활성화할 수 없습니다");
        }
        applyStatus("INACTIVE", reason, null);
    }

    public void lock(String reason, LocalDateTime unlockAt) {
        if (getStatus() instanceof UserStatus.Withdrawn) {
            throw new IllegalStateException("탈퇴 사용자는 잠글 수 없습니다");
        }
        applyStatus("LOCKED", reason, unlockAt);
        registerEvent(UserEvents.UserLockedEvent.of(getId(), reason, failedLoginCount));
    }

    public void withdraw(String reason) {
        if (getStatus() instanceof UserStatus.Withdrawn) {
            throw new IllegalStateException("이미 탈퇴 처리되었습니다");
        }
        applyStatus("WITHDRAWN", reason, null);
        registerEvent(UserEvents.UserWithdrawnEvent.of(getId(), reason));
    }

    public void incrementFailedLogin() {
        this.failedLoginCount++;
        if (this.failedLoginCount >= MAX_FAILED_LOGIN_BEFORE_LOCK) {
            LocalDateTime unlockAt = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
            lock("로그인 시도 초과(" + failedLoginCount + "회)", unlockAt);
        }
    }

    public void resetFailedLogin() {
        this.failedLoginCount = 0;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        resetFailedLogin();
    }

    public void changePassword(String newHash) {
        this.passwordHash = Objects.requireNonNull(newHash, "새 비밀번호 해시 필수");
    }

    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("이름은 비어 있을 수 없습니다");
        }
        this.name = newName.trim();
    }

    public void changePhone(PhoneNumber newPhone) {
        this.phone = newPhone;
    }

    public void agreeToMarketing() {
        this.marketingConsent = true;
    }

    public void revokeMarketingConsent() {
        this.marketingConsent = false;
    }

    public boolean canLogin() {
        UserStatus s = getStatus();
        if (s instanceof UserStatus.Locked locked && locked.isAutoUnlockable(LocalDateTime.now())) {
            activate("AUTO_UNLOCK");
            return true;
        }
        return s.canLogin();
    }

    public boolean hasPermission(String permission) {
        return role != null && role.hasPermission(permission);
    }

    private void applyStatus(String label, String reason, LocalDateTime unlockAt) {
        this.statusLabel = label;
        this.statusReason = reason;
        this.statusChangedAt = LocalDateTime.now();
        this.statusUnlockAt = unlockAt;
        this.cachedStatus = null;
    }
}
