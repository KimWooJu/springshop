package com.springshop.web.dto.request;

import com.springshop.web.validator.PhoneNumberConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 배송지 주소 등록 요청 DTO.
 *
 * <p>마이페이지 주소록 추가, 결제 단계의 신규 배송지 입력에서 사용된다.
 * 국내 우편번호 5자리 형식을 검증하고, 별칭/기본 배송지 플래그를 지원한다.
 */
@Schema(description = "배송지 주소 등록 요청")
public record CreateAddressRequest(
        @Schema(description = "주소 별칭", example = "집", required = true)
        @NotBlank(message = "별칭은 필수입니다.")
        @Size(max = 30)
        String alias,

        @Schema(description = "수령인 이름", example = "홍길동", required = true)
        @NotBlank(message = "수령인 이름은 필수입니다.")
        @Size(min = 2, max = 50)
        String recipientName,

        @Schema(description = "연락처", example = "010-1234-5678", required = true)
        @NotBlank(message = "연락처는 필수입니다.")
        @PhoneNumberConstraint
        String phone,

        @Schema(description = "보조 연락처")
        @PhoneNumberConstraint
        String secondaryPhone,

        @Schema(description = "우편번호", example = "06236", required = true)
        @NotBlank(message = "우편번호는 필수입니다.")
        @Pattern(regexp = "^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.")
        String zipCode,

        @Schema(description = "기본 주소", example = "서울특별시 강남구 테헤란로 152", required = true)
        @NotBlank(message = "기본 주소는 필수입니다.")
        @Size(max = 200)
        String street,

        @Schema(description = "상세 주소", example = "강남파이낸스센터 17층")
        @Size(max = 200)
        String detail,

        @Schema(description = "도시", example = "서울특별시")
        @Size(max = 50)
        String city,

        @Schema(description = "도/광역시", example = "서울특별시")
        @Size(max = 50)
        String province,

        @Schema(description = "국가 코드 (ISO 3166-1 alpha-2)", example = "KR")
        @Pattern(regexp = "^[A-Z]{2}$", message = "국가 코드는 2자리 대문자여야 합니다.")
        String country,

        @Schema(description = "기본 배송지 여부", example = "false")
        boolean isDefault,

        @Schema(description = "배송 메모", example = "문 앞에 놓아주세요")
        @Size(max = 200)
        String deliveryMemo
) {

    public CreateAddressRequest {
        if (alias != null) alias = alias.trim();
        if (recipientName != null) recipientName = recipientName.trim();
        if (country == null || country.isBlank()) {
            country = "KR";
        }
    }

    /** 전체 주소 한 줄로 결합. */
    public String fullAddress() {
        StringBuilder sb = new StringBuilder("(").append(zipCode).append(") ").append(street);
        if (detail != null && !detail.isBlank()) sb.append(' ').append(detail);
        return sb.toString();
    }
}
