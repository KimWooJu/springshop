package com.springshop.common.constant;

/**
 * 보안 관련 상수.
 *
 * <p>JWT, OAuth2, CORS, Rate Limiting, Redis 키 접두사, 공개 URL 패턴 등
 * 보안 컴포넌트에서 사용되는 상수 모음.</p>
 */
public final class SecurityConstants {

    private SecurityConstants() {
        throw new AssertionError("SecurityConstants는 인스턴스화할 수 없습니다");
    }

    // ===== JWT =====
    /** JWT 헤더 이름. */
    public static final String JWT_HEADER = "Authorization";
    /** JWT 헤더 접두사. */
    public static final String JWT_PREFIX = "Bearer ";
    /** Refresh 토큰 헤더 이름. */
    public static final String JWT_REFRESH_HEADER = "X-Refresh-Token";
    /** Access 토큰 만료 시간 (분). */
    public static final long ACCESS_TOKEN_EXPIRY_MINUTES = 30L;
    /** Refresh 토큰 만료 시간 (일). */
    public static final long REFRESH_TOKEN_EXPIRY_DAYS = 30L;
    /** Access 토큰 만료 시간 (밀리초). */
    public static final long ACCESS_TOKEN_EXPIRY_MS = ACCESS_TOKEN_EXPIRY_MINUTES * 60_000L;
    /** Refresh 토큰 만료 시간 (밀리초). */
    public static final long REFRESH_TOKEN_EXPIRY_MS = REFRESH_TOKEN_EXPIRY_DAYS * 24L * 60L * 60_000L;

    /** JWT 클레임: 사용자 ID. */
    public static final String JWT_CLAIM_USER_ID = "userId";
    /** JWT 클레임: 이메일. */
    public static final String JWT_CLAIM_EMAIL = "email";
    /** JWT 클레임: 역할. */
    public static final String JWT_CLAIM_ROLE = "role";
    /** JWT 클레임: 토큰 타입. */
    public static final String JWT_CLAIM_TYPE = "type";
    /** JWT 클레임: 발급자. */
    public static final String JWT_CLAIM_ISSUER = "iss";
    /** JWT 클레임: 권한 목록. */
    public static final String JWT_CLAIM_AUTHORITIES = "authorities";

    /** Access 토큰 타입. */
    public static final String JWT_TYPE_ACCESS = "ACCESS";
    /** Refresh 토큰 타입. */
    public static final String JWT_TYPE_REFRESH = "REFRESH";

    /** JWT 발급자 식별자. */
    public static final String JWT_ISSUER = "springshop";

    // ===== 비밀번호 =====
    /** BCrypt 강도. */
    public static final int BCRYPT_STRENGTH = 12;
    /** 최대 로그인 시도 횟수. */
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    /** 계정 잠금 시간 (분). */
    public static final int LOCK_DURATION_MINUTES = 30;
    /** 비밀번호 재설정 토큰 만료 시간 (분). */
    public static final int PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 30;
    /** 이메일 인증 토큰 만료 시간 (시간). */
    public static final int EMAIL_VERIFY_TOKEN_EXPIRY_HOURS = 24;
    /** 비밀번호 변경 주기 (일). */
    public static final int PASSWORD_CHANGE_INTERVAL_DAYS = 90;

    // ===== Rate Limiting =====
    /** 일반 요청 분당 제한. */
    public static final int RATE_LIMIT_REQUESTS_PER_MINUTE = 60;
    /** Burst 허용 요청 수. */
    public static final int RATE_LIMIT_BURST = 20;
    /** 로그인 분당 제한. */
    public static final int LOGIN_RATE_LIMIT_PER_MINUTE = 10;
    /** 이메일 발송 분당 제한. */
    public static final int EMAIL_RATE_LIMIT_PER_MINUTE = 3;
    /** Rate limit 윈도우 (초). */
    public static final long RATE_LIMIT_WINDOW_SECONDS = 60L;

    // ===== Redis 키 접두사 =====
    /** JWT 블랙리스트 키 접두사. */
    public static final String REDIS_JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    /** Refresh 토큰 저장 키 접두사. */
    public static final String REDIS_REFRESH_TOKEN_PREFIX = "jwt:refresh:";
    /** 비밀번호 재설정 키 접두사. */
    public static final String REDIS_PASSWORD_RESET_PREFIX = "auth:pwd-reset:";
    /** 이메일 인증 키 접두사. */
    public static final String REDIS_EMAIL_VERIFY_PREFIX = "auth:email-verify:";
    /** 로그인 시도 횟수 키 접두사. */
    public static final String REDIS_LOGIN_ATTEMPT_PREFIX = "auth:login-attempt:";
    /** Rate limit 키 접두사. */
    public static final String REDIS_RATE_LIMIT_PREFIX = "rate-limit:";
    /** 세션 키 접두사. */
    public static final String REDIS_SESSION_PREFIX = "session:";
    /** OTP 키 접두사. */
    public static final String REDIS_OTP_PREFIX = "auth:otp:";

    // ===== CORS =====
    /** 허용 Origin 목록. */
    public static final String[] ALLOWED_ORIGINS = {
        "http://localhost:3000",
        "http://localhost:8080",
        "https://springshop.com",
        "https://www.springshop.com",
        "https://admin.springshop.com"
    };
    /** 허용 HTTP 메서드. */
    public static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};
    /** 허용 헤더. */
    public static final String[] ALLOWED_HEADERS = {"*"};
    /** Preflight 캐시 시간 (초). */
    public static final long MAX_AGE_SECONDS = 3600L;

    // ===== 공개 URL 패턴 (인증 불필요) =====
    public static final String[] PUBLIC_URLS = {
        "/api/v1/auth/**",
        "/api/v1/products/**",
        "/api/v1/categories/**",
        "/api/v1/brands/**",
        "/api/v1/reviews/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/actuator/health",
        "/actuator/info",
        "/error",
        "/h2-console/**",
        "/favicon.ico"
    };

    // ===== 역할 =====
    /** 일반 사용자 역할. */
    public static final String ROLE_USER = "USER";
    /** 판매자 역할. */
    public static final String ROLE_SELLER = "SELLER";
    /** 관리자 역할. */
    public static final String ROLE_ADMIN = "ADMIN";
    /** 슈퍼 관리자 역할. */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    /** Spring Security 역할 접두사. */
    public static final String ROLE_PREFIX = "ROLE_";
}
