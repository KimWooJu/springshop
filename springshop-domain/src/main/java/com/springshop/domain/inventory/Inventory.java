package com.springshop.domain.inventory;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 재고 애그리거트 루트.
 *
 * <p>상품(productId) + 옵션(variantId) + 창고(warehouseId)의 조합으로 유일성을 보장한다.
 * 재고는 다음과 같은 세 가지 수량 카테고리를 가진다.</p>
 * <ul>
 *   <li>totalQuantity: 물리적으로 창고에 존재하는 전체 수량</li>
 *   <li>availableQuantity: 즉시 주문 가능한 수량 (전체 - 예약 - 보류)</li>
 *   <li>reservedQuantity: 결제 진행 중인 주문에 의해 예약된 수량</li>
 * </ul>
 *
 * <p>동시성 제어를 위해 {@link Version} 칼럼으로 낙관적 락을 적용한다.
 * 일반적으로 reserve → confirm/release 의 흐름을 따른다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "inventories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_pvw",
                columnNames = {"product_id", "variant_id", "warehouse_id"}),
        indexes = {
                @Index(name = "idx_inventory_product", columnList = "product_id"),
                @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_inventory_available", columnList = "available_quantity")
        }
)
public class Inventory extends BaseAuditEntity {

    /**
     * 재고 부족 경고 기본 임계치.
     */
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    /**
     * 임박 재고 기본 임계치.
     */
    public static final int DEFAULT_CRITICAL_STOCK_THRESHOLD = 3;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "last_received_at")
    private LocalDateTime lastReceivedAt;

    @Column(name = "last_shipped_at")
    private LocalDateTime lastShippedAt;

    @Column(name = "last_adjusted_at")
    private LocalDateTime lastAdjustedAt;

    protected Inventory() {
        super();
    }

    private Inventory(Long productId, Long variantId, Long warehouseId, int initialQuantity, String location) {
        super();
        this.productId = Objects.requireNonNull(productId, "productId 필수");
        this.variantId = variantId;
        this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId 필수");
        if (initialQuantity < 0) {
            throw new IllegalArgumentException("초기 수량은 0 이상이어야 합니다: " + initialQuantity);
        }
        this.totalQuantity = initialQuantity;
        this.availableQuantity = initialQuantity;
        this.reservedQuantity = 0;
        this.location = location;
    }

    /**
     * 새 재고 항목 생성.
     */
    public static Inventory create(Long productId, Long variantId, Long warehouseId, int initialQuantity, String location) {
        return new Inventory(productId, variantId, warehouseId, initialQuantity, location);
    }

    public Long getProductId() {
        return productId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getLastReceivedAt() {
        return lastReceivedAt;
    }

    public LocalDateTime getLastShippedAt() {
        return lastShippedAt;
    }

    public LocalDateTime getLastAdjustedAt() {
        return lastAdjustedAt;
    }

    public void updateLocation(String location) {
        this.location = location;
    }

    /**
     * 주문 예약. availableQuantity 감소, reservedQuantity 증가.
     */
    public void reserve(int quantity) {
        ensurePositive(quantity, "예약 수량");
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(
                    "예약 가능 수량 부족: 요청=%d, 가용=%d (productId=%s)"
                            .formatted(quantity, availableQuantity, productId));
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    /**
     * 예약 해제. 결제 실패/주문 취소 시.
     */
    public void release(int quantity) {
        ensurePositive(quantity, "해제 수량");
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "해제 수량이 예약 수량을 초과: 요청=%d, 예약=%d".formatted(quantity, reservedQuantity));
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    /**
     * 예약 확정. reservedQuantity 감소, totalQuantity 감소(출고 완료).
     */
    public void confirm(int quantity) {
        ensurePositive(quantity, "확정 수량");
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                    "확정 수량이 예약 수량을 초과: 요청=%d, 예약=%d".formatted(quantity, reservedQuantity));
        }
        this.reservedQuantity -= quantity;
        this.totalQuantity -= quantity;
        this.lastShippedAt = LocalDateTime.now();
    }

    /**
     * 입고 처리. totalQuantity, availableQuantity 동시 증가.
     */
    public void receive(int quantity, String reason) {
        ensurePositive(quantity, "입고 수량");
        this.totalQuantity += quantity;
        this.availableQuantity += quantity;
        this.lastReceivedAt = LocalDateTime.now();
    }

    /**
     * 재고 조정. 절대값으로 재설정한다. 차이값에 대한 변동 로그가 별도로 필요하다.
     *
     * @param newTotal 조정 후 totalQuantity
     * @param reason 조정 사유 (감사 로그용)
     * @return 변동량 (양수=증가, 음수=감소)
     */
    public int adjust(int newTotal, String reason) {
        if (newTotal < 0) {
            throw new IllegalArgumentException("조정 후 수량은 0 이상이어야 합니다: " + newTotal);
        }
        if (newTotal < reservedQuantity) {
            throw new IllegalStateException(
                    "조정 후 수량이 예약 수량보다 작을 수 없음: total=%d, reserved=%d"
                            .formatted(newTotal, reservedQuantity));
        }
        int delta = newTotal - this.totalQuantity;
        this.totalQuantity = newTotal;
        this.availableQuantity = newTotal - reservedQuantity;
        this.lastAdjustedAt = LocalDateTime.now();
        return delta;
    }

    /**
     * 요청 수량이 즉시 가용한지 검사.
     */
    public boolean isAvailable(int quantity) {
        return availableQuantity >= quantity;
    }

    /**
     * 재고 부족 경고 임계치 이하인지.
     */
    public boolean isLowStock(int threshold) {
        return availableQuantity <= threshold;
    }

    /**
     * 기본 임계치 적용.
     */
    public boolean isLowStock() {
        return isLowStock(DEFAULT_LOW_STOCK_THRESHOLD);
    }

    /**
     * 임박 재고.
     */
    public boolean isCriticalStock() {
        return availableQuantity <= DEFAULT_CRITICAL_STOCK_THRESHOLD;
    }

    /**
     * 품절 여부.
     */
    public boolean isOutOfStock() {
        return availableQuantity <= 0;
    }

    /**
     * 사용자에게 보여줄 가용성 상태 표현.
     */
    public String getAvailabilityStatus() {
        if (isOutOfStock()) {
            return "품절";
        }
        if (isCriticalStock()) {
            return "마감 임박";
        }
        if (isLowStock()) {
            return "재고 부족";
        }
        return "충분";
    }

    /**
     * 회전율 계산용: 총 수량 대비 가용 비율.
     */
    public double getAvailabilityRatio() {
        if (totalQuantity == 0) return 0.0;
        return (double) availableQuantity / totalQuantity;
    }

    private void ensurePositive(int quantity, String name) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("%s은(는) 양수여야 합니다: %d".formatted(name, quantity));
        }
    }

    /**
     * 재고 부족 예외.
     */
    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }

    @Override
    public String toString() {
        return "Inventory[id=%s, product=%s, variant=%s, warehouse=%s, total=%d, avail=%d, reserved=%d]"
                .formatted(getId(), productId, variantId, warehouseId, totalQuantity, availableQuantity, reservedQuantity);
    }
}
