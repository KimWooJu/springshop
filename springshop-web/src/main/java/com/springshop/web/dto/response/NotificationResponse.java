package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 응답.
 *
 * <p>인앱 알림 센터, 푸시 알림 히스토리에 사용된다.
 * 알림 종류(주문/결제/배송/마케팅/공지)별 type 필드와 클릭 시 이동할 deepLink를 포함한다.
 */
@Schema(description = "알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID") Long id,
        @Schema(description = "수신자 사용자 ID") Long userId,
        @Schema(description = "알림 타입",
                allowableValues = {"ORDER", "PAYMENT", "SHIPPING", "REVIEW", "COUPON",
                        "MARKETING", "SYSTEM", "REPLY", "STOCK"})
        String type,
        @Schema(description = "알림 카테고리", example = "TRANSACTIONAL")
        String category,
        @Schema(description = "제목") String title,
        @Schema(description = "본문") String content,
        @Schema(description = "썸네일 이미지") String thumbnailUrl,
        @Schema(description = "클릭 시 이동할 딥링크", example = "/orders/12345") String deepLink,
        @Schema(description = "읽음 여부", example = "false") boolean isRead,
        @Schema(description = "중요 알림 여부", example = "true") boolean important,
        @Schema(description = "푸시 발송 채널",
                allowableValues = {"IN_APP", "PUSH", "EMAIL", "SMS"})
        String channel,
        @Schema(description = "관련 리소스 ID (주문/상품 등)") Long resourceId,
        @Schema(description = "관련 리소스 타입") String resourceType,
        @Schema(description = "추가 메타 데이터") Map<String, Object> metadata,
        @Schema(description = "읽음 처리 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime readAt,
        @Schema(description = "생성 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @Schema(description = "만료 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime expiresAt
) {

    public NotificationResponse {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** 알림이 만료되었는지 여부. */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /** "방금 전", "5분 전", "어제" 같은 표시용 문자열. */
    public String relativeTime() {
        java.time.Duration d = java.time.Duration.between(createdAt, LocalDateTime.now());
        long mins = d.toMinutes();
        if (mins < 1) return "방금 전";
        if (mins < 60) return mins + "분 전";
        long hours = d.toHours();
        if (hours < 24) return hours + "시간 전";
        long days = d.toDays();
        if (days < 7) return days + "일 전";
        return createdAt.toLocalDate().toString();
    }

    /** 단순 헬퍼 */
    public static NotificationResponse of(Long id, Long userId, String type,
                                          String title, String content, String deepLink) {
        return new NotificationResponse(
                id, userId, type, "TRANSACTIONAL", title, content, null, deepLink,
                false, false, "IN_APP", null, null, Map.of(), null,
                LocalDateTime.now(), null
        );
    }
}
