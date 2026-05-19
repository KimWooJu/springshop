package com.springshop.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 쿠폰 사용 이력 Repository.
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /**
     * 쿠폰 + 사용자 사용 이력.
     */
    List<CouponUsage> findByCouponIdAndUserId(Long couponId, Long userId);

    /**
     * 쿠폰별 페이징.
     */
    Page<CouponUsage> findByCouponIdOrderByUsedAtDesc(Long couponId, Pageable pageable);

    /**
     * 사용자별 페이징.
     */
    Page<CouponUsage> findByUserIdOrderByUsedAtDesc(Long userId, Pageable pageable);

    /**
     * 주문에서 사용된 쿠폰 이력.
     */
    List<CouponUsage> findByOrderId(Long orderId);

    /**
     * 사용 + 사용자 + 쿠폰의 조합 카운트 (활성 사용 기준).
     */
    @Query("""
            SELECT COUNT(u) FROM CouponUsage u
             WHERE u.couponId = :couponId
               AND u.userId = :userId
               AND u.isCancelled = false
            """)
    long countByCouponIdAndUserId(@Param("couponId") Long couponId,
                                  @Param("userId") Long userId);

    /**
     * 중복 사용 확인.
     */
    boolean existsByCouponIdAndUserIdAndOrderId(Long couponId, Long userId, Long orderId);

    /**
     * 쿠폰별 누적 할인 금액.
     */
    @Query("""
            SELECT COALESCE(SUM(u.discountAmount), 0) FROM CouponUsage u
             WHERE u.couponId = :couponId
               AND u.isCancelled = false
            """)
    BigDecimal sumDiscountByCouponId(@Param("couponId") Long couponId);

    /**
     * 사용자별 누적 할인.
     */
    @Query("""
            SELECT COALESCE(SUM(u.discountAmount), 0) FROM CouponUsage u
             WHERE u.userId = :userId
               AND u.isCancelled = false
            """)
    BigDecimal sumDiscountByUserId(@Param("userId") Long userId);
}
