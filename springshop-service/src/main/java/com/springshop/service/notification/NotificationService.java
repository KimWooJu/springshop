package com.springshop.service.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 사용자 알림 서비스 인터페이스.
 *
 * <p>인앱 알림 발송(단건/대량), 읽음 처리, 알림 조회,
 * 주문/프로모션별 전용 알림 생성 메서드를 정의한다.
 */
public interface NotificationService {

    /**
     * 단건 알림을 발송한다.
     *
     * @param userId    수신자 ID
     * @param type      알림 유형 (ORDER_STATUS, PROMOTION, SYSTEM 등)
     * @param title     알림 제목
     * @param message   알림 내용
     * @param metadata  추가 메타데이터 (연관 엔티티 ID 등)
     * @return          생성된 알림 ID
     */
    Long sendNotification(Long userId, String type, String title,
                          String message, Map<String, String> metadata);

    /**
     * 여러 사용자에게 동일한 알림을 일괄 발송한다.
     * Virtual Thread로 병렬 처리하여 대량 발송 성능을 높인다.
     *
     * @param userIds  수신자 ID 목록
     * @param type     알림 유형
     * @param title    알림 제목
     * @param message  알림 내용
     * @return         발송 성공 수
     */
    int sendBulkNotification(List<Long> userIds, String type, String title, String message);

    /**
     * 사용자 알림 목록을 페이징 조회한다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 알림 페이지
     */
    Page<NotificationDto> getNotifications(Long userId, Pageable pageable);

    /**
     * 알림 단건을 조회한다.
     *
     * @param notificationId 알림 ID
     * @return NotificationDto
     */
    NotificationDto getNotification(Long notificationId);

    /**
     * 알림을 읽음 처리한다.
     *
     * @param notificationId 알림 ID
     * @param userId         처리 요청자 ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 사용자의 모든 알림을 읽음 처리한다.
     *
     * @param userId 사용자 ID
     * @return 처리된 알림 수
     */
    int markAllAsRead(Long userId);

    /**
     * 알림을 삭제한다.
     *
     * @param notificationId 알림 ID
     * @param userId         삭제 요청자 ID
     */
    void deleteNotification(Long notificationId, Long userId);

    /**
     * 미읽음 알림 수를 반환한다.
     *
     * @param userId 사용자 ID
     * @return 미읽음 알림 수
     */
    long getUnreadCount(Long userId);

    /**
     * 주문 상태 변경 알림을 발송한다.
     *
     * @param userId    수신자 ID
     * @param orderId   주문 ID
     * @param orderNo   주문 번호
     * @param newStatus 새 주문 상태
     */
    void sendOrderStatusNotification(Long userId, Long orderId, String orderNo, String newStatus);

    /**
     * 프로모션 알림을 발송한다.
     *
     * @param userId        수신자 ID
     * @param promotionTitle 프로모션 제목
     * @param promotionUrl   프로모션 URL
     */
    void sendPromotionNotification(Long userId, String promotionTitle, String promotionUrl);

    /**
     * 만료된 알림을 일괄 삭제한다 (스케줄러 호출).
     *
     * @param daysOld 보관 기간(일) 이상 된 알림 삭제
     * @return 삭제된 알림 수
     */
    int cleanupExpiredNotifications(int daysOld);

    /**
     * 알림 데이터 전송 객체.
     */
    record NotificationDto(
            Long id,
            Long userId,
            String type,
            String title,
            String message,
            boolean isRead,
            Map<String, String> metadata,
            java.time.LocalDateTime readAt,
            java.time.LocalDateTime createdAt
    ) {}
}
