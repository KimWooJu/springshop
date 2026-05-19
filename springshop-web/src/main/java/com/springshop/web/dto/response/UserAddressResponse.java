package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 사용자 주소 응답.
 *
 * <p>배송지 관리, 결제 화면에서 사용된다. 기본 주소 여부와 별칭(집/회사 등)을 포함한다.
 */
@Schema(description = "사용자 주소 응답")
public record UserAddressResponse(
        @Schema(description = "주소 ID", example = "5001")
        Long id,

        @Schema(description = "사용자 ID", example = "1024")
        Long userId,

        @Schema(description = "주소 별칭", example = "집")
        String alias,

        @Schema(description = "수령인 이름", example = "홍길동")
        String recipientName,

        @Schema(description = "연락처", example = "010-1234-5678")
        String phone,

        @Schema(description = "우편번호", example = "06236")
        String zipCode,

        @Schema(description = "기본 주소", example = "서울특별시 강남구 테헤란로 152")
        String street,

        @Schema(description = "상세 주소", example = "강남파이낸스센터 17층")
        String detail,

        @Schema(description = "도시", example = "서울특별시")
        String city,

        @Schema(description = "도/광역시", example = "서울특별시")
        String province,

        @Schema(description = "국가 코드 (ISO 3166-1 alpha-2)", example = "KR")
        String country,

        @Schema(description = "기본 배송지 여부", example = "true")
        boolean isDefault,

        @Schema(description = "배송 메모", example = "문 앞에 놓아주세요")
        String deliveryMemo,

        @Schema(description = "등록 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {

    /** 표시용으로 결합된 주소 한 줄을 반환한다. */
    public String fullAddress() {
        StringBuilder sb = new StringBuilder();
        if (zipCode != null && !zipCode.isBlank()) {
            sb.append("(").append(zipCode).append(") ");
        }
        if (street != null) sb.append(street).append(' ');
        if (detail != null) sb.append(detail);
        return sb.toString().trim();
    }

    /** 다른 주소로 복제하면서 기본 배송지 플래그만 변경. */
    public UserAddressResponse asDefault(boolean defaultFlag) {
        return new UserAddressResponse(
                id, userId, alias, recipientName, phone, zipCode, street, detail,
                city, province, country, defaultFlag, deliveryMemo, createdAt, updatedAt
        );
    }

    /** 새 주소 등록 즉시 응답 헬퍼. */
    public static UserAddressResponse of(Long id, Long userId, String alias,
                                         String recipientName, String phone,
                                         String zipCode, String street, String detail,
                                         boolean isDefault) {
        LocalDateTime now = LocalDateTime.now();
        return new UserAddressResponse(
                id, userId, alias, recipientName, phone, zipCode, street, detail,
                null, null, "KR", isDefault, null, now, now
        );
    }
}
