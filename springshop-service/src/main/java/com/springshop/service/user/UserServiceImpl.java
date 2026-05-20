package com.springshop.service.user;

import com.springshop.domain.user.User;
import com.springshop.domain.user.UserRepository;
import com.springshop.domain.user.UserStatus;
import com.springshop.domain.user.UserEvents.PasswordChangedEvent;
import com.springshop.domain.user.UserEvents.UserActivatedEvent;
import com.springshop.domain.user.UserEvents.UserLockedEvent;
import com.springshop.domain.user.UserEvents.UserRegisteredEvent;
import com.springshop.domain.user.UserEvents.UserWithdrawnEvent;
import com.springshop.common.exception.DuplicateResourceException;
import com.springshop.common.exception.InvalidStateException;
import com.springshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link UserService}의 기본 구현체.
 *
 * <p>비밀번호 정책 검증, 이메일 정규화, 닉네임 중복 처리, 도메인 이벤트 발행,
 * 잠금 임계치 관리 등의 비즈니스 규칙을 캡슐화한다.
 *
 * <p>읽기 메서드는 {@code @Transactional(readOnly = true)} 로,
 * 쓰기 메서드는 기본 트랜잭션으로 실행된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,64}$"
    );

    private static final int MAX_LOGIN_FAILURE = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(2);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    /** 비밀번호 초기화 토큰 in-memory 캐시. 실제 환경에서는 Redis 사용. */
    private final Map<String, ResetTokenEntry> resetTokenStore = new ConcurrentHashMap<>();

    /** 이메일 인증 토큰 캐시. */
    private final Map<String, Long> emailVerificationStore = new ConcurrentHashMap<>();

    /** 로그인 실패 카운터. */
    private final Map<Long, Integer> loginFailureCounter = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public User register(UserRegistrationCommand command) {
        log.info("사용자 등록 요청: email={}", command.email());

        validateRegistrationCommand(command);

        var normalizedEmail = normalizeEmail(command.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("이미 등록된 이메일입니다: " + normalizedEmail);
        }
        if (command.nickname() != null && userRepository.existsByNickname(command.nickname())) {
            throw new DuplicateResourceException("이미 사용 중인 닉네임입니다: " + command.nickname());
        }

        var encodedPassword = passwordEncoder.encode(command.rawPassword());

        var user = User.create(
            normalizedEmail,
            encodedPassword,
            command.name(),
            command.nickname(),
            command.phoneNumber()
        );

        var saved = userRepository.save(user);

        // 이메일 인증 토큰 발행
        var verificationToken = generateSecureToken();
        emailVerificationStore.put(verificationToken, saved.getId());

        eventPublisher.publishEvent(UserRegisteredEvent.of(
            saved.getId(), saved.getEmail().getValue(), saved.getName(), saved.getRole()
        ));

        log.info("사용자 등록 완료: id={}, email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    @Override
    @Transactional
    public User activate(Long userId) {
        var user = loadUser(userId);
        if (user.getStatus() instanceof UserStatus.Active) {
            log.debug("이미 활성 상태인 사용자: id={}", userId);
            return user;
        }
        if (user.getStatus() instanceof UserStatus.Withdrawn) {
            throw new InvalidStateException("탈퇴한 사용자는 활성화할 수 없습니다.");
        }
        user.activate();
        var saved = userRepository.save(user);
        eventPublisher.publishEvent(UserActivatedEvent.of(userId, "ADMIN"));
        log.info("사용자 활성화 완료: id={}", userId);
        return saved;
    }

    @Override
    @Transactional
    public User deactivate(Long userId, String reason) {
        var user = loadUser(userId);
        if (!(user.getStatus() instanceof UserStatus.Active)) {
            throw new InvalidStateException("활성 상태가 아닌 사용자입니다: " + user.getStatus());
        }
        user.deactivate(reason);
        log.info("사용자 비활성화: id={}, reason={}", userId, reason);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User lock(Long userId, String reason, LocalDateTime unlockAt) {
        var user = loadUser(userId);
        user.lock(reason, unlockAt);
        var saved = userRepository.save(user);
        eventPublisher.publishEvent(UserLockedEvent.of(userId, reason, 0));
        log.warn("사용자 잠금: id={}, reason={}, unlockAt={}", userId, reason, unlockAt);
        return saved;
    }

    @Override
    @Transactional
    public User unlock(Long userId) {
        var user = loadUser(userId);
        if (!(user.getStatus() instanceof UserStatus.Locked)) {
            throw new InvalidStateException("잠금 상태가 아닙니다: " + user.getStatus());
        }
        user.unlock();
        loginFailureCounter.remove(userId);
        log.info("사용자 잠금 해제: id={}", userId);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void withdraw(Long userId, String reason) {
        var user = loadUser(userId);
        user.withdraw(reason);
        userRepository.save(user);
        eventPublisher.publishEvent(UserWithdrawnEvent.of(userId, reason));
        log.info("회원 탈퇴 처리 완료: id={}, reason={}", userId, reason);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, UserProfileUpdateCommand command) {
        var user = loadUser(userId);
        if (command.nickname() != null
                && !command.nickname().equals(user.getNickname())
                && userRepository.existsByNickname(command.nickname())) {
            throw new DuplicateResourceException("이미 사용 중인 닉네임입니다: " + command.nickname());
        }
        user.updateProfile(
            command.name(),
            command.nickname(),
            command.phoneNumber(),
            command.profileImageUrl(),
            command.bio()
        );
        log.debug("프로필 업데이트: id={}", userId);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        var user = loadUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("비밀번호 변경 실패 — 현재 비밀번호 불일치: userId={}", userId);
            throw new InvalidStateException("현재 비밀번호가 일치하지 않습니다.");
        }
        validatePasswordPolicy(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidStateException("이전 비밀번호와 동일합니다.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        eventPublisher.publishEvent(PasswordChangedEvent.of(userId, "SELF"));
        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    @Override
    @Transactional
    public String issuePasswordResetToken(String email) {
        var user = userRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new ResourceNotFoundException("등록된 이메일이 없습니다: " + email));
        var token = generateSecureToken();
        resetTokenStore.put(token, new ResetTokenEntry(user.getId(), LocalDateTime.now().plus(RESET_TOKEN_TTL)));
        log.info("비밀번호 초기화 토큰 발급: userId={}", user.getId());
        return token;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        var entry = resetTokenStore.remove(token);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidStateException("유효하지 않거나 만료된 토큰입니다.");
        }
        validatePasswordPolicy(newPassword);
        var user = loadUser(entry.userId());
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        loginFailureCounter.remove(user.getId());
        log.info("비밀번호 초기화 완료: userId={}", user.getId());
    }

    @Override
    @Transactional
    public void recordLoginTimestamp(Long userId, String ipAddress) {
        var user = loadUser(userId);
        user.recordLogin(ipAddress, LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public int incrementLoginFailureCount(Long userId) {
        var count = loginFailureCounter.merge(userId, 1, Integer::sum);
        if (count >= MAX_LOGIN_FAILURE) {
            log.warn("로그인 실패 임계치 초과 — 자동 잠금: userId={}, count={}", userId, count);
            lock(userId, "로그인 연속 실패 " + count + "회", LocalDateTime.now().plus(LOCK_DURATION));
            loginFailureCounter.remove(userId);
        }
        return count;
    }

    @Override
    @Transactional
    public void resetLoginFailureCount(Long userId) {
        loginFailureCounter.remove(userId);
    }

    @Override
    public User findById(Long userId) {
        return loadUser(userId);
    }

    @Override
    public Optional<User> findOptionalById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    @Override
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    @Override
    public boolean isNicknameTaken(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    @Override
    public Page<User> searchUsers(UserSearchCondition condition, Pageable pageable) {
        // 도메인 리포지터리에 검색 메서드가 없는 경우 인메모리 필터링 대안 제공
        var all = userRepository.findAll();
        var filtered = all.stream()
            .filter(u -> matches(u, condition))
            .sorted(Comparator.comparing(User::getCreatedAt).reversed())
            .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        var page = start >= filtered.size() ? List.<User>of() : filtered.subList(start, end);
        return new PageImpl<>(page, pageable, filtered.size());
    }

    @Override
    public List<UserStatusCount> countByStatus() {
        return userRepository.findAll().stream()
            .collect(Collectors.groupingBy(User::getStatus, Collectors.counting()))
            .entrySet().stream()
            .map(e -> new UserStatusCount(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(c -> c.status().label()))
            .toList();
    }

    @Override
    public List<DailyUserStat> getDailyRegistrationStats(int days) {
        var since = LocalDateTime.now().minusDays(days);
        var users = userRepository.findAll().stream()
            .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(since))
            .toList();

        return users.stream()
            .collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new DailyUserStat(
                e.getKey().toString(),
                e.getValue(),
                e.getValue() // 활성 사용자 카운트는 별도 집계가 필요하므로 동일값
            ))
            .toList();
    }

    @Override
    @Transactional
    public void grantAdminRole(Long userId, String grantedBy) {
        var user = loadUser(userId);
        user.grantAdminRole();
        userRepository.save(user);
        log.info("관리자 권한 부여: userId={}, grantedBy={}", userId, grantedBy);
    }

    @Override
    @Transactional
    public void revokeAdminRole(Long userId, String revokedBy) {
        var user = loadUser(userId);
        user.revokeAdminRole();
        userRepository.save(user);
        log.info("관리자 권한 회수: userId={}, revokedBy={}", userId, revokedBy);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteUser(Long userId, String requester) {
        var user = loadUser(userId);
        userRepository.delete(user);
        log.warn("사용자 영구 삭제 — 회복 불가: userId={}, requester={}", userId, requester);
    }

    @Override
    @Transactional
    public int bulkSetDormant(int idleDays) {
        var threshold = LocalDateTime.now().minusDays(idleDays);
        var targets = userRepository.findAll().stream()
            .filter(u -> u.getStatus() instanceof UserStatus.Active)
            .filter(u -> u.getLastLoginAt() == null || u.getLastLoginAt().isBefore(threshold))
            .toList();

        targets.forEach(u -> {
            u.setDormant("자동 휴면 — " + idleDays + "일 미접속");
            userRepository.save(u);
        });
        log.info("대량 휴면 전환 완료: count={}", targets.size());
        return targets.size();
    }

    @Override
    public UserStatsSummary getStatsSummary() {
        var all = userRepository.findAll();
        var today = LocalDate.now();
        long total = all.size();
        long active = all.stream().filter(u -> u.getStatus() instanceof UserStatus.Active).count();
        long dormant = all.stream().filter(u -> u.getStatus() instanceof UserStatus.Inactive).count();
        long withdrawn = all.stream().filter(u -> u.getStatus() instanceof UserStatus.Withdrawn).count();
        long lockedToday = all.stream()
            .filter(u -> u.getStatus() instanceof UserStatus.Locked)
            .filter(u -> u.getLockedAt() != null && u.getLockedAt().toLocalDate().equals(today))
            .count();
        long newToday = all.stream()
            .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(today))
            .count();
        return new UserStatsSummary(total, active, dormant, withdrawn, lockedToday, newToday);
    }

    @Override
    @Transactional
    public User verifyEmail(String token) {
        var userId = emailVerificationStore.remove(token);
        if (userId == null) {
            throw new InvalidStateException("유효하지 않은 인증 토큰입니다.");
        }
        var user = loadUser(userId);
        user.markEmailVerified();
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User changeStatus(Long userId, UserStatus newStatus, String reason) {
        var user = loadUser(userId);
        user.changeStatus(newStatus, reason);
        return userRepository.save(user);
    }

    // ----- helpers -----

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    private boolean matches(User u, UserSearchCondition c) {
        if (c.keyword() != null && !c.keyword().isBlank()) {
            var k = c.keyword().toLowerCase();
            boolean hit = (u.getEmail() != null && u.getEmail().getValue().toLowerCase().contains(k))
                || (u.getNickname() != null && u.getNickname().toLowerCase().contains(k))
                || (u.getName() != null && u.getName().toLowerCase().contains(k));
            if (!hit) return false;
        }
        if (c.status() != null && u.getStatus() != c.status()) return false;
        if (c.registeredFrom() != null && (u.getCreatedAt() == null || u.getCreatedAt().isBefore(c.registeredFrom())))
            return false;
        if (c.registeredTo() != null && (u.getCreatedAt() == null || u.getCreatedAt().isAfter(c.registeredTo())))
            return false;
        if (Boolean.TRUE.equals(c.adminOnly()) && !u.isAdmin()) return false;
        return true;
    }

    private void validateRegistrationCommand(UserRegistrationCommand command) {
        if (command.email() == null || !EMAIL_PATTERN.matcher(command.email()).matches()) {
            throw new InvalidStateException("이메일 형식이 올바르지 않습니다.");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new InvalidStateException("이름은 필수입니다.");
        }
        validatePasswordPolicy(command.rawPassword());
    }

    private void validatePasswordPolicy(String raw) {
        if (raw == null || !PASSWORD_PATTERN.matcher(raw).matches()) {
            throw new InvalidStateException(
                "비밀번호는 8~64자, 영문/숫자/특수문자(@$!%*#?&)를 모두 포함해야 합니다."
            );
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String generateSecureToken() {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record ResetTokenEntry(Long userId, LocalDateTime expiresAt) {}
}
