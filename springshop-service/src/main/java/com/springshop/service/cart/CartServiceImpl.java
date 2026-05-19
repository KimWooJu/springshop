package com.springshop.service.cart;

import com.springshop.common.constant.AppConstants;
import com.springshop.common.exception.BusinessException;
import com.springshop.common.exception.ErrorCode;
import com.springshop.common.exception.ResourceNotFoundException;
import com.springshop.domain.cart.Cart;
import com.springshop.domain.cart.CartItem;
import com.springshop.domain.cart.CartRepository;
import com.springshop.domain.product.Product;
import com.springshop.domain.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 장바구니 핵심 서비스 구현체.
 *
 * <p>{@link CartService} 의 모든 메서드를 영속성/도메인 규칙에 기반하여 구현한다.
 * 한 사용자당 활성 장바구니는 1개만 유지되도록 보장하며, 충돌 발생 시 도메인이 던지는
 * 예외를 그대로 전파하거나 {@link BusinessException} 으로 변환한다.</p>
 *
 * <p>가격 계산은 단순 단가 × 수량 + 무료배송 임계값 기반 배송비 가산으로 구성된다.
 * 쿠폰 적용은 {@code CouponService} 가 별도로 처리하며, 본 서비스는 쿠폰 코드 문자열을
 * 그대로 요약 객체에 담아 노출한다.</p>
 */
@Service
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Autowired
    public CartServiceImpl(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = Objects.requireNonNull(cartRepository, "CartRepository는 필수입니다");
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository는 필수입니다");
    }

    @Override
    @Transactional
    public Cart getOrCreateCart(Long userId) {
        validateUserId(userId);
        return cartRepository.findActiveByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.createFor(userId);
                    Cart saved = cartRepository.save(newCart);
                    log.debug("새 장바구니 생성: userId={}, cartId={}", userId, saved.getId());
                    return saved;
                });
    }

    @Override
    @Transactional
    public CartItem addItem(Long userId, Long productId, Long variantId, int quantity) {
        validateUserId(userId);
        Objects.requireNonNull(productId, "productId는 필수입니다");
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 양수여야 합니다: " + quantity);
        }

        Cart cart = getOrCreateCart(userId);

        // 장바구니 항목 수 제한 (도메인 상수 또는 글로벌 상수 중 작은 값을 사용)
        int limit = Math.min(AppConstants.MAX_CART_ITEMS, Cart.MAX_ITEMS);
        if (cart.getItemCount() >= limit && cart.findItem(productId, variantId).isEmpty()) {
            log.warn("장바구니 한도 초과: userId={}, 현재={}, 한도={}", userId, cart.getItemCount(), limit);
            throw new BusinessException(ErrorCode.SYSTEM_REQUEST_INVALID,
                    "장바구니는 최대 " + limit + "개까지 담을 수 있습니다");
        }

        // 상품 검증
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.product(productId));

        // 가격 산출 — variant 가격 가산은 별도 모듈 도입 시 처리
        BigDecimal unitPrice = product.getBasePrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PRODUCT_PRICE_INVALID);
        }

        CartItem item = cart.addItem(productId, variantId,
                product.getName(),
                /* thumbnailUrl */ null,
                quantity,
                unitPrice);

        cartRepository.save(cart);
        log.info("장바구니 아이템 추가: userId={}, productId={}, variantId={}, qty={}",
                userId, productId, variantId, quantity);
        return item;
    }

    @Override
    @Transactional
    public CartItem updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        validateUserId(userId);
        Objects.requireNonNull(cartItemId, "cartItemId는 필수입니다");

        Cart cart = getOrCreateCart(userId);
        cart.updateItemQuantity(cartItemId, quantity);
        cartRepository.save(cart);

        // 수량 0 이하인 경우 항목이 제거되었으므로 null 대신 가장 가까운 의미 있는 항목 반환
        return cart.getItems().stream()
                .filter(i -> Objects.equals(i.getId(), cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", cartItemId));
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        validateUserId(userId);
        Objects.requireNonNull(cartItemId, "cartItemId는 필수입니다");

        Cart cart = getOrCreateCart(userId);
        boolean removed = cart.removeItem(cartItemId);
        if (!removed) {
            log.warn("장바구니 항목 제거 대상 없음: userId={}, cartItemId={}", userId, cartItemId);
            throw new ResourceNotFoundException("CartItem", cartItemId);
        }
        cartRepository.save(cart);
        log.info("장바구니 아이템 제거: userId={}, cartItemId={}", userId, cartItemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        validateUserId(userId);
        Cart cart = getOrCreateCart(userId);
        if (cart.isEmpty()) {
            log.debug("빈 장바구니 비우기 요청: userId={}", userId);
            return;
        }
        cart.clear();
        cartRepository.save(cart);
        log.info("장바구니 초기화: userId={}", userId);
    }

    @Override
    public Cart getCart(Long userId) {
        validateUserId(userId);
        // 빈 객체를 반환하면 영속성 컨텍스트 외부에서 사용 가능
        return cartRepository.findActiveWithItemsByUserId(userId)
                .or(() -> cartRepository.findActiveByUserId(userId))
                .orElseGet(() -> Cart.createFor(userId));
    }

    @Override
    public CartSummary calculateSummary(Long userId, String couponCode) {
        validateUserId(userId);
        Cart cart = getCart(userId);
        BigDecimal subtotal = cart.calculateSubtotal();
        BigDecimal discount = computeCouponDiscount(couponCode, subtotal);

        BigDecimal discounted = subtotal.subtract(discount);
        if (discounted.compareTo(BigDecimal.ZERO) < 0) {
            discounted = BigDecimal.ZERO;
        }

        BigDecimal shippingFee = calculateShippingFee(discounted);
        boolean freeShipping = shippingFee.compareTo(BigDecimal.ZERO) == 0;

        BigDecimal total = discounted.add(shippingFee);

        log.debug("장바구니 합계: userId={}, subtotal={}, discount={}, ship={}, total={}",
                userId, subtotal, discount, shippingFee, total);

        return new CartSummary(subtotal, discount, shippingFee, total, couponCode, freeShipping);
    }

    @Override
    @Transactional
    public void mergeGuestCart(Long userId, List<GuestCartItem> guestItems) {
        validateUserId(userId);
        if (guestItems == null || guestItems.isEmpty()) {
            log.debug("비회원 장바구니 비어있음: userId={}", userId);
            return;
        }

        int success = 0;
        int failure = 0;
        for (GuestCartItem guestItem : guestItems) {
            try {
                addItem(userId, guestItem.productId(), guestItem.variantId(), guestItem.quantity());
                success++;
            } catch (RuntimeException e) {
                failure++;
                log.warn("비회원 장바구니 병합 실패: productId={}, reason={}",
                        guestItem.productId(), e.getMessage());
            }
        }
        log.info("비회원 장바구니 병합 완료: userId={}, total={}, success={}, fail={}",
                userId, guestItems.size(), success, failure);
    }

    @Override
    @Transactional
    public void deactivateCart(Long userId) {
        validateUserId(userId);
        Optional<Cart> active = cartRepository.findActiveByUserId(userId);
        if (active.isPresent()) {
            Cart cart = active.get();
            cart.deactivate();
            cartRepository.save(cart);
            log.info("장바구니 비활성화 완료: userId={}, cartId={}", userId, cart.getId());
        } else {
            log.debug("비활성화할 활성 장바구니 없음: userId={}", userId);
        }
    }

    @Override
    public int getCartItemCount(Long userId) {
        validateUserId(userId);
        return cartRepository.findActiveByUserId(userId)
                .map(Cart::getItemCount)
                .orElse(0);
    }

    @Override
    public boolean isProductInCart(Long userId, Long productId) {
        validateUserId(userId);
        Objects.requireNonNull(productId, "productId는 필수입니다");
        return cartRepository.findActiveWithItemsByUserId(userId)
                .map(cart -> cart.getItems().stream()
                        .anyMatch(item -> Objects.equals(item.getProductId(), productId)))
                .orElse(false);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * 무료 배송 임계값을 초과하면 0, 미만이면 정액 배송비를 반환한다.
     */
    private BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        if (orderAmount == null) {
            return BigDecimal.valueOf(AppConstants.SHIPPING_FEE);
        }
        return orderAmount.compareTo(BigDecimal.valueOf(AppConstants.FREE_SHIPPING_THRESHOLD)) >= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(AppConstants.SHIPPING_FEE);
    }

    /**
     * 쿠폰 코드를 기반으로 할인액을 산출한다. 본 서비스는 쿠폰 시스템과 직접 연결되지 않으므로
     * 0 을 반환한다. 실제 할인은 CouponService 호출 시 적용된다.
     */
    private BigDecimal computeCouponDiscount(String couponCode, BigDecimal subtotal) {
        if (couponCode == null || couponCode.isBlank()) {
            return BigDecimal.ZERO;
        }
        // 쿠폰 모듈 미연결 단계에서는 0원 할인을 반환한다.
        log.debug("쿠폰 적용 요청 (할인 계산은 CouponService 위임): couponCode={}, subtotal={}",
                couponCode, subtotal);
        return BigDecimal.ZERO;
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID: " + userId);
        }
    }
}
