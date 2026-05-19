package com.springshop.web.dto.request;

import com.springshop.web.validator.PhoneNumberConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 주소 수정 요청 DTO.
 *
 * <p>부분 업데이트 - null 이 아닌 필드만 갱신한다.
 * 기본 배송지 플래그는 별도 PUT /{id}/default 엔드포인트로 처리할 수도 있지만
 * 편의를 위해 본 DTO 에서도 지원한다.
 */
@Schema(description = "주소 수정 요청")
public record UpdateAddressRequest(
        @Schema(description = "주소 별칭")
        @Size(max = 30)
        String alias,

        @Schema(description = "수령인 이름")
        @Size(min = 2, max = 50)
        String recipientName,

        @Schema(description = "연락처")
        @PhoneNumberConstraint
        String phone,

        @Schema(description = "보조 연락처")
        @PhoneNumberConstraint
        String secondaryPhone,

        @Schema(description = "우편번호")
        @Pattern(regexp = "^\\d{5}$")
        String zipCode,

        @Schema(description = "기본 주소")
        @Size(max = 200)
        String street,

        @Schema(description = "상세 주소")
        @Size(max = 200)
        String detail,

        @Schema(description = "도시")
        @Size(max = 50)
        String city,

        @Schema(description = "도/광역시")
        @Size(max = 50)
        String province,

        @Schema(description = "국가 코드")
        @Pattern(regexp = "^[A-Z]{2}$")
        String country,

        @Schema(description = "기본 배송지 여부 (null=변경 안 함)")
        Boolean isDefault,

        @Schema(description = "배송 메모")
        @Size(max = 200)
        String deliveryMemo
) {

    public UpdateAddressRequest {
        if (alias != null) alias = alias.trim();
        if (recipientName != null) recipientName = recipientName.trim();
    }

    /** 적어도 하나의 필드가 갱신 대상인지. */
    public boolean hasChanges() {
        return alias != null || recipientName != null || phone != null
                || secondaryPhone != null || zipCode != null || street != null
                || detail != null || city != null || province != null
                || country != null || isDefault != null || deliveryMemo != null;
    }
}
