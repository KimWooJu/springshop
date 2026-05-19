package com.springshop.web.dto.request;

import com.springshop.web.validator.PhoneNumberConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 회원 정보 수정 요청 DTO.
 *
 * <p>부분 업데이트를 지원하기 위해 모든 필드는 nullable.
 * null 이 아닌 필드만 갱신한다. 비밀번호 변경은 별도 엔드포인트 사용.
 */
@Schema(description = "회원 정보 수정 요청")
public record UpdateUserRequest(
        @Schema(description = "이름")
        @Size(min = 2, max = 50)
        String name,

        @Schema(description = "닉네임")
        @Size(min = 2, max = 30)
        String nickname,

        @Schema(description = "전화번호")
        @PhoneNumberConstraint
        String phone,

        @Schema(description = "한 줄 소개")
        @Size(max = 200)
        String bio,

        @Schema(description = "프로필 이미지 URL")
        @Size(max = 500)
        @Pattern(regexp = "^(https?://).+",
                message = "URL 은 http(s):// 로 시작해야 합니다.")
        String profileImageUrl,

        @Schema(description = "커버 이미지 URL")
        @Size(max = 500)
        String coverImageUrl,

        @Schema(description = "생년월일 (변경 1회 한정)")
        LocalDate birthDate,

        @Schema(description = "성별", example = "M",
                allowableValues = {"M", "F", "N"})
        @Pattern(regexp = "^(M|F|N)$")
        String gender,

        @Schema(description = "직업")
        @Size(max = 50)
        String occupation,

        @Schema(description = "관심 카테고리 슬러그 (콤마구분)")
        @Size(max = 500)
        String interestSlugs,

        @Schema(description = "마케팅 수신 동의")
        Boolean agreeMarketing,

        @Schema(description = "이메일 수신 동의")
        Boolean agreeEmailNotification,

        @Schema(description = "SMS 수신 동의")
        Boolean agreeSmsNotification,

        @Schema(description = "푸시 수신 동의")
        Boolean agreePushNotification
) {

    public UpdateUserRequest {
        if (name != null) name = name.trim();
        if (nickname != null) nickname = nickname.trim();
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("미래 날짜는 생년월일이 될 수 없습니다.");
        }
    }

    /** 모든 필드가 null이면 "수정할 내용이 없음"으로 간주. */
    public boolean isEmpty() {
        return name == null && nickname == null && phone == null && bio == null
                && profileImageUrl == null && coverImageUrl == null && birthDate == null
                && gender == null && occupation == null && interestSlugs == null
                && agreeMarketing == null && agreeEmailNotification == null
                && agreeSmsNotification == null && agreePushNotification == null;
    }
}
