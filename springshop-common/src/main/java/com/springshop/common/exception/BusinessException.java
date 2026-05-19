package com.springshop.common.exception;

/**
 * 비즈니스 로직 위반으로 발생하는 예외.
 * HTTP 400/409 등 클라이언트 오류 응답을 유도한다.
 */
public class BusinessException extends SpringShopException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public BusinessException(ErrorCode errorCode, Object[] args, Throwable cause) {
        super(errorCode, args, cause);
    }

    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    public static BusinessException withArgs(ErrorCode errorCode, Object... args) {
        return new BusinessException(errorCode, args);
    }

    /**
     * 비즈니스 규칙 위반 여부를 명시적으로 확인.
     */
    public boolean isBusinessRuleViolation() {
        return getErrorCode().getHttpStatus().value() == 400
                || getErrorCode().getHttpStatus().value() == 409;
    }

    /**
     * 클라이언트가 재시도 가능한 예외인지 확인.
     */
    public boolean isRetryable() {
        return switch (getErrorCode()) {
            case INVENTORY_LOCK_TIMEOUT, PAYMENT_TIMEOUT -> true;
            default -> false;
        };
    }

    /**
     * 사용자에게 노출 가능한 메시지인지 확인.
     */
    public boolean isUserVisible() {
        return getErrorCode().getHttpStatus().is4xxClientError();
    }

    /**
     * 디버깅용 상세 컨텍스트 문자열을 반환한다.
     */
    public String describeContext() {
        return "BusinessException[code=%s, status=%d, retryable=%s, userVisible=%s]"
                .formatted(
                        getErrorCode().getCode(),
                        getHttpStatus().value(),
                        isRetryable(),
                        isUserVisible()
                );
    }

    /**
     * 예외 카테고리를 분류한다.
     */
    public String categorize() {
        String code = getErrorCode().getCode();
        if (code.startsWith("USER_")) return "USER";
        if (code.startsWith("PRODUCT_")) return "PRODUCT";
        if (code.startsWith("ORDER_")) return "ORDER";
        if (code.startsWith("PAYMENT_")) return "PAYMENT";
        if (code.startsWith("INVENTORY_")) return "INVENTORY";
        if (code.startsWith("REVIEW_")) return "REVIEW";
        if (code.startsWith("COUPON_")) return "COUPON";
        return "OTHER";
    }
}
