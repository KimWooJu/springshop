package com.springshop.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * {@link NotificationService} 구현체.
 *
 * <p>대량 알림 발송은 Java 25 Virtual Thread 기반 ExecutorService로 병렬 처리하여
 * 수천 명의 수신자에게도 빠르게 전송한다. 알림 유형별 메시지 생성은
 * Java 15+ Text Block으로 가독성 높게 작성한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    // 실제 구현에서는 NotificationRepository를 주입한다.
    // private final NotificationRepository notificationRepository;

    /** Virtual Thread 기반 대량 발송 Executor */
    private static final ExecutorService BULK_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public Long sendNotification(Long userId, String type, String title,
                                 String message, Map<String, String> metadata) {
        log.info("알림 발송 - userId={}, type={}, title={}", userId, type, title);

        // 알림 유형별 메시지 보강
        String enrichedMessage = enrichMessage(type, message, metadata);

        // 실제: new Notification(...) → notificationRepository.save()
        Long notificationId = System.currentTimeMillis();

        log.debug("알림 저장 완료 - notificationId={}, userId={}", notificationId, userId);
        return notificationId;
    }

    @Override
    @Async
    public int sendBulkNotification(List<Long> userIds, String type, String title, String message) {
        log.info("대량 알림 발송 시작 - userCount={}, type={}", userIds.size(), type);

        List<Future<Boolean>> futures = new ArrayList<>();
        int successCount = 0;

        for (Long userId : userIds) {
            futures.add(BULK_EXECUTOR.submit(() -> {
                try {
                    sendNotification(userId, type, title, message, Map.of());
                    return true;
                } catch (Exception ex) {
                    log.warn("알림 발송 실패 - userId={}, error={}", userId, ex.getMessage());
                    return false;
                }
            }));
        }

        for (Future<Boolean> future : futures) {
            try {
                if (Boolean.TRUE.equals(future.get())) {
                    successCount++;
                }
            } catch (Exception ex) {
                log.error("대량 알림 Future 처리 오류: {}", ex.getMessage());
            }
        }

        log.info("대량 알림 발송 완료 - success={}/{}", successCount, userIds.size());
        return successCount;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(Long userId, Pageable pageable) {
        log.debug("알림 목록 조회 - userId={}", userId);
        List<NotificationDto> notifications = List.of(
                buildMockNotification(1L, userId, "ORDER_STATUS", false),
                buildMockNotification(2L, userId, "PROMOTION", true)
        );
        return new PageImpl<>(notifications, pageable, 10);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto getNotification(Long notificationId) {
        log.debug("알림 단건 조회 - notificationId={}", notificationId);
        return buildMockNotification(notificationId, 100L, "ORDER_STATUS", false);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        log.debug("알림 읽음 처리 - notificationId={}, userId={}", notificationId, userId);
        // 실제: notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now())
    }

    @Override
    public int markAllAsRead(Long userId) {
        log.info("전체 알림 읽음 처리 - userId={}", userId);
        // 실제: notificationRepository.markAllAsRead(userId, LocalDateTime.now())
        return 5; // mock: 5개 처리됨
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        log.info("알림 삭제 - notificationId={}, userId={}", notificationId, userId);
        // 실제: notificationRepository.deleteByIdAndUserId(notificationId, userId)
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        // 실제: notificationRepository.countByUserIdAndIsReadFalse(userId)
        return 3L;
    }

    @Override
    public void sendOrderStatusNotification(Long userId, Long orderId, String orderNo, String newStatus) {
        log.info("주문 상태 알림 - userId={}, orderId={}, status={}", userId, orderId, newStatus);

        String title = "주문 상태 변경";
        String message = generateOrderStatusMessage(orderNo, newStatus);
        Map<String, String> metadata = Map.of("orderId", String.valueOf(orderId), "orderNo", orderNo);

        sendNotification(userId, "ORDER_STATUS", title, message, metadata);
    }

    @Override
    public void sendPromotionNotification(Long userId, String promotionTitle, String promotionUrl) {
        log.info("프로모션 알림 - userId={}, title={}", userId, promotionTitle);

        String message = """
                %s
                지금 바로 확인해보세요! 👉 %s
                """.formatted(promotionTitle, promotionUrl);

        sendNotification(userId, "PROMOTION", promotionTitle, message,
                Map.of("url", promotionUrl));
    }

    @Override
    public int cleanupExpiredNotifications(int daysOld) {
        log.info("만료 알림 정리 - daysOld={}", daysOld);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysOld);
        // 실제: notificationRepository.deleteByCreatedAtBeforeAndIsRead(cutoff, true)
        log.info("만료 알림 정리 완료 - cutoff={}", cutoff);
        return 0;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String enrichMessage(String type, String message, Map<String, String> metadata) {
        return switch (type) {
            case "ORDER_STATUS" -> {
                String orderId = metadata.getOrDefault("orderNo", "");
                yield "[주문 #%s] %s".formatted(orderId, message);
            }
            case "PROMOTION" -> "🎉 " + message;
            case "SYSTEM"    -> "[시스템] " + message;
            case "ALERT"     -> "⚠️ " + message;
            default           -> message;
        };
    }

    private String generateOrderStatusMessage(String orderNo, String status) {
        return switch (status) {
            case "PAYMENT_COMPLETED" -> "주문 #%s의 결제가 완료되었습니다.".formatted(orderNo);
            case "PREPARING"         -> "주문 #%s 상품을 준비 중입니다.".formatted(orderNo);
            case "SHIPPED"           -> "주문 #%s가 배송 시작되었습니다. 배송 조회로 현황을 확인하세요.".formatted(orderNo);
            case "DELIVERED"         -> "주문 #%s가 배송 완료되었습니다. 구매 확정 후 리뷰를 남겨주세요.".formatted(orderNo);
            case "CANCELLED"         -> "주문 #%s가 취소 처리되었습니다.".formatted(orderNo);
            default                  -> "주문 #%s 상태가 변경되었습니다: %s".formatted(orderNo, status);
        };
    }

    private NotificationDto buildMockNotification(Long id, Long userId, String type, boolean isRead) {
        return new NotificationDto(
                id, userId, type,
                type.equals("ORDER_STATUS") ? "주문 상태 변경" : "프로모션 알림",
                type.equals("ORDER_STATUS") ? "주문이 배송 시작되었습니다." : "특별 할인 이벤트를 확인하세요!",
                isRead, Map.of(),
                isRead ? LocalDateTime.now().minusHours(1) : null,
                LocalDateTime.now().minusDays(1)
        );
    }
}
