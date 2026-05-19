package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 결제 처리 요청 DTO.
 *
 * <p>주문 ID + 결제 수단 + PG 토큰을 기반으로 결제를 진행한다.
 * 카드 할부, 부분 결제, 포인트 동시 사용 등 다양한 옵션을 표현한다.
 *
 * <p>compact constructor 에서 금액/할부 개월수의 정합성을 검증한다.
 *
 * <pre>
 *   POST /api/v1/payments
 *   {
 *     "orderId": 9001,
 *     "paymentMethodType": "CARD",
 *     "pgToken": "pg_token_xxx",
 *     "amount": 59000,
 *     "installmentMonths": 3,
 *     "pointsToUse": 1000
 *   }
 * </pre>
 */
@Schema(description = "결제 처리 요청")
public record ProcessPaymentRequest(

        @Schema(description = "주문 ID", example = "9001")
        @NotNull(message = "주문 ID는 필수입니다.")
        @Positive
        Long orderId,

        @Schema(description = "결제 수단",
                allowableValues = {"CARD", "BANK_TRANSFER", "VBANK", "KAKAOPAY", "NAVERPAY", "PAYCO", "POINT_ONLY"})
        @NotBlank(message = "결제 수단은 필수입니다.")
        String paymentMethodType,

        @Schema(description = "PG 인증 토큰", example = "pg_token_xxx")
        @NotBlank(message = "PG 토큰은 필수입니다.")
        @Size(min = 10, max = 500)
        String pgToken,

        @Schema(description = "결제 금액", example = "59000")
        @NotNull(message = "결제 금액은 필수입니다.")
        @DecimalMin(value = "0.0", message = "결제 금액은 0 이상이어야 합니다.")
        BigDecimal amount,

        @Schema(description = "할부 개월수 (0=일시불, 카드 전용)", example = "0", defaultValue = "0")
        @Min(value = 0, message = "할부 개월수는 0 이상이어야 합니다.")
        @Max(value = 36, message = "할부 개월수는 36 이하여야 합니다.")
        Integer installmentMonths,

        @Schema(description = "사용 포인트", example = "0")
        @DecimalMin(value = "0.0", message = "포인트는 0 이상이어야 합니다.")
        BigDecimal pointsToUse,

        @Schema(description = "현금영수증 발급 정보 (사업자/소비자 식별 번호)", nullable = true)
        String cashReceiptNumber,

        @Schema(description = "현금영수증 발급 유형", allowableValues = {"NONE", "PERSONAL", "BUSINESS"})
        String cashReceiptType,

        @Schema(description = "PG 추가 메타데이터")
        Map<String, String> metadata,

        @Schema(description = "고객 사용 디바이스 (모바일/PC 분리 정산용)",
                allowableValues = {"PC", "MOBILE", "APP"})
        String deviceType,

        @Schema(description = "동의: 결제 조건 확인", defaultValue = "false")
        Boolean agreeTerms
) {

    public ProcessPaymentRequest {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("결제 금액은 0 이상이어야 합니다.");
        }
        if (installmentMonths == null) installmentMonths = 0;
        if (installmentMonths > 0 && !"CARD".equals(paymentMethodType)) {
            throw new IllegalArgumentException("할부는 카드 결제에서만 가능합니다.");
        }
        if (pointsToUse == null) pointsToUse = BigDecimal.ZERO;
        if ("POINT_ONLY".equals(paymentMethodType) && pointsToUse.signum() <= 0) {
            throw new IllegalArgumentException("포인트 전액 결제는 사용 포인트가 필요합니다.");
        }
        if (cashReceiptType == null) cashReceiptType = "NONE";
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (agreeTerms == null) agreeTerms = Boolean.FALSE;
    }

    public boolean isInstallment() {
        return installmentMonths != null && installmentMonths > 0;
    }

    public boolean usesPoints() {
        return pointsToUse != null && pointsToUse.signum() > 0;
    }

    public boolean isMobile() {
        return "MOBILE".equalsIgnoreCase(deviceType) || "APP".equalsIgnoreCase(deviceType);
    }
}
