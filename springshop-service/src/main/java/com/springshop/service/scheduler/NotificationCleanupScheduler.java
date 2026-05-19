package com.springshop.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 알림 데이터 정리 스케줄러.
 *
 * <p>오래된 읽음 알림을 삭제하고, 알림 통계를 집계하며,
 * 미발송 알림 큐를 재처리한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    // private final NotificationService notificationService;

    /**
     * 매일 새벽 1시 30일 이상 된 읽음 알림을 삭제한다.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void cleanupOldReadNotifications() {
        log.info("[스케줄러] 오래된 알림 삭제 시작 - time={}", LocalDateTime.now());

        try {
            // 실제: notificationService.cleanupExpiredNotifications(30)
            int deleted = 0;
            log.info("[스케줄러] 오래된 알림 삭제 완료 - deletedCount={}", deleted);
        } catch (Exception ex) {
            log.error("[스케줄러] 알림 삭제 실패: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 매주 월요일 오전 7시 주간 알림 발송 통계를 집계한다.
     */
    @Scheduled(cron = "0 0 7 * * MON")
    public void aggregateWeeklyNotificationStats() {
        log.info("[스케줄러] 주간 알림 통계 집계 시작");

        try {
            // 실제: 지난 주 알림 발송/읽음률 통계 집계 → 대시보드 업데이트
            log.info("[스케줄러] 주간 알림 통계 집계 완료");
        } catch (Exception ex) {
            log.error("[스케줄러] 알림 통계 집계 실패: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 매 5분마다 실패한 알림 발송을 재시도한다.
     */
    @Scheduled(fixedDelay = 300_000)
    public void retryFailedNotifications() {
        log.debug("[스케줄러] 실패 알림 재시도 확인 - time={}", LocalDateTime.now());

        try {
            // 실제: 발송 실패 상태의 알림 조회 → 재발송 시도
            // 최대 3회 재시도, 초과 시 DLQ 이동
        } catch (Exception ex) {
            log.warn("[스케줄러] 실패 알림 재시도 오류: {}", ex.getMessage());
        }
    }
}
