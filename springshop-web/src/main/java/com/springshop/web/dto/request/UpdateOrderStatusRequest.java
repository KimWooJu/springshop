package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 주문 상태 변경 요청 DTO (관리자).
 *
 * <p>관리자가 주문 상태를 수동으로 변경할 때 사용된다.
 * 일반적으로 결제 완료 후 자동 상태 전환되지만, 예외 케이스에서 사용된다.
 */
@Schema(description = "주문 상태 변경 요청")
public record UpdateOrderStatusRequest(
        @Schema(description = "신규 상태", required = true,
                allowableValues = {"PENDING", "PAID", "PREPARING", "SHIPPED",
                        "DELIVERED", "CANCELLED", "RETURNED", "REFUNDED"})
        @NotNull
        @Pattern(regexp = "^(PENDING|PAID|PREPARING|SHIPPED|DELIVERED|CANCELLED|RETURNED|REFUNDED)$")
        String status,

        @Schema(description = "변경 사유 (감사 로그)", required = true)
        @NotNull
        @Size(min = 5, max = 500)
        String reason,

        @Schema(description = "운송장 번호 (SHIPPED 로 변경 시 필수)")
        @Size(max = 50)
        String trackingNumber,

        @Schema(description = "택배사 코드")
        @Size(max = 30)
        String carrierCode,

        @Schema(description = "고객 알림 발송 여부", example = "true")
        boolean notifyCustomer,

        @Schema(description = "알림 메시지 (notifyCustomer=true 시)")
        @Size(max = 1000)
        String customMessage,

        @Schema(description = "관리자 내부 메모 (고객 비공개)")
        @Size(max = 1000)
        String internalNote
) {

    public UpdateOrderStatusRequest {
        if ("SHIPPED".equals(status) && (trackingNumber == null || trackingNumber.isBlank())) {
            throw new IllegalArgumentException("배송 상태로 변경 시 운송장 번호가 필요합니다.");
        }
        if (reason != null) reason = reason.trim();
    }

    /** 고객 알림 필요 여부 빠르게 판별. */
    public boolean shouldNotify() {
        return notifyCustomer && switch (status) {
            case "PAID", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED" -> true;
            default -> false;
        };
    }
}
