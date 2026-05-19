package com.springshop.domain.order;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 주문 항목(라인 아이템) 엔티티.
 *
 * <p>{@link Order}와 N:1 매핑되며, 주문 시점의 상품 스냅샷(이름, 옵션, 단가)을 함께
 * 저장하여 사후 가격 변동에 영향받지 않도록 한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_item_order", columnList = "order_id"),
                @Index(name = "idx_order_item_product", columnList = "product_id")
        }
)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    @Column(name = "option_info", length = 200)
    private String optionInfo;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "discount_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    protected OrderItem() {
        super();
    }

    public OrderItem(Order order, Long productId, Long variantId, String productName,
                     String optionInfo, int quantity, BigDecimal unitPrice) {
        super();
        this.order = Objects.requireNonNull(order, "주문 필수");
        this.productId = Objects.requireNonNull(productId, "상품 ID 필수");
        this.variantId = variantId;
        this.productName = Objects.requireNonNull(productName, "상품명 필수");
        this.optionInfo = optionInfo;
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수: " + quantity);
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("단가는 0 이상");
        }
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = calculateTotal();
    }

    public static OrderItem of(Order order, Long productId, Long variantId, String name,
                               String option, int qty, BigDecimal price) {
        return new OrderItem(order, productId, variantId, name, option, qty, price);
    }

    public Order getOrder() {
        return order;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public String getProductName() {
        return productName;
    }

    public String getOptionInfo() {
        return optionInfo;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return totalPrice.subtract(discountAmount);
    }

    /**
     * 수량 변경. 변경 시 합계 재계산.
     */
    public void updateQuantity(int newQty) {
        if (newQty <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 합니다");
        }
        this.quantity = newQty;
        this.totalPrice = calculateTotal();
    }

    /**
     * 할인 금액 적용.
     */
    public void applyDiscount(BigDecimal discount) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("할인 금액은 0 이상");
        }
        if (discount.compareTo(totalPrice) > 0) {
            throw new IllegalArgumentException("할인이 총액을 초과");
        }
        this.discountAmount = discount;
    }

    /**
     * 단가 × 수량을 계산해 반환한다.
     */
    public BigDecimal calculateTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String summary() {
        return "%s%s x %d = %s".formatted(
                productName,
                optionInfo == null ? "" : "(" + optionInfo + ")",
                quantity,
                totalPrice.toPlainString());
    }
}
