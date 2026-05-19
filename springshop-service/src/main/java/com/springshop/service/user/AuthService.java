package com.springshop.service.user;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 인증 / 토큰 관리 서비스.
 *
 * <p>JWT 발급/갱신, 블랙리스트 관리, OAuth2 소셜 로그인 토큰 교환,
 * 세션 정보 조회 등 인증 흐름의 핵심 기능을 담당한다.
 */
public interface AuthService {

    /** 이메일/비밀번호 기반 로그인. 액세스 + 리프레시 토큰을 발급한다. */
    AuthToken login(String email, String rawPassword, String userAgent, String ipAddress);

    /** 리프레시 토큰을 이용해 액세스 토큰을 갱신한다. */
    AuthToken refreshToken(String refreshToken);

    /** 액세스 토큰을 블랙리스트에 추가하고 세션을 종료한다. */
    void logout(String accessToken);

    /** 특정 사용자의 모든 활성 토큰을 무효화한다 (비밀번호 변경 후 등). */
    void revokeAllTokensOf(Long userId, String reason);

    /** 단일 토큰을 무효화한다. */
    void revokeToken(String token, String reason);

    /** 토큰 유효성 검증. */
    TokenValidationResult validateToken(String token);

    /** 토큰에서 사용자 ID를 추출한다. */
    Optional<Long> extractUserId(String token);

    /** OAuth2 인가 코드 → 액세스 토큰 교환. */
    OAuthTokenResponse exchangeAuthorizationCode(String provider, String code, String redirectUri);

    /** OAuth 사용자 정보 → 내부 사용자 매핑 (없으면 회원가입). */
    AuthToken loginWithOAuth(String provider, String oauthAccessToken);

    /** 두 단계 인증 코드를 발송한다. */
    String sendTwoFactorCode(Long userId, TwoFactorChannel channel);

    /** 두 단계 인증 코드 검증. */
    boolean verifyTwoFactorCode(Long userId, String code);

    /** API 키 발급 (서드파티 통합용). */
    String issueApiKey(Long userId, String description);

    /** API 키 폐기. */
    void revokeApiKey(String apiKey);

    record AuthToken(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        LocalDateTime issuedAt,
        Long userId
    ) {}

    record OAuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String scope,
        String idToken
    ) {}

    record TokenValidationResult(
        boolean valid,
        Long userId,
        String reason,
        LocalDateTime expiresAt
    ) {
        public static TokenValidationResult invalid(String reason) {
            return new TokenValidationResult(false, null, reason, null);
        }
    }

    enum TwoFactorChannel { SMS, EMAIL, AUTHENTICATOR }
}
