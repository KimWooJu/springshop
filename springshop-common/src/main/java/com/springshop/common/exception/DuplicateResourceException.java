package com.springshop.common.exception;

import java.io.Serial;
import java.util.Objects;

/**
 * 중복 리소스가 발견되었을 때 발생하는 예외.
 * 필드명과 중복 값을 함께 보관한다.
 */
public class DuplicateResourceException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String resourceName;
    private final String field;
    private final Object value;

    public DuplicateResourceException(String message) {
        super(ErrorCode.SYSTEM_VALIDATION_FAILED, message);
        this.resourceName = message;
        this.field = "";
        this.value = null;
    }

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(resolveErrorCode(resourceName, field), "%s(%s=%s)".formatted(resourceName, field, value));
        this.resourceName = Objects.requireNonNull(resourceName);
        this.field = Objects.requireNonNull(field);
        this.value = value;
    }

    public DuplicateResourceException(ErrorCode errorCode, String resourceName,
                                      String field, Object value) {
        super(errorCode, "%s(%s=%s)".formatted(resourceName, field, value));
        this.resourceName = Objects.requireNonNull(resourceName);
        this.field = Objects.requireNonNull(field);
        this.value = value;
    }

    private static ErrorCode resolveErrorCode(String resourceName, String field) {
        if (resourceName == null) return ErrorCode.SYSTEM_VALIDATION_FAILED;
        String key = (resourceName + "_" + field).toLowerCase();
        return switch (key) {
            case "user_email" -> ErrorCode.USER_EMAIL_DUPLICATED;
            case "user_phone" -> ErrorCode.USER_PHONE_DUPLICATED;
            case "product_name" -> ErrorCode.PRODUCT_NAME_DUPLICATED;
            default -> switch (resourceName.toLowerCase()) {
                case "user" -> ErrorCode.USER_ALREADY_EXISTS;
                default -> ErrorCode.SYSTEM_VALIDATION_FAILED;
            };
        };
    }

    public static DuplicateResourceException ofUser(String field, Object value) {
        return new DuplicateResourceException("User", field, value);
    }

    public static DuplicateResourceException ofProduct(String field, Object value) {
        return new DuplicateResourceException("Product", field, value);
    }

    public static DuplicateResourceException email(String emailValue) {
        return new DuplicateResourceException("User", "email", emailValue);
    }

    public static DuplicateResourceException phone(String phoneValue) {
        return new DuplicateResourceException("User", "phone", phoneValue);
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }

    public String describe() {
        return "DuplicateResource[resource=%s, field=%s, value=%s]"
                .formatted(resourceName, field, value);
    }
}
