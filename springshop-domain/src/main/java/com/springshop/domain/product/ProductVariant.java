package com.springshop.domain.product;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 상품 옵션(Variant) 엔티티.
 *
 * <p>같은 상품 안에서 색상/사이즈 등 옵션 조합을 표현한다. SKU는 옵션별 고유
 * 식별 코드이며, 재고는 옵션 단위로 관리된다(예약/확정/해제).</p>
 *
 * <p>낙관적 락(@Version)을 사용하여 동시 재고 차감 경쟁 상태를 방지한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "product_variants",
        uniqueConstraints = @UniqueConstraint(name = "uk_variant_sku", columnNames = "sku"),
        indexes = @Index(name = "idx_variant_product", columnList = "product_id")
)
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "option_name", length = 50, nullable = false)
    private String optionName;

    @Column(name = "option_value", length = 100, nullable = false)
    private String optionValue;

    @Column(name = "sku", length = 50, nullable = false)
    private String sku;

    @Column(name = "additional_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal additionalPrice = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Version
    @Column(name = "variant_version", nullable = false)
    private Long variantVersion;

    protected ProductVariant() {
        super();
    }

    public ProductVariant(Product product, String optionName, String optionValue, String sku, BigDecimal additionalPrice) {
        super();
        this.product = Objects.requireNonNull(product, "상품 필수");
        this.optionName = Objects.requireNonNull(optionName, "옵션 이름 필수");
        this.optionValue = Objects.requireNonNull(optionValue, "옵션 값 필수");
        this.sku = validateSku(sku);
        this.additionalPrice = additionalPrice == null ? BigDecimal.ZERO : additionalPrice;
    }

    private static String validateSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU는 필수입니다");
        }
        if (!sku.matches("^[A-Z0-9_-]{3,50}$")) {
            throw new IllegalArgumentException("SKU는 영대문자/숫자/-/_ 만 허용: " + sku);
        }
        return sku;
    }

    public Product getProduct() {
        return product;
    }

    public String getOptionName() {
        return optionName;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getAdditionalPrice() {
        return additionalPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public int getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    public boolean isActive() {
        return isActive;
    }

    public Long getVariantVersion() {
        return variantVersion;
    }

    /**
     * 재고 예약. 결제 진행 중 임시 점유.
     */
    public void reserve(int quantity) {
        validateQuantity(quantity);
        if (getAvailableQuantity() < quantity) {
            throw new IllegalStateException(
                    "재고 부족: 가용 %d, 요청 %d".formatted(getAvailableQuantity(), quantity));
        }
        this.reservedQuantity += quantity;
    }

    /**
     * 예약 해제(결제 실패, 취소 등).
     */
    public void release(int quantity) {
        validateQuantity(quantity);
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("예약량 초과 해제: " + quantity);
        }
        this.reservedQuantity -= quantity;
    }

    /**
     * 예약을 확정하여 실제 재고에서 차감.
     */
    public void confirm(int quantity) {
        validateQuantity(quantity);
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("확정 수량이 예약량보다 큼: " + quantity);
        }
        this.reservedQuantity -= quantity;
        this.stockQuantity -= quantity;
    }

    /**
     * 입고 처리.
     */
    public void receive(int quantity) {
        validateQuantity(quantity);
        this.stockQuantity += quantity;
    }

    /**
     * 추가 가격 변경.
     */
    public void updateAdditionalPrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("추가 가격은 0 이상이어야 합니다");
        }
        this.additionalPrice = newPrice;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    /**
     * 옵션 표시명.
     */
    public String displayName() {
        return "%s: %s (SKU: %s)".formatted(optionName, optionValue, sku);
    }

    private void validateQuantity(int q) {
        if (q <= 0) throw new IllegalArgumentException("수량은 양수: " + q);
    }
}
