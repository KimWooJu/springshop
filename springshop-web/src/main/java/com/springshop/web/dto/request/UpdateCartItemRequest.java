package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 장바구니 아이템 수량 변경 요청 DTO.
 *
 * <p>수량 변경, 옵션 변경, 선택 토글, 즉시 삭제(quantity=0) 의도를 표현한다.
 *
 * <p>compact constructor 에서 비정상 입력 거부 및 정규화를 수행한다.
 */
@Schema(description = "장바구니 아이템 수정 요청")
public record UpdateCartItemRequest(

        @Schema(description = "변경 수량 (0 이면 삭제)", example = "3")
        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 0, message = "수량은 0 이상이어야 합니다.")
        @Max(value = 999, message = "수량은 999 이하여야 합니다.")
        Integer quantity,

        @Schema(description = "옵션 변경 시 새 옵션 ID", nullable = true)
        Long newVariantId,

        @Schema(description = "결제 대상으로 선택할지 여부", defaultValue = "true")
        Boolean selected,

        @Schema(description = "선물 포장 여부")
        Boolean giftWrap,

        @Schema(description = "주문서 메모 (선택)")
        String memo
) {

    public UpdateCartItemRequest {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }
        if (selected == null) {
            selected = Boolean.TRUE;
        }
        if (giftWrap == null) {
            giftWrap = Boolean.FALSE;
        }
        if (memo != null && memo.length() > 200) {
            throw new IllegalArgumentException("메모는 200자 이하여야 합니다.");
        }
    }

    /** 삭제 요청인지 여부. */
    public boolean isRemoveRequest() {
        return quantity != null && quantity == 0;
    }

    /** 옵션 변경 요청 여부. */
    public boolean isVariantChange() {
        return newVariantId != null && newVariantId > 0;
    }
}
