package com.springshop.common.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * SpringShop 시스템의 모든 비즈니스/시스템 예외의 최상위 추상 클래스.
 * ErrorCode 기반으로 HTTP 상태와 메시지를 일관되게 관리한다.
 */
public abstract class SpringShopException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;
    private final HttpStatus httpStatus;
    private final LocalDateTime timestamp;

    protected SpringShopException(ErrorCode errorCode) {
        this(errorCode, new Object[0], null);
    }

    protected SpringShopException(ErrorCode errorCode, Object... args) {
        this(errorCode, args, null);
    }

    protected SpringShopException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, new Object[0], cause);
    }

    protected SpringShopException(ErrorCode errorCode, Object[] args, Throwable cause) {
        super(errorCode.format(args), cause);
        this.errorCode = errorCode;
        this.args = args != null ? args.clone() : new Object[0];
        this.httpStatus = errorCode.getHttpStatus();
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 단순 ErrorCode로부터 예외 생성 헬퍼 (서브 클래스에서 사용).
     */
    public static <T extends SpringShopException> T of(ErrorCode errorCode,
                                                       ExceptionFactory<T> factory) {
        return factory.create(errorCode, new Object[0]);
    }

    /**
     * 인자를 받는 ErrorCode로 예외 생성 헬퍼.
     */
    public static <T extends SpringShopException> T withArgs(ErrorCode errorCode,
                                                             ExceptionFactory<T> factory,
                                                             Object... args) {
        return factory.create(errorCode, args);
    }

    @FunctionalInterface
    public interface ExceptionFactory<T extends SpringShopException> {
        T create(ErrorCode errorCode, Object[] args);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args.clone();
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    public boolean isClientError() {
        return httpStatus.is4xxClientError();
    }

    public boolean isServerError() {
        return httpStatus.is5xxServerError();
    }

    @Override
    public String toString() {
        return "%s{code=%s, status=%d, message=%s, args=%s, timestamp=%s}".formatted(
                getClass().getSimpleName(),
                errorCode.getCode(),
                httpStatus.value(),
                getMessage(),
                Arrays.toString(args),
                timestamp
        );
    }
}
