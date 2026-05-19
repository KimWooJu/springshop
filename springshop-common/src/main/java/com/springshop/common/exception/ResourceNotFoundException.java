package com.springshop.common.exception;

import java.io.Serial;
import java.util.Objects;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외.
 * 리소스 명과 식별자를 받아 메시지를 자동 생성한다.
 */
public class ResourceNotFoundException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String resourceName;
    private final Object identifier;

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resolveErrorCode(resourceName), "%s(id=%s)".formatted(resourceName, identifier));
        this.resourceName = Objects.requireNonNull(resourceName);
        this.identifier = identifier;
    }

    public ResourceNotFoundException(ErrorCode errorCode, String resourceName, Object identifier) {
        super(errorCode, "%s(id=%s)".formatted(resourceName, identifier));
        this.resourceName = Objects.requireNonNull(resourceName);
        this.identifier = identifier;
    }

    /**
     * 리소스 명을 기반으로 적합한 ErrorCode를 자동 결정한다.
     */
    private static ErrorCode resolveErrorCode(String resourceName) {
        if (resourceName == null) return ErrorCode.SYSTEM_NOT_FOUND;
        return switch (resourceName.toLowerCase()) {
            case "user" -> ErrorCode.USER_NOT_FOUND;
            case "product" -> ErrorCode.PRODUCT_NOT_FOUND;
            case "category" -> ErrorCode.PRODUCT_CATEGORY_NOT_FOUND;
            case "brand" -> ErrorCode.PRODUCT_BRAND_NOT_FOUND;
            case "order" -> ErrorCode.ORDER_NOT_FOUND;
            case "payment" -> ErrorCode.PAYMENT_NOT_FOUND;
            case "inventory" -> ErrorCode.INVENTORY_NOT_FOUND;
            case "review" -> ErrorCode.REVIEW_NOT_FOUND;
            case "coupon" -> ErrorCode.COUPON_NOT_FOUND;
            default -> ErrorCode.SYSTEM_NOT_FOUND;
        };
    }

    public static ResourceNotFoundException user(Object id) {
        return new ResourceNotFoundException("User", id);
    }

    public static ResourceNotFoundException product(Object id) {
        return new ResourceNotFoundException("Product", id);
    }

    public static ResourceNotFoundException order(Object id) {
        return new ResourceNotFoundException("Order", id);
    }

    public static ResourceNotFoundException payment(Object id) {
        return new ResourceNotFoundException("Payment", id);
    }

    public static ResourceNotFoundException category(Object id) {
        return new ResourceNotFoundException("Category", id);
    }

    public static ResourceNotFoundException brand(Object id) {
        return new ResourceNotFoundException("Brand", id);
    }

    public static ResourceNotFoundException review(Object id) {
        return new ResourceNotFoundException("Review", id);
    }

    public static ResourceNotFoundException coupon(Object id) {
        return new ResourceNotFoundException("Coupon", id);
    }

    public String getResourceName() {
        return resourceName;
    }

    public Object getIdentifier() {
        return identifier;
    }
}
