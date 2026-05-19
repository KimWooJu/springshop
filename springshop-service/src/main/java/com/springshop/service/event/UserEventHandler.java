package com.springshop.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;

/**
 * 사용자 도메인 이벤트 핸들러.
 *
 * <p>회원 가입, 비밀번호 변경, 탈퇴, 로그인 이상 감지 등
 * 사용자 라이프사이클 이벤트를 수신하여 이메일/알림 발송 등의 부수 작업을 처리한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventHandler {

    // 실제 구현에서는 아래 서비스를 주입한다.
    // private final EmailService emailService;
    // private final NotificationService notificationService;
    // private final CouponService couponService;

    /**
     * 회원 가입 이벤트 — 환영 이메일 발송 및 신규 가입 쿠폰 발급.
     *
     * @param event 회원 가입 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("회원 가입 이벤트 처리 - userId={}, email={}", event.userId(), event.email());

        try {
            // 1. 환영 이메일 발송
            sendWelcomeEmail(event);
            // 2. 신규 가입 쿠폰 발급
            issueWelcomeCoupon(event);
            // 3. 가입 완료 인앱 알림
            sendWelcomeNotification(event);

            log.info("회원 가입 이벤트 처리 완료 - userId={}", event.userId());
        } catch (Exception ex) {
            log.error("회원 가입 이벤트 처리 실패 - userId={}, error={}",
                    event.userId(), ex.getMessage(), ex);
        }
    }

    /**
     * 비밀번호 변경 이벤트 — 보안 알림 이메일 발송.
     *
     * @param event 비밀번호 변경 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onPasswordChanged(PasswordChangedEvent event) {
        log.info("비밀번호 변경 이벤트 - userId={}", event.userId());

        try {
            sendPasswordChangedEmail(event);
            sendSecurityNotification(event);
        } catch (Exception ex) {
            log.warn("비밀번호 변경 알림 실패 - userId={}, error={}",
                    event.userId(), ex.getMessage());
        }
    }

    /**
     * 회원 탈퇴 이벤트 — 탈퇴 확인 이메일 발송 및 개인정보 삭제 예약.
     *
     * @param event 회원 탈퇴 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void onUserWithdrawn(UserWithdrawnEvent event) {
        log.info("회원 탈퇴 이벤트 처리 - userId={}", event.userId());

        try {
            sendWithdrawalEmail(event);
            schedulePersonalDataDeletion(event);
        } catch (Exception ex) {
            log.warn("회원 탈퇴 이벤트 처리 실패 - userId={}", event.userId());
        }
    }

    /**
     * 로그인 이상 감지 이벤트 — 보안 경고 이메일 발송.
     *
     * @param event 로그인 이상 이벤트
     */
    @EventListener
    @Async
    public void onSuspiciousLoginDetected(SuspiciousLoginEvent event) {
        log.warn("이상 로그인 감지 - userId={}, ip={}, country={}",
                event.userId(), event.ipAddress(), event.country());
        sendSuspiciousLoginAlert(event);
    }

    // -------------------------------------------------------------------------
    // Private action methods
    // -------------------------------------------------------------------------

    private void sendWelcomeEmail(UserRegisteredEvent event) {
        log.debug("환영 이메일 발송 - to={}", event.email());
        // 실제: emailService.sendWelcome(event.email(), event.userName())
    }

    private void issueWelcomeCoupon(UserRegisteredEvent event) {
        log.debug("신규 가입 쿠폰 발급 - userId={}", event.userId());
        // 실제: couponService.issueCoupon("WELCOME10", event.userId())
    }

    private void sendWelcomeNotification(UserRegisteredEvent event) {
        log.debug("환영 알림 발송 - userId={}", event.userId());
    }

    private void sendPasswordChangedEmail(PasswordChangedEvent event) {
        log.debug("비밀번호 변경 이메일 - to={}", event.email());
    }

    private void sendSecurityNotification(PasswordChangedEvent event) {
        log.debug("보안 알림 발송 - userId={}", event.userId());
    }

    private void sendWithdrawalEmail(UserWithdrawnEvent event) {
        log.debug("탈퇴 확인 이메일 - to={}", event.email());
    }

    private void schedulePersonalDataDeletion(UserWithdrawnEvent event) {
        // GDPR/개인정보보호법에 따라 30일 후 완전 삭제 예약
        log.info("개인정보 삭제 예약 - userId={}, scheduledAt={}", event.userId(),
                event.withdrawnAt().plusDays(30));
    }

    private void sendSuspiciousLoginAlert(SuspiciousLoginEvent event) {
        log.warn("이상 로그인 경고 이메일 발송 - userId={}", event.userId());
    }

    // -------------------------------------------------------------------------
    // Event record types
    // -------------------------------------------------------------------------

    /** 회원 가입 이벤트 */
    public record UserRegisteredEvent(
            Long userId, String email, String userName, LocalDateTime registeredAt
    ) {}

    /** 비밀번호 변경 이벤트 */
    public record PasswordChangedEvent(
            Long userId, String email, String ipAddress, LocalDateTime changedAt
    ) {}

    /** 회원 탈퇴 이벤트 */
    public record UserWithdrawnEvent(
            Long userId, String email, String reason, LocalDateTime withdrawnAt
    ) {}

    /** 이상 로그인 감지 이벤트 */
    public record SuspiciousLoginEvent(
            Long userId, String email, String ipAddress, String country,
            String deviceInfo, LocalDateTime detectedAt
    ) {}
}
