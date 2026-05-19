package com.springshop.domain.cart;

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
 * 장바구니 항목 엔티티.
 *
 * <p>{@link Cart}의 자식으로, 상품 1개(또는 옵션 조합)에 대한 사용자의 수량/가격 스냅샷이다.
 * 상품 가격이나 이름은 사용자가 담은 시점의 값을 보관하여 가격 변동에 대한 UX 일관성을
 * 유지한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "cart_items",
        indexes = {
                @Index(name = "idx_cart_item_cart", columnList = "cart_id"),
                @Index(name = "idx_cart_item_product", columnList = "product_id")
        }
)
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_cart_item_cart"))
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "unit_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "selected", nullable = false)
    private boolean selected = true;

    @Column(name = "option_description", length = 300)
    private String optionDescription;

    /**
     * JPA 기본 생성자.
     */
    protected CartItem() {
        super();
    }

    private CartItem(Cart cart,
                     Long productId,
                     Long variantId,
                     String productName,
                     String thumbnailUrl,
                     int quantity,
                     BigDecimal unitPrice) {
        super();
        this.cart = Objects.requireNonNull(cart, "장바구니는 null일 수 없습니다");
        this.productId = Objects.requireNonNull(productId, "상품 ID는 null일 수 없습니다");
        this.variantId = variantId;
        this.productName = Objects.requireNonNullElse(productName, "상품");
        this.thumbnailUrl = thumbnailUrl;
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 합니다: " + quantity);
        }
        Objects.requireNonNull(unitPrice, "단가는 null일 수 없습니다");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("단가는 0 이상이어야 합니다: " + unitPrice);
        }
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.selected = true;
    }

    /**
     * 정적 팩토리.
     */
    public static CartItem create(Cart cart,
                                  Long productId,
                                  Long variantId,
                                  String productName,
                                  String thumbnailUrl,
                                  int quantity,
                                  BigDecimal unitPrice) {
        return new CartItem(cart, productId, variantId, productName, thumbnailUrl, quantity, unitPrice);
    }

    public Cart getCart() {
        return cart;
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isSelected() {
        return selected;
    }

    public String getOptionDescription() {
        return optionDescription;
    }

    /**
     * 수량 +1.
     */
    public void increaseQuantity() {
        increaseQuantity(1);
    }

    /**
     * 수량 + amount.
     */
    public void increaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("증가량은 양수여야 합니다: " + amount);
        }
        int next = this.quantity + amount;
        if (next > Cart.MAX_QUANTITY_PER_ITEM) {
            throw new IllegalStateException(
                    "항목당 최대 수량 초과: %d (한도 %d)".formatted(next, Cart.MAX_QUANTITY_PER_ITEM));
        }
        this.quantity = next;
    }

    /**
     * 수량 -1. 0 이하가 되면 예외.
     */
    public void decreaseQuantity() {
        decreaseQuantity(1);
    }

    public void decreaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("감소량은 양수여야 합니다: " + amount);
        }
        int next = this.quantity - amount;
        if (next <= 0) {
            throw new IllegalStateException("수량 감소 후 0 이하가 됩니다: " + next);
        }
        this.quantity = next;
    }

    /**
     * 수량 직접 설정. 유효성 검증을 거친다.
     */
    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 합니다: " + quantity);
        }
        if (quantity > Cart.MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException(
                    "수량은 %d 이하여야 합니다: %d".formatted(Cart.MAX_QUANTITY_PER_ITEM, quantity));
        }
        this.quantity = quantity;
    }

    public void select() {
        this.selected = true;
    }

    public void deselect() {
        this.selected = false;
    }

    public void updateOptionDescription(String description) {
        this.optionDescription = description;
    }

    /**
     * 단가 변경. 보통은 사용자가 새로고침 후 적용한다.
     */
    public void refreshUnitPrice(BigDecimal newPrice) {
        Objects.requireNonNull(newPrice, "단가는 null일 수 없습니다");
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("단가는 0 이상이어야 합니다: " + newPrice);
        }
        this.unitPrice = newPrice;
    }

    /**
     * 항목 소계 = 단가 * 수량.
     */
    public BigDecimal calculateSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 동일한 상품/옵션 조합인지 검사.
     */
    public boolean sameProductAndVariant(Long productId, Long variantId) {
        return Objects.equals(this.productId, productId) && Objects.equals(this.variantId, variantId);
    }

    @Override
    public String toString() {
        return "CartItem[id=%s, productId=%s, variantId=%s, qty=%d, unit=%s]"
                .formatted(getId(), productId, variantId, quantity, unitPrice);
    }
}
