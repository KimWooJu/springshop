package com.springshop.common.exception;

import java.io.Serial;

/**
 * 재고 관련 예외 (재고 부족, 락 타임아웃 등).
 */
public class InventoryException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long productId;
    private final Integer requestedQuantity;
    private final Integer availableQuantity;
    private final String warehouseCode;

    public InventoryException(ErrorCode errorCode) {
        super(errorCode);
        this.productId = null;
        this.requestedQuantity = null;
        this.availableQuantity = null;
        this.warehouseCode = null;
    }

    public InventoryException(ErrorCode errorCode, Long productId,
                              Integer requestedQuantity, Integer availableQuantity) {
        super(errorCode, requestedQuantity, availableQuantity);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
        this.warehouseCode = null;
    }

    public InventoryException(ErrorCode errorCode, Long productId,
                              Integer requestedQuantity, Integer availableQuantity,
                              String warehouseCode) {
        super(errorCode, requestedQuantity, availableQuantity);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
        this.warehouseCode = warehouseCode;
    }

    public static InventoryException insufficient(Long productId,
                                                  int requested,
                                                  int available) {
        return new InventoryException(
                ErrorCode.INVENTORY_INSUFFICIENT,
                productId,
                requested,
                available
        );
    }

    public static InventoryException lockTimeout(Long productId) {
        return new InventoryException(
                ErrorCode.INVENTORY_LOCK_TIMEOUT,
                productId,
                null,
                null
        );
    }

    public static InventoryException notFound(Long productId) {
        return new InventoryException(
                ErrorCode.INVENTORY_NOT_FOUND,
                productId,
                null,
                null
        );
    }

    public static InventoryException negativeQuantity(int requested) {
        return new InventoryException(
                ErrorCode.INVENTORY_NEGATIVE_QUANTITY,
                null,
                requested,
                null
        );
    }

    public static InventoryException reservationExpired(Long productId) {
        return new InventoryException(
                ErrorCode.INVENTORY_RESERVATION_EXPIRED,
                productId,
                null,
                null
        );
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public int getShortageQuantity() {
        if (requestedQuantity == null || availableQuantity == null) return 0;
        return Math.max(0, requestedQuantity - availableQuantity);
    }

    @Override
    public boolean isRetryable() {
        return getErrorCode() == ErrorCode.INVENTORY_LOCK_TIMEOUT;
    }
}
