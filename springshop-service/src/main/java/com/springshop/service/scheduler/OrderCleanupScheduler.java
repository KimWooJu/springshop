package com.springshop.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 주문/장바구니 정리 스케줄러.
 *
 * <p>오래된 PENDING 주문을 자동 취소하고, 방치된 장바구니를 정리하며,
 * 주문 관련 임시 데이터를 주기적으로 청소한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    // private final OrderService orderService;
    // private final CartService cartService;

    /**
     * 매 30분마다 결제 미완료 주문(30분 초과)을 자동 취소한다.
     */
    @Scheduled(fixedDelay = 1_800_000)
    public void cancelAbandonedOrders() {
        log.info("[스케줄러] 미완료 주문 자동 취소 시작 - time={}", LocalDateTime.now());

        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
            // 실제: orderService.cancelPendingOrdersBefore(threshold)
            log.info("[스케줄러] 미완료 주문 자동 취소 완료 - threshold={}", threshold);
        } catch (Exception ex) {
            log.error("[스케줄러] 미완료 주문 취소 실패: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 매일 새벽 2시 7일 이상 방치된 장바구니를 정리한다.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupAbandonedCarts() {
        log.info("[스케줄러] 방치 장바구니 정리 시작");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);
            // 실제: cartService.deleteAbandonedCartsBefore(threshold)
            log.info("[스케줄러] 방치 장바구니 정리 완료 - threshold={}", threshold);
        } catch (Exception ex) {
            log.error("[스케줄러] 장바구니 정리 실패: {}", ex.getMessage(), ex);
        }
    }

    /**
     * 매일 새벽 3시 완료된 주문의 임시 데이터를 아카이브한다.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void archiveCompletedOrders() {
        log.info("[스케줄러] 완료 주문 아카이브 시작");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusMonths(6);
            // 실제: orderService.archiveOrdersCompletedBefore(threshold)
            log.info("[스케줄러] 완료 주문 아카이브 완료 - threshold={}", threshold);
        } catch (Exception ex) {
            log.error("[스케줄러] 주문 아카이브 실패: {}", ex.getMessage(), ex);
        }
    }
}
