package com.springshop.service.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 쿠폰 서비스 인터페이스.
 *
 * <p>쿠폰 생성, 발급, 유효성 검증, 적용, 만료 처리, 통계 등
 * 쿠폰 라이프사이클 전반을 정의한다.
 *
 * <p>동시성 환경에서의 중복 발급을 방지하기 위해 낙관적 락과
 * Redis를 이용한 분산 락을 사용한다.
 */
public interface CouponService {

    /**
     * 새 쿠폰을 생성한다 (관리자 전용).
     *
     * @param code           쿠폰 코드 (고유)
     * @param discountType   할인 유형 (FIXED / PERCENT)
     * @param discountValue  할인 값
     * @param minOrderAmount 최소 주문 금액
     * @param maxUsageCount  총 사용 한도
     * @param startDate      유효 시작일
     * @param endDate        유효 종료일
     * @return               생성된 쿠폰 ID
     */
    Long createCoupon(String code, String discountType, BigDecimal discountValue,
                      BigDecimal minOrderAmount, int maxUsageCount,
                      LocalDate startDate, LocalDate endDate);

    /**
     * 사용자에게 쿠폰을 발급한다.
     * 발급 한도 초과, 이미 발급된 경우 예외를 던진다.
     *
     * @param couponCode 발급할 쿠폰 코드
     * @param userId     수령자 ID
     * @return           사용자 쿠폰 ID
     */
    Long issueCoupon(String couponCode, Long userId);

    /**
     * 주문에 쿠폰을 적용하고 할인 금액을 계산한다.
     *
     * @param couponCode   적용할 쿠폰 코드
     * @param userId       적용자 ID
     * @param orderAmount  주문 원금
     * @return             적용 결과 (할인 금액, 최종 금액)
     */
    CouponApplicationResult applyCoupon(String couponCode, Long userId, BigDecimal orderAmount);

    /**
     * 쿠폰 유효성만 검증한다 (적용하지 않음).
     *
     * @param couponCode  검증할 쿠폰 코드
     * @param userId      검증 요청자 ID
     * @param orderAmount 주문 금액
     * @return            검증 결과
     */
    CouponValidationResult validateCoupon(String couponCode, Long userId, BigDecimal orderAmount);

    /**
     * 쿠폰 정보를 단건 조회한다.
     *
     * @param couponId 쿠폰 ID
     * @return CouponDto
     */
    CouponDto getCoupon(Long couponId);

    /**
     * 사용자 보유 쿠폰 목록을 조회한다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 쿠폰 페이지
     */
    Page<CouponDto> getUserCoupons(Long userId, Pageable pageable);

    /**
     * 기간 만료 쿠폰을 일괄 만료 처리한다 (스케줄러 호출).
     *
     * @return 만료 처리된 쿠폰 수
     */
    int expireCoupons();

    /**
     * 현재 사용 가능한 쿠폰 목록을 조회한다.
     *
     * @param userId 사용자 ID
     * @return 사용 가능 쿠폰 목록
     */
    List<CouponDto> getAvailableCoupons(Long userId);

    /**
     * 쿠폰을 폐기한다 (관리자 전용).
     *
     * @param couponId 폐기할 쿠폰 ID
     * @param reason   폐기 사유
     */
    void revokeCoupon(Long couponId, String reason);

    /**
     * 쿠폰 사용 통계를 집계한다.
     *
     * @param couponId 쿠폰 ID
     * @return 통계 데이터 (totalIssued, totalUsed, totalDiscount 등)
     */
    CouponStatistics getCouponStatistics(Long couponId);

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /**
     * 쿠폰 유효성 검증 결과 — sealed interface로 타입 안전성 보장.
     */
    sealed interface CouponValidationResult
            permits CouponValidationResult.Valid,
                    CouponValidationResult.Expired,
                    CouponValidationResult.NotApplicable,
                    CouponValidationResult.UsageLimitReached,
                    CouponValidationResult.AlreadyUsed {

        record Valid(BigDecimal discountAmount, BigDecimal finalAmount) implements CouponValidationResult {}
        record Expired(String couponCode, java.time.LocalDate expiredAt) implements CouponValidationResult {}
        record NotApplicable(String reason) implements CouponValidationResult {}
        record UsageLimitReached(int limit, int current) implements CouponValidationResult {}
        record AlreadyUsed(java.time.LocalDateTime usedAt) implements CouponValidationResult {}
    }

    /** 쿠폰 적용 결과 */
    record CouponApplicationResult(
            String couponCode,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String discountType
    ) {}

    /** 쿠폰 데이터 전송 객체 */
    record CouponDto(
            Long id,
            String code,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minOrderAmount,
            int maxUsageCount,
            int currentUsageCount,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            boolean isUsed,
            java.time.LocalDateTime usedAt
    ) {}

    /** 쿠폰 통계 */
    record CouponStatistics(
            Long couponId,
            String couponCode,
            long totalIssued,
            long totalUsed,
            BigDecimal totalDiscountAmount,
            double usageRate
    ) {}
}
