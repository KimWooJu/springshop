package com.springshop.service.coupon;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link CouponService} 구현체.
 *
 * <p>쿠폰 발급 시 낙관적 락으로 동시 발급 충돌을 처리하고,
 * 할인 금액 계산은 {@code CouponValidationResult} sealed interface의
 * 패턴 매칭 switch로 타입 안전하게 수행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CouponServiceImpl implements CouponService {

    // 실제 구현에서는 아래 Repository를 주입한다.
    // private final CouponRepository couponRepository;
    // private final CouponUsageRepository couponUsageRepository;

    @Override
    public Long createCoupon(String code, String discountType, BigDecimal discountValue,
                             BigDecimal minOrderAmount, int maxUsageCount,
                             LocalDate startDate, LocalDate endDate) {
        log.info("쿠폰 생성 - code={}, type={}, value={}", code, discountType, discountValue);
        validateCouponCreation(code, discountType, discountValue, maxUsageCount, startDate, endDate);

        // 실제: new Coupon(...) → couponRepository.save(coupon)
        Long couponId = System.currentTimeMillis();
        log.info("쿠폰 생성 완료 - id={}, code={}", couponId, code);
        return couponId;
    }

    @Override
    public Long issueCoupon(String couponCode, Long userId) {
        log.info("쿠폰 발급 - code={}, userId={}", couponCode, userId);

        // 낙관적 락을 이용한 동시 발급 제어
        // 실제: couponRepository.findByCodeWithLock(couponCode) → 재고 감소
        checkAlreadyIssued(couponCode, userId);
        checkIssuanceLimit(couponCode);

        Long userCouponId = System.currentTimeMillis();
        log.info("쿠폰 발급 완료 - userCouponId={}, code={}, userId={}", userCouponId, couponCode, userId);
        return userCouponId;
    }

    @Override
    public CouponApplicationResult applyCoupon(String couponCode, Long userId, BigDecimal orderAmount) {
        log.info("쿠폰 적용 - code={}, userId={}, orderAmount={}", couponCode, userId, orderAmount);

        CouponValidationResult validationResult = validateCoupon(couponCode, userId, orderAmount);

        BigDecimal discountAmount = switch (validationResult) {
            case CouponValidationResult.Valid v -> v.discountAmount();
            case CouponValidationResult.Expired e -> {
                log.warn("만료된 쿠폰 적용 시도 - code={}, expiredAt={}", couponCode, e.expiredAt());
                throw new IllegalStateException("만료된 쿠폰입니다: " + couponCode);
            }
            case CouponValidationResult.NotApplicable n -> {
                log.warn("적용 불가 쿠폰 - code={}, reason={}", couponCode, n.reason());
                throw new IllegalStateException("쿠폰 적용 불가: " + n.reason());
            }
            case CouponValidationResult.UsageLimitReached l -> {
                log.warn("사용 한도 초과 쿠폰 - code={}, limit={}", couponCode, l.limit());
                throw new IllegalStateException("쿠폰 사용 한도 초과: " + l.current() + "/" + l.limit());
            }
            case CouponValidationResult.AlreadyUsed a -> {
                log.warn("이미 사용된 쿠폰 - code={}, usedAt={}", couponCode, a.usedAt());
                throw new IllegalStateException("이미 사용된 쿠폰입니다. 사용일시: " + a.usedAt());
            }
        };

        BigDecimal finalAmount = orderAmount.subtract(discountAmount).max(BigDecimal.ZERO);

        // 실제: 쿠폰 사용 내역 기록, 사용 횟수 증가
        recordCouponUsage(couponCode, userId, orderAmount, discountAmount);

        log.info("쿠폰 적용 완료 - code={}, discount={}, final={}", couponCode, discountAmount, finalAmount);
        return new CouponApplicationResult(couponCode, orderAmount, discountAmount, finalAmount, "PERCENT");
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResult validateCoupon(String couponCode, Long userId, BigDecimal orderAmount) {
        log.debug("쿠폰 유효성 검증 - code={}, userId={}, orderAmount={}", couponCode, userId, orderAmount);

        // 실제: 쿠폰 DB 조회 후 각 조건 순서대로 검사
        // 여기서는 mock 결과 반환
        BigDecimal discountAmount = orderAmount.multiply(BigDecimal.valueOf(0.1))
                .setScale(0, RoundingMode.DOWN);
        BigDecimal finalAmount = orderAmount.subtract(discountAmount);
        return new CouponValidationResult.Valid(discountAmount, finalAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDto getCoupon(Long couponId) {
        log.debug("쿠폰 조회 - couponId={}", couponId);
        return buildMockCouponDto(couponId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CouponDto> getUserCoupons(Long userId, Pageable pageable) {
        log.debug("사용자 쿠폰 목록 - userId={}", userId);
        List<CouponDto> coupons = List.of(
                buildMockCouponDto(1L),
                buildMockCouponDto(2L)
        );
        return new PageImpl<>(coupons, pageable, coupons.size());
    }

    @Override
    public int expireCoupons() {
        log.info("만료 쿠폰 처리 시작 - 기준일={}", LocalDate.now());
        // 실제: couponRepository.findAllByEndDateBeforeAndStatusNotExpired(LocalDate.now())
        int expiredCount = 0;
        log.info("만료 쿠폰 처리 완료 - count={}", expiredCount);
        return expiredCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponDto> getAvailableCoupons(Long userId) {
        log.debug("사용 가능 쿠폰 목록 - userId={}", userId);
        return List.of(buildMockCouponDto(1L));
    }

    @Override
    public void revokeCoupon(Long couponId, String reason) {
        log.warn("쿠폰 폐기 - couponId={}, reason={}", couponId, reason);
        // 실제: couponRepository.findById(couponId).revoke(reason)
    }

    @Override
    @Transactional(readOnly = true)
    public CouponStatistics getCouponStatistics(Long couponId) {
        log.debug("쿠폰 통계 - couponId={}", couponId);
        return new CouponStatistics(
                couponId, "SUMMER2026", 500L, 320L,
                new BigDecimal("1600000"), 64.0
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateCouponCreation(String code, String discountType, BigDecimal value,
                                        int maxUsage, LocalDate start, LocalDate end) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("쿠폰 코드는 필수입니다.");
        }
        if (!List.of("FIXED", "PERCENT").contains(discountType)) {
            throw new IllegalArgumentException("할인 유형은 FIXED 또는 PERCENT여야 합니다.");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("할인 값은 0보다 커야 합니다.");
        }
        if ("PERCENT".equals(discountType) && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("퍼센트 할인은 100%를 초과할 수 없습니다.");
        }
        if (maxUsage <= 0) {
            throw new IllegalArgumentException("최대 사용 횟수는 1 이상이어야 합니다.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("유효 시작일이 종료일보다 늦을 수 없습니다.");
        }
    }

    private void checkAlreadyIssued(String couponCode, Long userId) {
        // 실제: couponUsageRepository.existsByCouponCodeAndUserId(couponCode, userId)
        log.debug("중복 발급 확인 - code={}, userId={}", couponCode, userId);
    }

    private void checkIssuanceLimit(String couponCode) {
        // 실제: coupon.getCurrentUsageCount() >= coupon.getMaxUsageCount()
        log.debug("발급 한도 확인 - code={}", couponCode);
    }

    private void recordCouponUsage(String couponCode, Long userId,
                                   BigDecimal orderAmount, BigDecimal discountAmount) {
        log.debug("쿠폰 사용 기록 - code={}, userId={}, discount={}", couponCode, userId, discountAmount);
        // 실제: new CouponUsage(...) → couponUsageRepository.save()
    }

    private CouponDto buildMockCouponDto(Long couponId) {
        return new CouponDto(
                couponId, "SUMMER2026", "PERCENT", new BigDecimal("10"),
                new BigDecimal("50000"), 500, 320,
                LocalDate.now(), LocalDate.now().plusMonths(3),
                "ACTIVE", false, null
        );
    }
}
