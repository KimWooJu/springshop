package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JWT 인증 토큰 응답.
 *
 * <p>로그인/리프레시/OAuth2 콜백 등 인증이 완료된 시점에 클라이언트로 발급된다.
 * Access Token + Refresh Token + 사용자 메타 정보를 함께 전달한다.
 */
@Schema(description = "인증 토큰 응답")
public record AuthTokenResponse(
        @Schema(description = "Access Token (JWT)")
        String accessToken,

        @Schema(description = "Refresh Token")
        String refreshToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료까지 남은 초", example = "3600")
        long expiresIn,

        @Schema(description = "Access Token 만료 시각")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime accessTokenExpiresAt,

        @Schema(description = "Refresh Token 만료 시각")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime refreshTokenExpiresAt,

        @Schema(description = "사용자 ID", example = "1024")
        Long userId,

        @Schema(description = "사용자 이메일", example = "alice@example.com")
        String email,

        @Schema(description = "사용자 이름", example = "홍길동")
        String name,

        @Schema(description = "역할", example = "USER")
        String role,

        @Schema(description = "권한 목록")
        List<String> authorities,

        @Schema(description = "최초 로그인 여부", example = "false")
        boolean firstLogin,

        @Schema(description = "비밀번호 변경 필요 여부", example = "false")
        boolean passwordChangeRequired
) {

    public AuthTokenResponse {
        authorities = authorities == null ? List.of() : List.copyOf(authorities);
        if (tokenType == null) tokenType = "Bearer";
    }

    /** 기본 Bearer 토큰 응답 헬퍼. */
    public static AuthTokenResponse of(String access, String refresh, long expiresInSeconds,
                                       Long userId, String email, String name, String role) {
        LocalDateTime now = LocalDateTime.now();
        return new AuthTokenResponse(
                access, refresh, "Bearer", expiresInSeconds,
                now.plusSeconds(expiresInSeconds), now.plusDays(14),
                userId, email, name, role, List.of("ROLE_" + role), false, false
        );
    }

    /** 비밀번호 변경 강제 케이스. */
    public static AuthTokenResponse requirePasswordChange(String access, String refresh,
                                                          Long userId, String email,
                                                          String name, String role) {
        return new AuthTokenResponse(
                access, refresh, "Bearer", 3600,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(14),
                userId, email, name, role, List.of("ROLE_" + role), false, true
        );
    }

    /** Authorization 헤더 값을 반환한다. */
    public String authorizationHeader() {
        return tokenType + " " + accessToken;
    }
}
