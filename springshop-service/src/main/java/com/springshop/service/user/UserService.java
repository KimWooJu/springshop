package com.springshop.service.user;

import com.springshop.domain.user.User;
import com.springshop.domain.user.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 도메인의 핵심 애플리케이션 서비스.
 *
 * <p>회원 가입, 인증, 프로필 관리, 상태 변경, 통계 조회 등
 * 사용자 라이프사이클 전반에 걸친 유스케이스를 캡슐화한다.
 *
 * <p>모든 조회 메서드는 read-only 트랜잭션, 변경 메서드는
 * 기본 트랜잭션으로 실행되며, 도메인 이벤트는 구현체에서
 * {@code ApplicationEventPublisher}를 통해 발행된다.
 */
public interface UserService {

    /**
     * 신규 사용자 등록.
     *
     * <p>이메일 중복 검사, 비밀번호 정책 검증, 비밀번호 해시 처리,
     * 환영 이벤트 발행을 수행한다.
     *
     * @param command 신규 사용자 등록 커맨드
     * @return 영속화된 사용자 엔티티
     */
    User register(UserRegistrationCommand command);

    /**
     * 휴면 또는 미인증 상태의 사용자를 활성 상태로 전환한다.
     *
     * @param userId 대상 사용자 식별자
     * @return 활성화된 사용자
     */
    User activate(Long userId);

    /**
     * 사용자를 비활성 상태(자진 휴면)로 전환한다.
     */
    User deactivate(Long userId, String reason);

    /**
     * 사용자를 잠금 상태로 전환한다. 로그인 실패 누적, 부정 행위 탐지 시 호출된다.
     */
    User lock(Long userId, String reason, LocalDateTime unlockAt);

    /**
     * 잠금된 사용자를 해제한다.
     */
    User unlock(Long userId);

    /**
     * 사용자가 회원 탈퇴를 요청한다. 개인정보 즉시 삭제가 아닌
     * 익명화 + 30일 보존 정책을 적용한다.
     */
    void withdraw(Long userId, String reason);

    /**
     * 사용자 프로필(이름, 닉네임, 전화번호 등)을 갱신한다.
     */
    User updateProfile(Long userId, UserProfileUpdateCommand command);

    /**
     * 비밀번호 변경. 현재 비밀번호 검증 후 신규 비밀번호로 교체한다.
     */
    void changePassword(Long userId, String currentPassword, String newPassword);

    /**
     * 비밀번호 초기화 토큰 발급 및 이메일 발송.
     */
    String issuePasswordResetToken(String email);

    /**
     * 발급된 초기화 토큰으로 새 비밀번호를 적용한다.
     */
    void resetPassword(String token, String newPassword);

    /**
     * 마지막 로그인 시각을 기록한다.
     */
    void recordLoginTimestamp(Long userId, String ipAddress);

    /**
     * 로그인 실패 카운터를 증가시키고, 임계값을 초과하면 자동으로 잠금 처리한다.
     */
    int incrementLoginFailureCount(Long userId);

    /**
     * 로그인 성공 시 실패 카운터를 초기화한다.
     */
    void resetLoginFailureCount(Long userId);

    /** 단일 조회 — 없으면 예외 발생 */
    User findById(Long userId);

    /** 단일 조회 — Optional 반환 */
    Optional<User> findOptionalById(Long userId);

    /** 이메일 단일 조회 */
    Optional<User> findByEmail(String email);

    /** 이메일 또는 닉네임 중복 여부 */
    boolean isEmailRegistered(String email);

    boolean isNicknameTaken(String nickname);

    /**
     * 키워드 검색(이메일/닉네임/이름 LIKE 검색)과 필터를 결합한 페이징 검색.
     */
    Page<User> searchUsers(UserSearchCondition condition, Pageable pageable);

    /**
     * 상태별 사용자 수 조회 (관리자 대시보드).
     */
    List<UserStatusCount> countByStatus();

    /**
     * 신규 가입 통계 — 일자별 카운트.
     */
    List<DailyUserStat> getDailyRegistrationStats(int days);

    /**
     * 관리자 권한 부여/회수.
     */
    void grantAdminRole(Long userId, String grantedBy);

    void revokeAdminRole(Long userId, String revokedBy);

    /**
     * 사용자 영구 삭제 (GDPR 등 법적 요구 사항).
     */
    void deleteUser(Long userId, String requester);

    /**
     * 휴면 전환 — 일정 기간 미접속 사용자 일괄 처리.
     */
    int bulkSetDormant(int idleDays);

    /**
     * 통계 요약 (전체/활성/휴면/탈퇴) 조회.
     */
    UserStatsSummary getStatsSummary();

    /**
     * 이메일 인증 토큰 검증 및 상태 변경.
     */
    User verifyEmail(String token);

    /**
     * 사용자 상태 강제 변경 (관리자용).
     */
    User changeStatus(Long userId, UserStatus newStatus, String reason);

    record UserRegistrationCommand(
        String email,
        String rawPassword,
        String name,
        String nickname,
        String phoneNumber,
        String referralCode
    ) {}

    record UserProfileUpdateCommand(
        String name,
        String nickname,
        String phoneNumber,
        String profileImageUrl,
        String bio
    ) {}

    record UserSearchCondition(
        String keyword,
        UserStatus status,
        LocalDateTime registeredFrom,
        LocalDateTime registeredTo,
        Boolean adminOnly
    ) {}

    record UserStatusCount(UserStatus status, long count) {}

    record DailyUserStat(String date, long newUsers, long activeUsers) {}

    record UserStatsSummary(
        long total,
        long active,
        long dormant,
        long withdrawn,
        long lockedToday,
        long newRegistrationsToday
    ) {}
}
