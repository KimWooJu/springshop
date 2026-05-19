package com.springshop.web.dto.request;

import com.springshop.web.validator.PasswordConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 DTO.
 *
 * <p>현재 비밀번호 확인 + 신규 비밀번호 + 확인 입력을 받는다.
 * compact constructor 에서 신규==확인 일치 검증, 현재!=신규 검증을 수행한다.
 */
@Schema(description = "비밀번호 변경 요청")
public record ChangePasswordRequest(
        @Schema(description = "현재 비밀번호", required = true)
        @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,

        @Schema(description = "새 비밀번호 (강도 조건 적용)", required = true)
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @PasswordConstraint
        String newPassword,

        @Schema(description = "새 비밀번호 확인", required = true)
        @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
        String newPasswordConfirm,

        @Schema(description = "비밀번호 변경 후 모든 디바이스 로그아웃 여부", example = "true")
        boolean logoutOtherDevices,

        @Schema(description = "보안 코드 (이메일/SMS 인증) - 민감 작업 보호용")
        String securityCode
) {

    public ChangePasswordRequest {
        if (newPassword != null && newPasswordConfirm != null
                && !newPassword.equals(newPasswordConfirm)) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }
        if (currentPassword != null && newPassword != null
                && currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
    }

    /** 보안 코드를 통한 추가 인증이 필요한지. */
    public boolean requiresSecurityVerification() {
        return securityCode == null || securityCode.isBlank();
    }
}
