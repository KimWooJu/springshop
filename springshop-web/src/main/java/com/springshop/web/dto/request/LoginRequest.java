package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인 요청 DTO.
 *
 * <p>이메일/비밀번호 기본 로그인. 추가로 rememberMe 옵션과
 * 디바이스 식별 정보를 받아 멀티 디바이스 세션 관리에 활용한다.
 */
@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "이메일", example = "alice@example.com", required = true)
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100)
        String email,

        @Schema(description = "비밀번호", required = true)
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 100, message = "비밀번호는 8~100자여야 합니다.")
        String password,

        @Schema(description = "로그인 상태 유지 여부", example = "false")
        boolean rememberMe,

        @Schema(description = "디바이스 ID (고유 식별자)")
        @Size(max = 100)
        String deviceId,

        @Schema(description = "디바이스 종류",
                allowableValues = {"WEB", "ANDROID", "IOS", "DESKTOP"})
        String deviceType,

        @Schema(description = "디바이스 표시명", example = "Chrome on MacBook Pro")
        @Size(max = 200)
        String deviceName,

        @Schema(description = "OS 종류", example = "macOS 14.5")
        @Size(max = 100)
        String osVersion,

        @Schema(description = "앱 버전 (모바일)", example = "1.4.2")
        @Size(max = 20)
        String appVersion,

        @Schema(description = "캡차 토큰 (5회 이상 실패 시 필수)")
        @Size(max = 1000)
        String captchaToken,

        @Schema(description = "2FA OTP 코드 (2FA 활성화 시 필수)")
        @Size(min = 6, max = 6)
        String otpCode
) {

    public LoginRequest {
        if (email != null) email = email.trim().toLowerCase();
    }

    /** 자동 발급 가능한 토큰 만료 시간 (초). */
    public long preferredExpiresIn() {
        return rememberMe ? 30L * 24 * 3600 : 3600L;
    }

    /** 2FA 검증 필요 여부. */
    public boolean requires2FA() {
        return otpCode == null;
    }
}
