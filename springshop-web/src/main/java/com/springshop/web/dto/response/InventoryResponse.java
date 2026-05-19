package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 응답.
 *
 * <p>관리자 재고 화면, 상품 상세 페이지의 "남은 수량" 표시에 사용된다.
 * 예약(주문 대기)된 수량과 실제 가용 수량을 분리하여 제공한다.
 */
@Schema(description = "재고 응답")
public record InventoryResponse(
        @Schema(description = "재고 ID") Long inventoryId,
        @Schema(description = "상품 ID") Long productId,
        @Schema(description = "옵션 변종 ID (없으면 단품)") Long variantId,
        @Schema(description = "SKU") String sku,
        @Schema(description = "총 입고 수량") int totalQuantity,
        @Schema(description = "예약(미결제 주문) 수량") int reservedQuantity,
        @Schema(description = "가용 수량 (total - reserved)") int availableQuantity,
        @Schema(description = "안전 재고선") int safetyStock,
        @Schema(description = "재고 상태",
                allowableValues = {"IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK", "DISCONTINUED"})
        String status,
        @Schema(description = "재고 부족 경고") boolean lowStockWarning,
        @Schema(description = "창고 위치 코드", example = "WH-SEOUL-A-12") String location,
        @Schema(description = "최근 입고 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastReceivedAt,
        @Schema(description = "최근 출고 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastShippedAt,
        @Schema(description = "최근 조정 이력 (최대 5개)") List<AdjustmentLog> recentAdjustments,
        @Schema(description = "마지막 갱신 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {

    public InventoryResponse {
        recentAdjustments = recentAdjustments == null ? List.of() : List.copyOf(recentAdjustments);
    }

    @Schema(description = "재고 조정 이력")
    public record AdjustmentLog(
            Long id, String type, int quantityDelta, String reason, String operator,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime adjustedAt
    ) {}

    /** 안전 재고 미만이면 LOW_STOCK 자동 판정. */
    public static String computeStatus(int total, int reserved, int safety) {
        int available = total - reserved;
        if (available <= 0) return "OUT_OF_STOCK";
        if (available <= safety) return "LOW_STOCK";
        return "IN_STOCK";
    }

    /** 단순 헬퍼. */
    public static InventoryResponse of(Long inventoryId, Long productId, Long variantId,
                                       int total, int reserved, int safety) {
        int available = total - reserved;
        return new InventoryResponse(
                inventoryId, productId, variantId, null,
                total, reserved, available, safety,
                computeStatus(total, reserved, safety),
                available <= safety,
                null, null, null, List.of(), LocalDateTime.now()
        );
    }

    public boolean isOutOfStock() { return availableQuantity <= 0; }
}
