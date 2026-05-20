package com.springshop.common.exception;

/**
 * 객체가 특정 연산을 처리하기에 부적합한 상태일 때 발생하는 예외.
 */
public class InvalidStateException extends BusinessException {

    public InvalidStateException(String message) {
        super(ErrorCode.SYSTEM_INVALID_STATE, message);
    }

    public InvalidStateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidStateException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
