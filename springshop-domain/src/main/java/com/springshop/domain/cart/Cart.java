package com.springshop.domain.cart;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 장바구니 애그리거트 루트.
 *
 * <p>한 사용자는 다수의 장바구니를 가질 수 있으나, 활성 상태({@code active=true}) 장바구니는
 * 동시점에 단 1개여야 한다. 결제가 완료되면 해당 장바구니는 비활성화되고, 새 장바구니가
 * 자동으로 생성된다.</p>
 *
 * <p>장바구니 항목은 상품 ID 와 옵션(variant) ID 의 조합으로 식별된다. 동일한 조합으로
 * 다시 추가하면 수량이 증가한다. 가격은 사용자가 담은 시점의 가격을 보존하며, 상품 가격
 * 변동 시 사용자가 확인 후 재계산을 트리거해야 한다.</p>
 *
 * <p>장바구니의 합계는 항목별 단가 * 수량의 합으로 계산되며, 할인/쿠폰/배송비는 주문
 * 단계에서 적용된다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "carts",
        uniqueConstraints = @UniqueConstraint(name = "uk_cart_user_active", columnNames = {"user_id", "active"}),
        indexes = {
                @Index(name = "idx_cart_user", columnList = "user_id"),
                @Index(name = "idx_cart_active", columnList = "active")
        }
)
public class Cart extends BaseAuditEntity {

    /**
     * 장바구니 최대 항목 수. 비즈니스 규칙으로 정의된 값.
     */
    public static final int MAX_ITEMS = 100;

    /**
     * 항목당 최대 수량. 1회 주문 한도를 위한 제한.
     */
    public static final int MAX_QUANTITY_PER_ITEM = 999;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "last_modified_action", length = 50)
    private String lastModifiedAction;

    /**
     * JPA 기본 생성자. 외부 사용 금지.
     */
    protected Cart() {
        super();
    }

    private Cart(Long userId) {
        super();
        this.userId = Objects.requireNonNull(userId, "사용자 ID는 null일 수 없습니다");
        this.active = true;
        this.lastModifiedAction = "CREATED";
    }

    /**
     * 새 장바구니 생성 팩토리.
     *
     * @param userId 사용자 ID (필수)
     * @return 활성 상태의 빈 장바구니
     */
    public static Cart createFor(Long userId) {
        return new Cart(userId);
    }

    public Long getUserId() {
        return userId;
    }

    /**
     * 항목 목록의 불변 뷰를 반환한다.
     */
    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getDeactivatedAt() {
        return deactivatedAt;
    }

    public String getLastModifiedAction() {
        return lastModifiedAction;
    }

    /**
     * 상품을 장바구니에 추가한다. 동일한 (productId, variantId) 조합이 이미 있으면 수량만 증가시킨다.
     *
     * @param productId   상품 ID
     * @param variantId   옵션 ID (없는 경우 null 허용)
     * @param productName 캐싱된 상품명
     * @param thumbnailUrl 썸네일 URL
     * @param quantity    추가 수량
     * @param unitPrice   단가 (사용자가 담은 시점)
     * @return 추가되거나 수량이 증가된 CartItem
     */
    public CartItem addItem(Long productId,
                            Long variantId,
                            String productName,
                            String thumbnailUrl,
                            int quantity,
                            BigDecimal unitPrice) {
        ensureActive();
        Objects.requireNonNull(productId, "상품 ID는 null일 수 없습니다");
        Objects.requireNonNull(unitPrice, "단가는 null일 수 없습니다");
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 합니다: " + quantity);
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("단가는 0 이상이어야 합니다: " + unitPrice);
        }

        Optional<CartItem> existing = findItem(productId, variantId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity > MAX_QUANTITY_PER_ITEM) {
                throw new IllegalStateException(
                        "항목당 최대 수량 초과: %d (한도 %d)".formatted(newQuantity, MAX_QUANTITY_PER_ITEM));
            }
            item.setQuantity(newQuantity);
            this.lastModifiedAction = "QUANTITY_INCREASED";
            return item;
        }

        if (items.size() >= MAX_ITEMS) {
            throw new IllegalStateException(
                    "장바구니에 더 이상 항목을 추가할 수 없습니다 (한도 %d)".formatted(MAX_ITEMS));
        }

        CartItem newItem = CartItem.create(this, productId, variantId, productName, thumbnailUrl, quantity, unitPrice);
        this.items.add(newItem);
        this.lastModifiedAction = "ITEM_ADDED";
        return newItem;
    }

    /**
     * 장바구니에서 특정 항목 제거.
     */
    public boolean removeItem(Long cartItemId) {
        ensureActive();
        Objects.requireNonNull(cartItemId, "cartItemId는 null일 수 없습니다");
        boolean removed = items.removeIf(item -> Objects.equals(item.getId(), cartItemId));
        if (removed) {
            this.lastModifiedAction = "ITEM_REMOVED";
        }
        return removed;
    }

    /**
     * 항목 수량 변경. 0 이하면 제거.
     */
    public void updateItemQuantity(Long cartItemId, int quantity) {
        ensureActive();
        if (quantity <= 0) {
            removeItem(cartItemId);
            return;
        }
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException(
                    "수량은 %d 이하여야 합니다: %d".formatted(MAX_QUANTITY_PER_ITEM, quantity));
        }
        CartItem item = items.stream()
                .filter(it -> Objects.equals(it.getId(), cartItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("장바구니에 해당 항목이 없습니다: " + cartItemId));
        item.setQuantity(quantity);
        this.lastModifiedAction = "QUANTITY_UPDATED";
    }

    /**
     * 장바구니 전체 비우기.
     */
    public void clear() {
        ensureActive();
        if (!items.isEmpty()) {
            this.items.clear();
            this.lastModifiedAction = "CLEARED";
        }
    }

    /**
     * 항목별 소계의 합. 할인이나 배송비는 포함되지 않는다.
     */
    public BigDecimal calculateSubtotal() {
        return items.stream()
                .map(CartItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 항목 종류 수. 동일 상품의 수량이 5인 경우라도 1로 카운트한다.
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * 전체 수량 합. (3개 + 5개 = 8)
     */
    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * (productId, variantId) 조합으로 항목 검색.
     */
    public Optional<CartItem> findItem(Long productId, Long variantId) {
        Objects.requireNonNull(productId, "상품 ID는 null일 수 없습니다");
        return items.stream()
                .filter(item -> Objects.equals(item.getProductId(), productId)
                        && Objects.equals(item.getVariantId(), variantId))
                .findFirst();
    }

    /**
     * 장바구니 비활성화. 일반적으로 주문 완료 시 호출된다.
     */
    public void deactivate() {
        if (!this.active) {
            throw new IllegalStateException("이미 비활성화된 장바구니입니다");
        }
        this.active = false;
        this.deactivatedAt = LocalDateTime.now();
        this.lastModifiedAction = "DEACTIVATED";
    }

    /**
     * 특정 상품의 총 수량 조회. 동일 상품이 옵션 별로 여러 항목으로 나뉜 경우 합산.
     */
    public int getQuantityOfProduct(Long productId) {
        Objects.requireNonNull(productId, "상품 ID는 null일 수 없습니다");
        return items.stream()
                .filter(it -> Objects.equals(it.getProductId(), productId))
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * 장바구니 항목 중 최소 단가.
     */
    public Optional<BigDecimal> getMinUnitPrice() {
        return items.stream()
                .map(CartItem::getUnitPrice)
                .min(BigDecimal::compareTo);
    }

    /**
     * 장바구니 항목 중 최대 단가.
     */
    public Optional<BigDecimal> getMaxUnitPrice() {
        return items.stream()
                .map(CartItem::getUnitPrice)
                .max(BigDecimal::compareTo);
    }

    private void ensureActive() {
        if (!this.active) {
            throw new IllegalStateException("비활성 상태의 장바구니는 수정할 수 없습니다: id=" + getId());
        }
    }

    /**
     * 디버깅용 요약 정보.
     */
    public String summary() {
        return """
                Cart[id=%s, userId=%s, active=%s, items=%d, totalQty=%d, subtotal=%s]
                """.formatted(getId(), userId, active, items.size(), getTotalQuantity(), calculateSubtotal());
    }
}
