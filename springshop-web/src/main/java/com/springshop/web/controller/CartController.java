package com.springshop.web.controller;

import com.springshop.web.dto.request.AddToCartRequest;
import com.springshop.web.dto.request.ApplyCouponRequest;
import com.springshop.web.dto.request.UpdateCartItemRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.CartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 장바구니 API 컨트롤러.
 *
 * <p>회원/게스트 장바구니 조회, 상품 추가/수정/삭제, 일괄 비우기, 쿠폰 적용/해제,
 * 결제 직전 요약 조회 등 장바구니 도메인의 모든 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "장바구니 API", description = "장바구니 CRUD/쿠폰 적용")
public class CartController {

    private static final Logger LOG = LoggerFactory.getLogger(CartController.class);
    private static final AtomicLong ITEM_SEQ = new AtomicLong(70000);

    @Operation(summary = "내 장바구니 조회", description = "로그인한 사용자의 장바구니를 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestParam(required = false) String guestKey) {
        LOG.debug("장바구니 조회 - guestKey={}", guestKey);
        CartResponse data = buildSampleCart(true);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "장바구니 항목 추가", description = "상품을 장바구니에 담는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "재고 부족")
    })
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddToCartRequest request) {
        LOG.info("장바구니 추가 - productId={}, qty={}, variant={}",
                request.productId(), request.quantity(), request.variantId());
        CartResponse data = buildSampleCart(true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "장바구니에 추가되었습니다"));
    }

    @Operation(summary = "장바구니 항목 수정", description = "수량 변경, 옵션 변경, 선택 토글.")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @Parameter(description = "장바구니 항목 ID", required = true) @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        LOG.info("장바구니 수정 - itemId={}, qty={}", itemId, request.quantity());
        if (request.isRemoveRequest()) {
            LOG.info("수량 0 → 자동 삭제");
        }
        CartResponse data = buildSampleCart(!request.isRemoveRequest());
        return ResponseEntity.ok(ApiResponse.Success.of(data, "수정 완료"));
    }

    @Operation(summary = "장바구니 항목 삭제", description = "특정 항목을 삭제한다.")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long itemId) {
        LOG.info("장바구니 항목 삭제 - itemId={}", itemId);
        return ResponseEntity.ok(ApiResponse.Success.of(buildSampleCart(false), "삭제 완료"));
    }

    @Operation(summary = "장바구니 비우기", description = "모든 항목을 일괄 삭제한다.")
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart() {
        LOG.warn("장바구니 비우기");
        return ResponseEntity.ok(ApiResponse.Success.of(CartResponse.empty(1024L), "장바구니를 비웠습니다"));
    }

    @Operation(summary = "쿠폰 적용", description = "장바구니에 쿠폰을 적용한다 (검증 + 할인 계산).")
    @PostMapping("/coupon")
    public ResponseEntity<ApiResponse<CartResponse>> applyCoupon(
            @Valid @RequestBody ApplyCouponRequest request) {
        LOG.info("쿠폰 적용 - code={}", request.couponCode());
        CartResponse data = buildSampleCart(true);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "쿠폰이 적용되었습니다"));
    }

    @Operation(summary = "쿠폰 해제", description = "적용된 쿠폰을 해제한다.")
    @DeleteMapping("/coupon")
    public ResponseEntity<ApiResponse<CartResponse>> removeCoupon() {
        LOG.info("쿠폰 해제");
        return ResponseEntity.ok(ApiResponse.Success.of(buildSampleCart(false), "쿠폰 해제 완료"));
    }

    @Operation(summary = "장바구니 요약", description = "결제 직전 화면용 요약 정보.")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CartResponse>> summary() {
        LOG.debug("장바구니 요약");
        CartResponse data = buildSampleCart(true);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "선택 항목 일괄 삭제", description = "체크된 항목만 삭제한다.")
    @DeleteMapping("/items/selected")
    public ResponseEntity<ApiResponse<CartResponse>> removeSelected() {
        LOG.info("선택 항목 삭제");
        return ResponseEntity.ok(ApiResponse.Success.of(buildSampleCart(false), "삭제 완료"));
    }

    @Operation(summary = "전체 선택 토글", description = "모든 항목의 선택 상태를 토글한다.")
    @PutMapping("/items/select-all")
    public ResponseEntity<ApiResponse<CartResponse>> toggleSelectAll(
            @RequestParam boolean selected) {
        LOG.info("전체 선택 토글 - selected={}", selected);
        return ResponseEntity.ok(ApiResponse.Success.of(buildSampleCart(true)));
    }

    @Operation(summary = "장바구니 병합 (게스트 → 회원)", description = "로그인 시 게스트 장바구니를 회원 장바구니에 병합.")
    @PostMapping("/merge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> mergeGuestCart(
            @RequestParam String guestKey) {
        LOG.info("게스트 장바구니 병합 - guestKey={}", guestKey);
        return ResponseEntity.ok(ApiResponse.Success.of(buildSampleCart(true), "병합 완료"));
    }

    private CartResponse buildSampleCart(boolean withItems) {
        List<CartResponse.CartItemRef> items = new ArrayList<>();
        if (withItems) {
            items.add(new CartResponse.CartItemRef(
                    ITEM_SEQ.incrementAndGet(), 10001L, "샘플 상품 A",
                    "https://cdn.example.com/p1.jpg",
                    2001L, "블랙 / L",
                    2, new BigDecimal("19900"), new BigDecimal("17910"),
                    new BigDecimal("35820"), 100, false, true, "AVAILABLE"));
            items.add(new CartResponse.CartItemRef(
                    ITEM_SEQ.incrementAndGet(), 10002L, "샘플 상품 B",
                    "https://cdn.example.com/p2.jpg",
                    null, "기본",
                    1, new BigDecimal("29900"), null,
                    new BigDecimal("29900"), 50, false, true, "AVAILABLE"));
        }
        BigDecimal subtotal = new BigDecimal(withItems ? "65720" : "0");
        BigDecimal shippingFee = subtotal.compareTo(new BigDecimal("50000")) >= 0
                ? BigDecimal.ZERO : new BigDecimal("3000");
        BigDecimal couponDiscount = BigDecimal.ZERO;
        BigDecimal finalAmount = subtotal.add(shippingFee).subtract(couponDiscount);
        return new CartResponse(
                ITEM_SEQ.incrementAndGet(), 1024L, null, items,
                subtotal, BigDecimal.ZERO, couponDiscount, shippingFee, finalAmount,
                (int) Math.round(finalAmount.doubleValue() * 0.01),
                null,
                items.size(),
                items.stream().mapToInt(CartResponse.CartItemRef::quantity).sum(),
                subtotal.compareTo(new BigDecimal("10000")) >= 0,
                subtotal.compareTo(new BigDecimal("50000")) >= 0,
                new BigDecimal("50000").subtract(subtotal).max(BigDecimal.ZERO),
                LocalDateTime.now());
    }
}
