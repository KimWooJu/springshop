package com.springshop.web.controller;

import com.springshop.web.dto.request.ApplyCouponRequest;
import com.springshop.web.dto.request.CreateCouponRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.CouponResponse;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 쿠폰 API 컨트롤러.
 *
 * <p>사용자 보유 쿠폰 조회, 쿠폰 발급, 쿠폰 적용/검증, 관리자 쿠폰 CRUD,
 * 쿠폰 코드 등록 등 쿠폰 도메인 전반의 엔드포인트를 제공한다.
 *
 * <p>쿠폰 정책 종류: 정액 할인(FIXED) / 정률 할인(PERCENT) / 무료 배송(FREE_SHIPPING).
 */
@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "쿠폰 API", description = "쿠폰 관리 및 적용")
public class CouponController {

    private static final Logger LOG = LoggerFactory.getLogger(CouponController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(80000);

    @Operation(summary = "내 쿠폰 목록", description = "로그인한 사용자의 보유 쿠폰을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getMyCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeExpired,
            @RequestParam(defaultValue = "false") boolean usableOnly) {
        LOG.info("내 쿠폰 목록 - page={}, includeExpired={}, usableOnly={}",
                page, includeExpired, usableOnly);
        List<CouponResponse> coupons = buildSampleCoupons(size, usableOnly);
        return ResponseEntity.ok(ApiResponse.Paginated.of(coupons, 23L, page, size));
    }

    @Operation(summary = "쿠폰 생성 (관리자)", description = "관리자가 새 쿠폰을 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "코드 중복")
    })
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {
        LOG.info("쿠폰 생성 - code={}, discountType={}, value={}",
                request.code(), request.discountType(), request.discountValue());

        Long newId = ID_SEQ.incrementAndGet();
        CouponResponse data = new CouponResponse(
                newId, request.code(), request.name(), request.description(),
                request.discountType(), request.discountValue(),
                request.minimumOrderAmount(), request.maxDiscountAmount(),
                request.totalIssueQuantity(), 0,
                request.totalIssueQuantity(),
                request.maxUsagePerUser() == null ? 1 : request.maxUsagePerUser(),
                0,
                request.hasCategoryRestriction() ? request.applicableCategoryIds().get(0) : null,
                request.hasProductRestriction() ? request.applicableProductIds().get(0) : null,
                request.validFrom() == null ? null : request.validFrom().toLocalDate(),
                request.validUntil() == null ? null : request.validUntil().toLocalDate(),
                true, true, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "쿠폰이 생성되었습니다"));
    }

    @Operation(summary = "전체 쿠폰 목록 (관리자)", description = "관리자가 시스템 내 모든 쿠폰을 페이징 조회한다.")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> adminCouponList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        LOG.info("관리자 쿠폰 목록 - page={}, status={}, keyword={}", page, status, keyword);
        List<CouponResponse> coupons = buildSampleCoupons(size, false);
        return ResponseEntity.ok(ApiResponse.Paginated.of(coupons, 567L, page, size));
    }

    @Operation(summary = "쿠폰 수정 (관리자)", description = "관리자가 쿠폰 정보를 수정한다. 이미 사용된 쿠폰은 수정 제한적.")
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CreateCouponRequest request) {
        LOG.info("쿠폰 수정 - id={}, code={}", id, request.code());
        CouponResponse data = new CouponResponse(
                id, request.code(), request.name(), request.description(),
                request.discountType(), request.discountValue(),
                request.minimumOrderAmount(), request.maxDiscountAmount(),
                request.totalIssueQuantity(), 0,
                request.totalIssueQuantity(), 1, 0, null, null,
                request.validFrom() == null ? null : request.validFrom().toLocalDate(),
                request.validUntil() == null ? null : request.validUntil().toLocalDate(),
                true, true, LocalDateTime.now().minusDays(7));
        return ResponseEntity.ok(ApiResponse.Success.of(data, "쿠폰이 수정되었습니다"));
    }

    @Operation(summary = "쿠폰 삭제 (관리자)", description = "쿠폰을 비활성화한다. 이미 발급된 쿠폰은 그대로 유지된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean forceDelete) {
        LOG.warn("쿠폰 삭제 - id={}, forceDelete={}", id, forceDelete);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.Success.empty());
    }

    @Operation(summary = "쿠폰 검증", description = "쿠폰 코드와 주문 금액의 유효성을 검증한다 (적용 전 미리보기).")
    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCoupon(
            @Valid @RequestBody ApplyCouponRequest request) {
        LOG.info("쿠폰 검증 - code={}, amount={}", request.couponCode(), request.orderAmount());

        // Java 25 패턴 매칭으로 검증 결과 분류
        String validationStatus = switch (request.couponCode()) {
            case String code when code.startsWith("INVALID") -> "INVALID";
            case String code when code.startsWith("EXPIRED") -> "EXPIRED";
            case String code when code.startsWith("USED") -> "ALREADY_USED";
            case String code when request.orderAmount().compareTo(new BigDecimal("30000")) < 0
                    -> "MIN_ORDER_NOT_MET";
            case null -> "NULL_CODE";
            default -> "VALID";
        };

        BigDecimal discount = "VALID".equals(validationStatus)
                ? request.orderAmount().multiply(new BigDecimal("0.1"))
                : BigDecimal.ZERO;

        Map<String, Object> result = Map.of(
                "couponCode", request.couponCode(),
                "validationStatus", validationStatus,
                "isValid", "VALID".equals(validationStatus),
                "discountAmount", discount,
                "finalAmount", request.orderAmount().subtract(discount),
                "minOrderAmount", new BigDecimal("30000"),
                "validatedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result));
    }

    @Operation(summary = "사용 가능한 쿠폰", description = "특정 주문에 적용 가능한 쿠폰 목록을 조회한다.")
    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAvailableCoupons(
            @RequestParam(required = false) Long cartId,
            @RequestParam(required = false) BigDecimal orderAmount,
            @RequestParam(required = false) List<Long> productIds,
            @RequestParam(required = false) List<Long> categoryIds) {
        LOG.debug("사용 가능 쿠폰 - cartId={}, amount={}, products={}",
                cartId, orderAmount, productIds);
        List<CouponResponse> coupons = buildSampleCoupons(5, true);
        return ResponseEntity.ok(ApiResponse.Success.of(coupons,
                coupons.size() + "개의 쿠폰을 사용할 수 있습니다"));
    }

    @Operation(summary = "쿠폰 발급/등록", description = "쿠폰 코드를 입력하여 본인 계정에 발급받는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 발급됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유효하지 않은 코드")
    })
    @PostMapping("/{code}/issue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CouponResponse>> issueCoupon(
            @Parameter(description = "쿠폰 코드", required = true, example = "WELCOME2026")
            @PathVariable String code) {
        LOG.info("쿠폰 발급 - code={}", code);
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        if (normalizedCode.isBlank() || normalizedCode.length() < 6) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.Failure.of("INVALID_CODE", "유효하지 않은 쿠폰 코드입니다."));
        }
        CouponResponse data = new CouponResponse(
                ID_SEQ.incrementAndGet(), normalizedCode, "신규 발급 쿠폰",
                "신규 발급된 쿠폰입니다.", "PERCENTAGE",
                new BigDecimal("10"), new BigDecimal("30000"), new BigDecimal("5000"),
                10000, 0, 9999, 1, 0, null, null,
                LocalDate.now(), LocalDate.now().plusMonths(3),
                true, true, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "쿠폰이 발급되었습니다"));
    }

    @Operation(summary = "쿠폰 일괄 발급 (관리자)", description = "관리자가 특정 사용자 그룹에 쿠폰을 일괄 발급한다.")
    @PostMapping("/admin/bulk-issue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkIssueCoupons(
            @RequestParam Long couponId,
            @RequestParam List<Long> userIds,
            @RequestParam(required = false) String reason) {
        LOG.info("쿠폰 일괄 발급 - couponId={}, userCount={}, reason={}",
                couponId, userIds.size(), reason);
        Map<String, Object> result = Map.of(
                "couponId", couponId,
                "targetUserCount", userIds.size(),
                "successCount", userIds.size(),
                "failedCount", 0,
                "issuedAt", LocalDateTime.now().toString(),
                "batchId", "bulk-" + ID_SEQ.incrementAndGet()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.Success.of(result, "일괄 발급이 시작되었습니다"));
    }

    @Operation(summary = "쿠폰 사용 이력", description = "특정 쿠폰의 사용 이력을 조회한다 (관리자).")
    @GetMapping("/admin/{id}/usage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCouponUsage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        LOG.debug("쿠폰 사용 이력 - id={}, page={}", id, page);
        List<Map<String, Object>> usage = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            usage.add(Map.of(
                    "usageId", (long) (i + 1),
                    "couponId", id,
                    "userId", 1000L + i,
                    "orderId", 50000L + i,
                    "discountAmount", new BigDecimal("5000"),
                    "usedAt", LocalDateTime.now().minusDays(i % 30).toString()
            ));
        }
        return ResponseEntity.ok(ApiResponse.Paginated.of(usage, 1234L, page, size));
    }

    // ---------------------------------------
    // 헬퍼
    // ---------------------------------------

    private List<CouponResponse> buildSampleCoupons(int size, boolean usableOnly) {
        List<CouponResponse> coupons = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String type = switch (i % 3) {
                case 0 -> "PERCENTAGE";
                case 1 -> "FIXED_AMOUNT";
                default -> "FREE_SHIPPING";
            };
            BigDecimal value = switch (type) {
                case "PERCENTAGE" -> new BigDecimal("10");
                case "FIXED_AMOUNT" -> new BigDecimal("5000");
                default -> BigDecimal.ZERO;
            };
            coupons.add(new CouponResponse(
                    (long) (80000 + i), "COUPON" + i, "샘플 쿠폰 " + i,
                    "쿠폰 설명 " + i, type, value,
                    new BigDecimal("30000"), new BigDecimal("10000"),
                    10000, 100 * i, 10000 - (100 * i), 1, usableOnly ? 0 : i % 2,
                    null, null,
                    LocalDate.now().minusDays(7), LocalDate.now().plusMonths(1),
                    true, true, LocalDateTime.now().minusDays(i)));
        }
        return coupons;
    }
}
