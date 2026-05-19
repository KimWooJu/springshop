package com.springshop.common.exception;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 주문 처리 관련 예외.
 */
public class OrderException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String orderNumber;
    private final String fromStatus;
    private final String toStatus;

    public OrderException(ErrorCode errorCode) {
        super(errorCode);
        this.orderNumber = null;
        this.fromStatus = null;
        this.toStatus = null;
    }

    public OrderException(ErrorCode errorCode, String orderNumber) {
        super(errorCode, orderNumber);
        this.orderNumber = orderNumber;
        this.fromStatus = null;
        this.toStatus = null;
    }

    public OrderException(ErrorCode errorCode, String fromStatus, String toStatus) {
        super(errorCode, fromStatus, toStatus);
        this.orderNumber = null;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public OrderException(ErrorCode errorCode, String orderNumber,
                          String fromStatus, String toStatus) {
        super(errorCode, fromStatus, toStatus);
        this.orderNumber = orderNumber;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public static OrderException alreadyPaid(String orderNumber) {
        return new OrderException(ErrorCode.ORDER_ALREADY_PAID, orderNumber);
    }

    public static OrderException alreadyCancelled(String orderNumber) {
        return new OrderException(ErrorCode.ORDER_ALREADY_CANCELLED, orderNumber);
    }

    public static OrderException cannotCancel(String currentStatus) {
        return new OrderException(ErrorCode.ORDER_CANNOT_CANCEL, currentStatus);
    }

    public static OrderException empty() {
        return new OrderException(ErrorCode.ORDER_EMPTY);
    }

    public static OrderException amountInvalid() {
        return new OrderException(ErrorCode.ORDER_AMOUNT_INVALID);
    }

    public static OrderException minAmountRequired(BigDecimal minAmount) {
        var ex = new OrderException(ErrorCode.ORDER_MIN_AMOUNT_REQUIRED, minAmount.toPlainString());
        return ex;
    }

    public static OrderException deliveryAddressRequired() {
        return new OrderException(ErrorCode.ORDER_DELIVERY_ADDRESS_REQUIRED);
    }

    public static OrderException invalidStatusTransition(String from, String to) {
        return new OrderException(ErrorCode.ORDER_STATUS_TRANSITION_INVALID, from, to);
    }

    public static OrderException accessDenied() {
        return new OrderException(ErrorCode.ORDER_ACCESS_DENIED);
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public boolean isStatusTransitionError() {
        return getErrorCode() == ErrorCode.ORDER_STATUS_TRANSITION_INVALID;
    }
}
