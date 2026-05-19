package com.springshop.web.dto.request;

import com.springshop.web.validator.PasswordConstraint;
import com.springshop.web.validator.PhoneNumberConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 회원 가입 요청 DTO.
 *
 * <p>이메일/비밀번호/이름/전화번호 + 약관 동의 플래그를 받는다.
 * 비밀번호는 커스텀 {@link PasswordConstraint} 으로 검증한다.
 *
 * <p>compact constructor 에서 추가로 약관 동의 검증과 정규화(이메일 소문자화)를 수행한다.
 */
@Schema(description = "회원 가입 요청")
public record CreateUserRequest(
        @Schema(description = "이메일", example = "alice@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @Schema(description = "비밀번호 (대소문자+숫자+특수문자 포함 8자 이상)")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @PasswordConstraint
        String password,

        @Schema(description = "비밀번호 확인")
        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm,

        @Schema(description = "이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다.")
        String name,

        @Schema(description = "닉네임", example = "shopper-7")
        @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
        String nickname,

        @Schema(description = "전화번호", example = "010-1234-5678")
        @PhoneNumberConstraint
        String phone,

        @Schema(description = "생년월일")
        LocalDate birthDate,

        @Schema(description = "성별", example = "M",
                allowableValues = {"M", "F", "N"})
        String gender,

        @Schema(description = "추천인 코드 (선택)")
        @Size(max = 20)
        String referralCode,

        @Schema(description = "이용 약관 동의 (필수)")
        @AssertTrue(message = "이용 약관에 동의해야 합니다.")
        boolean agreeTerms,

        @Schema(description = "개인정보 처리방침 동의 (필수)")
        @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
        boolean agreePrivacy,

        @Schema(description = "만 14세 이상 확인 (필수)")
        @AssertTrue(message = "만 14세 이상이어야 가입 가능합니다.")
        boolean ageOver14,

        @Schema(description = "마케팅 수신 동의 (선택)")
        boolean agreeMarketing
) {
    public CreateUserRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (password != null && passwordConfirm != null && !password.equals(passwordConfirm)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        if (birthDate != null && birthDate.isAfter(LocalDate.now().minusYears(14))) {
            throw new IllegalArgumentException("만 14세 이상만 가입 가능합니다.");
        }
    }
}
