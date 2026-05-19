package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 상품 일괄 처리 요청 DTO (관리자).
 *
 * <p>여러 상품에 대해 활성화/비활성화/품절 표기/가격 변경 등의 일괄 작업을 표현한다.
 *
 * <p>compact constructor 에서 중복 ID 제거, 최대 개수 제한을 수행한다.
 */
@Schema(description = "상품 일괄 처리 요청 (관리자)")
public record BulkProductRequest(

        @Schema(description = "대상 상품 ID 목록")
        @NotEmpty(message = "상품 ID 목록은 비어있을 수 없습니다.")
        @Size(max = 500, message = "한 번에 최대 500개까지 처리 가능합니다.")
        List<Long> productIds,

        @Schema(description = "수행할 작업")
        @NotNull(message = "작업 유형은 필수입니다.")
        BulkAction action,

        @Schema(description = "작업 사유 (감사 로그)")
        String reason,

        @Schema(description = "PRICE_CHANGE 작업 시 새 가격")
        java.math.BigDecimal newPrice,

        @Schema(description = "CATEGORY_CHANGE 작업 시 새 카테고리 ID")
        Long newCategoryId,

        @Schema(description = "STATUS_CHANGE 작업 시 새 상태")
        String newStatus,

        @Schema(description = "Dry-run 모드 (실제 실행하지 않고 검증만)", defaultValue = "false")
        Boolean dryRun
) {

    public BulkProductRequest {
        if (productIds != null) {
            productIds = productIds.stream().distinct().toList();
        }
        if (dryRun == null) dryRun = Boolean.FALSE;
        if (action == BulkAction.PRICE_CHANGE && newPrice == null) {
            throw new IllegalArgumentException("PRICE_CHANGE 작업은 newPrice가 필요합니다.");
        }
        if (action == BulkAction.CATEGORY_CHANGE && newCategoryId == null) {
            throw new IllegalArgumentException("CATEGORY_CHANGE 작업은 newCategoryId가 필요합니다.");
        }
        if (action == BulkAction.STATUS_CHANGE && (newStatus == null || newStatus.isBlank())) {
            throw new IllegalArgumentException("STATUS_CHANGE 작업은 newStatus가 필요합니다.");
        }
    }

    public int size() {
        return productIds == null ? 0 : productIds.size();
    }

    /** 일괄 처리 작업 유형. */
    public enum BulkAction {
        ACTIVATE,
        DEACTIVATE,
        DELETE,
        SOLD_OUT,
        RESTOCK,
        PRICE_CHANGE,
        CATEGORY_CHANGE,
        STATUS_CHANGE,
        FEATURE_ON,
        FEATURE_OFF
    }
}
