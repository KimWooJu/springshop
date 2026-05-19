package com.springshop.common.exception;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 입력값 검증 실패 시 발생하는 예외.
 * 다중 필드 오류를 포함할 수 있다.
 */
public class ValidationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<FieldError> fieldErrors;

    public ValidationException() {
        super(ErrorCode.SYSTEM_VALIDATION_FAILED);
        this.fieldErrors = new ArrayList<>();
    }

    public ValidationException(String message) {
        super(ErrorCode.SYSTEM_VALIDATION_FAILED, message);
        this.fieldErrors = new ArrayList<>();
    }

    public ValidationException(List<FieldError> fieldErrors) {
        super(ErrorCode.SYSTEM_VALIDATION_FAILED,
                "검증 실패: %d개 필드 오류".formatted(fieldErrors == null ? 0 : fieldErrors.size()));
        this.fieldErrors = fieldErrors == null ? new ArrayList<>() : new ArrayList<>(fieldErrors);
    }

    public ValidationException(ErrorCode errorCode, List<FieldError> fieldErrors) {
        super(errorCode, "%d개 필드 오류".formatted(fieldErrors == null ? 0 : fieldErrors.size()));
        this.fieldErrors = fieldErrors == null ? new ArrayList<>() : new ArrayList<>(fieldErrors);
    }

    public ValidationException addFieldError(String field, String message) {
        this.fieldErrors.add(new FieldError(field, null, message));
        return this;
    }

    public ValidationException addFieldError(String field, Object rejectedValue, String message) {
        this.fieldErrors.add(new FieldError(field, rejectedValue, message));
        return this;
    }

    public List<FieldError> getFieldErrors() {
        return Collections.unmodifiableList(fieldErrors);
    }

    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }

    public int getErrorCount() {
        return fieldErrors.size();
    }

    public static ValidationException builder() {
        return new ValidationException();
    }

    public static ValidationException of(String field, String message) {
        return new ValidationException().addFieldError(field, message);
    }

    /**
     * 개별 필드 검증 오류를 표현하는 record.
     */
    public record FieldError(String field, Object rejectedValue, String message) {
        public FieldError {
            Objects.requireNonNull(field, "field is required");
            Objects.requireNonNull(message, "message is required");
        }

        public String describe() {
            return "%s: %s (rejected=%s)".formatted(field, message, rejectedValue);
        }
    }
}
