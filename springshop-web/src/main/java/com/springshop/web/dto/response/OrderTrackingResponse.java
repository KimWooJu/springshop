package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 배송 추적 응답.
 *
 * <p>주문 상태 페이지의 배송 진행 막대바에 사용된다.
 * 택배사 API와 연동되어 실시간 배송 단계 이력을 시간순으로 표시한다.
 */
@Schema(description = "주문 배송 추적 응답")
public record OrderTrackingResponse(
        @Schema(description = "주문 ID") Long orderId,
        @Schema(description = "주문 번호") String orderNumber,
        @Schema(description = "현재 상태") String status,
        @Schema(description = "상태 한국어 표시") String statusDisplay,
        @Schema(description = "택배사 코드", example = "CJ_LOGISTICS") String carrierCode,
        @Schema(description = "택배사명") String carrierName,
        @Schema(description = "운송장 번호") String trackingNumber,
        @Schema(description = "택배사 추적 URL") String trackingUrl,
        @Schema(description = "발송 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime shippedAt,
        @Schema(description = "수령(완료) 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime deliveredAt,
        @Schema(description = "도착 예정일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate estimatedDelivery,
        @Schema(description = "수령자 이름") String recipientName,
        @Schema(description = "배송지 주소(요약)") String addressSummary,
        @Schema(description = "배송 단계 이력 (시간순)") List<TrackingStep> steps,
        @Schema(description = "단계별 진행률 (0~100)") int progressPercent
) {

    public OrderTrackingResponse {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    @Schema(description = "배송 단계")
    public record TrackingStep(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime timestamp,
            String location,
            String status,
            String description,
            boolean completed
    ) {

        public static TrackingStep completed(LocalDateTime time, String loc, String status, String desc) {
            return new TrackingStep(time, loc, status, desc, true);
        }

        public static TrackingStep pending(String status, String desc) {
            return new TrackingStep(null, null, status, desc, false);
        }
    }

    /** 진행률 자동 계산. */
    public static int computeProgress(String status) {
        return switch (status) {
            case "PENDING", "PAID" -> 10;
            case "PREPARING" -> 30;
            case "SHIPPED" -> 60;
            case "OUT_FOR_DELIVERY" -> 80;
            case "DELIVERED" -> 100;
            case "CANCELLED", "RETURNED" -> 0;
            case null, default -> 0;
        };
    }

    /** 단순 헬퍼. */
    public static OrderTrackingResponse of(Long orderId, String orderNumber, String status,
                                           String carrierName, String trackingNumber) {
        return new OrderTrackingResponse(
                orderId, orderNumber, status,
                OrderSummaryResponse.toStatusDisplay(status),
                null, carrierName, trackingNumber, null,
                null, null, null, null, null, List.of(), computeProgress(status)
        );
    }
}
