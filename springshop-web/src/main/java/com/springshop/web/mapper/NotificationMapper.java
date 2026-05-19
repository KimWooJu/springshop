package com.springshop.web.mapper;

import com.springshop.domain.notification.Notification;
import com.springshop.web.dto.response.NotificationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 알림 엔티티 ↔ 응답 DTO 매퍼.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    /**
     * Notification 엔티티를 응답 DTO로 변환한다.
     *
     * @param notification 변환할 알림 엔티티
     * @return NotificationResponse DTO
     */
    @Mapping(target = "type", source = "notificationType")
    @Mapping(target = "typeDisplay", ignore = true)
    @Mapping(target = "actionUrl", ignore = true)
    NotificationResponse toResponse(Notification notification);

    /**
     * Notification 엔티티 목록을 응답 DTO 목록으로 변환한다.
     */
    List<NotificationResponse> toResponseList(List<Notification> notifications);

    /**
     * 알림 유형을 표시용 한글 레이블로 변환한다.
     *
     * @param type 알림 유형 코드
     * @return 표시용 레이블
     */
    default String mapTypeDisplay(String type) {
        if (type == null) return "알림";
        return switch (type.toUpperCase()) {
            case "ORDER_STATUS"   -> "주문 알림";
            case "PROMOTION"      -> "프로모션";
            case "SYSTEM"         -> "시스템";
            case "ALERT"          -> "경고";
            case "REVIEW_REQUEST" -> "리뷰 요청";
            case "COUPON"         -> "쿠폰";
            default               -> type;
        };
    }
}
