package com.springshop.common.exception;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 결제 처리 중 발생하는 예외.
 * PG사 오류 코드와 결제 금액 등 추가 정보를 포함한다.
 */
public class PaymentException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String pgErrorCode;
    private final String pgErrorMessage;
    private final BigDecimal amount;
    private final String paymentMethod;

    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
        this.pgErrorCode = null;
        this.pgErrorMessage = null;
        this.amount = null;
        this.paymentMethod = null;
    }

    public PaymentException(ErrorCode errorCode, String pgErrorCode, String pgErrorMessage) {
        super(errorCode, pgErrorMessage);
        this.pgErrorCode = pgErrorCode;
        this.pgErrorMessage = pgErrorMessage;
        this.amount = null;
        this.paymentMethod = null;
    }

    public PaymentException(ErrorCode errorCode, BigDecimal amount, String paymentMethod) {
        super(errorCode, amount, paymentMethod);
        this.pgErrorCode = null;
        this.pgErrorMessage = null;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public PaymentException(ErrorCode errorCode, String pgErrorCode, String pgErrorMessage,
                            BigDecimal amount, String paymentMethod, Throwable cause) {
        super(errorCode, new Object[]{pgErrorMessage}, cause);
        this.pgErrorCode = pgErrorCode;
        this.pgErrorMessage = pgErrorMessage;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public static PaymentException pgError(String pgErrorCode, String pgErrorMessage) {
        return new PaymentException(ErrorCode.PAYMENT_PG_ERROR, pgErrorCode, pgErrorMessage);
    }

    public static PaymentException timeout() {
        return new PaymentException(ErrorCode.PAYMENT_TIMEOUT);
    }

    public static PaymentException amountMismatch(BigDecimal expected, BigDecimal actual) {
        var ex = new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        return ex;
    }

    public static PaymentException cardDeclined(String pgCode) {
        return new PaymentException(ErrorCode.PAYMENT_CARD_DECLINED, pgCode, "카드사 거절");
    }

    public static PaymentException insufficientBalance(BigDecimal amount) {
        return new PaymentException(ErrorCode.PAYMENT_INSUFFICIENT_BALANCE, amount, null);
    }

    public static PaymentException alreadyRefunded() {
        return new PaymentException(ErrorCode.PAYMENT_ALREADY_REFUNDED);
    }

    public String getPgErrorCode() {
        return pgErrorCode;
    }

    public String getPgErrorMessage() {
        return pgErrorMessage;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public boolean isRetryable() {
        return switch (getErrorCode()) {
            case PAYMENT_TIMEOUT, PAYMENT_PG_ERROR -> true;
            default -> false;
        };
    }

    public boolean requiresManualReview() {
        return switch (getErrorCode()) {
            case PAYMENT_REFUND_FAILED, PAYMENT_AMOUNT_MISMATCH -> true;
            default -> false;
        };
    }
}
