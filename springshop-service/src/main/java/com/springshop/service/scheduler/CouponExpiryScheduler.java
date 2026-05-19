package com.springshop.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 쿠폰 만료 처리 스케줄러.
 *
 * <p>기간 만료 쿠폰을 일괄 만료 처리하고, 만료 임박 쿠폰에 대해
 * 사용자에게 사전 알림을 발송하여 쿠폰 활용을 독려한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponExpiryScheduler {

    // private final CouponService couponService;
    // private final NotificationService notificationService;

    /**
     * 매일 자정 만료 쿠폰을 일괄 처리한다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredCoupons() {
        log.info("[스케줄러] 쿠폰 만료 처리 시작 - date={}", LocalDate.now());

        try {
            // 실제: int count = couponService.expireCoupons()
            int count = 0;
            log.info("[스케줄러] 쿠폰 만료 처리 완료 - processedCount={}", count);
        } catch (Exception ex) {
            log.error("[스케줄러] 쿠폰 만료 처리 실패: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 매일 오전 10시 D-3 만료 예정 쿠폰에 대해 사용 유도 알림을 발송한다.
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendExpiryWarningNotifications() {
        log.info("[스케줄러] 쿠폰 만료 임박 알림 발송 - targetDate={}",
                LocalDate.now().plusDays(3));

        try {
            // 실제: D-3 만료 쿠폰 보유자 조회 → 알림 발송
            log.info("[스케줄러] 쿠폰 만료 임박 알림 발송 완료");
        } catch (Exception ex) {
            log.error("[스케줄러] 쿠폰 만료 임박 알림 실패: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 매주 일요일 오후 11시 미사용 만료 쿠폰 통계를 집계한다.
     */
    @Scheduled(cron = "0 0 23 * * SUN")
    public void aggregateExpiredCouponStatistics() {
        log.info("[스케줄러] 미사용 만료 쿠폰 통계 집계 시작 - time={}", LocalDateTime.now());

        try {
            // 실제: 이번 주 만료된 미사용 쿠폰 통계 → 관리자 리포트
            log.info("[스케줄러] 미사용 만료 쿠폰 통계 집계 완료");
        } catch (Exception ex) {
            log.error("[스케줄러] 쿠폰 통계 집계 실패: {}", ex.getMessage(), ex);
        }
    }
}
