package com.springshop.service.cart;

import com.springshop.domain.cart.Cart;
import com.springshop.domain.cart.CartItem;

import java.math.BigDecimal;
import java.util.List;

/**
 * 장바구니 핵심 서비스.
 *
 * <p>사용자별 활성 장바구니의 생성·항목 추가/수정/삭제·합계 계산·비활성화를 담당한다.
 * 비회원에서 회원으로 전환 시 비회원 장바구니 항목을 회원 장바구니로 병합한다.</p>
 *
 * <p>가격 변경, 재고 부족, 쿠폰 적용 등의 정책은 별도 서비스(ProductService, InventoryService,
 * CouponService)와 협업한다. 트랜잭션 경계는 Impl 측에서 명시한다.</p>
 *
 * @author SpringShop Service Team
 */
public interface CartService {

    /**
     * 사용자의 활성 장바구니를 조회하거나, 없으면 새로 생성하여 반환한다.
     */
    Cart getOrCreateCart(Long userId);

    /**
     * 장바구니에 상품을 추가한다. 동일한 (productId, variantId) 조합이 이미 있으면 수량만 증가시킨다.
     */
    CartItem addItem(Long userId, Long productId, Long variantId, int quantity);

    /**
     * 장바구니 항목의 수량을 변경한다. 0 이하 값이 들어오면 해당 항목을 제거한다.
     */
    CartItem updateItemQuantity(Long userId, Long cartItemId, int quantity);

    /**
     * 장바구니 항목 제거.
     */
    void removeItem(Long userId, Long cartItemId);

    /**
     * 장바구니 전체 비우기.
     */
    void clearCart(Long userId);

    /**
     * 사용자의 현재 활성 장바구니 (없으면 빈 객체).
     */
    Cart getCart(Long userId);

    /**
     * 합계 계산. 소계 / 할인 / 배송비 / 총합 / 적용 쿠폰 / 무료배송 여부.
     */
    CartSummary calculateSummary(Long userId, String couponCode);

    /**
     * 비회원 장바구니 → 회원 장바구니 병합. 로그인 직후 호출된다.
     */
    void mergeGuestCart(Long userId, List<GuestCartItem> guestItems);

    /**
     * 장바구니 비활성화. 주문 완료 시 호출.
     */
    void deactivateCart(Long userId);

    /**
     * 장바구니 항목 종류 수.
     */
    int getCartItemCount(Long userId);

    /**
     * 특정 상품이 장바구니에 담겨있는지.
     */
    boolean isProductInCart(Long userId, Long productId);

    /**
     * 장바구니 합계 요약.
     */
    record CartSummary(BigDecimal subtotal,
                       BigDecimal discountAmount,
                       BigDecimal shippingFee,
                       BigDecimal total,
                       String appliedCoupon,
                       boolean freeShipping) {
        /**
         * 할인이 전혀 적용되지 않은 경우.
         */
        public boolean hasNoDiscount() {
            return discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) == 0;
        }
    }

    /**
     * 비회원 장바구니 항목.
     */
    record GuestCartItem(Long productId, Long variantId, int quantity) {
        public GuestCartItem {
            if (productId == null) {
                throw new IllegalArgumentException("productId는 필수입니다");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity는 양수여야 합니다");
            }
        }
    }
}
