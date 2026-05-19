package com.springshop.common.exception;

import java.io.Serial;

/**
 * 인증 실패 (401 Unauthorized) 시 발생하는 예외.
 */
public class AuthenticationException extends SpringShopException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AuthenticationException() {
        super(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthenticationException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public AuthenticationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public AuthenticationException(ErrorCode errorCode, Object[] args, Throwable cause) {
        super(errorCode, args, cause);
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException(ErrorCode.AUTH_TOKEN_EXPIRED);
    }

    public static AuthenticationException tokenInvalid() {
        return new AuthenticationException(ErrorCode.AUTH_TOKEN_INVALID);
    }

    public static AuthenticationException tokenRequired() {
        return new AuthenticationException(ErrorCode.AUTH_TOKEN_REQUIRED);
    }

    public static AuthenticationException tokenBlacklisted() {
        return new AuthenticationException(ErrorCode.AUTH_TOKEN_BLACKLISTED);
    }

    public static AuthenticationException refreshTokenInvalid() {
        return new AuthenticationException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }

    public static AuthenticationException sessionExpired() {
        return new AuthenticationException(ErrorCode.AUTH_SESSION_EXPIRED);
    }

    public static AuthenticationException loginAttemptsExceeded(int minutesUntilUnlock) {
        return new AuthenticationException(ErrorCode.AUTH_LOGIN_ATTEMPTS_EXCEEDED, minutesUntilUnlock);
    }

    public boolean isTokenRelated() {
        return switch (getErrorCode()) {
            case AUTH_TOKEN_EXPIRED, AUTH_TOKEN_INVALID, AUTH_TOKEN_REQUIRED,
                 AUTH_TOKEN_BLACKLISTED, AUTH_REFRESH_TOKEN_INVALID -> true;
            default -> false;
        };
    }

    public boolean shouldClearTokenCookie() {
        return isTokenRelated();
    }

    public boolean shouldNotifyAdmin() {
        return getErrorCode() == ErrorCode.AUTH_LOGIN_ATTEMPTS_EXCEEDED;
    }
}
