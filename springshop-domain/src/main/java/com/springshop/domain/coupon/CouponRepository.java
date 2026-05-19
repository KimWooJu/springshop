package com.springshop.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 쿠폰 Repository.
 *
 * <p>코드 단건 조회, 유효 쿠폰 목록, 만료 임박 쿠폰, 사용 가능한 쿠폰 조회 등을 제공한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * 코드로 단건 조회.
     */
    Optional<Coupon> findByCode(String code);

    /**
     * 활성 + 기간 유효 쿠폰 목록.
     */
    @Query("""
            SELECT c FROM Coupon c
             WHERE c.isActive = true
               AND c.startDate <= :now
               AND c.endDate >= :now
            """)
    List<Coupon> findValidCoupons(@Param("now") LocalDateTime now);

    /**
     * 사용자가 과거에 사용한 쿠폰 목록 (CouponUsage 조인).
     */
    @Query("""
            SELECT DISTINCT c FROM Coupon c, CouponUsage u
             WHERE u.couponId = c.id
               AND u.userId = :userId
               AND u.isCancelled = false
             ORDER BY u.usedAt DESC
            """)
    List<Coupon> findUsedByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 특정 쿠폰 사용 횟수.
     */
    @Query("""
            SELECT COUNT(u) FROM CouponUsage u
             WHERE u.userId = :userId
               AND u.couponId = :couponId
               AND u.isCancelled = false
            """)
    long countUsageByUserIdAndCouponId(@Param("userId") Long userId,
                                       @Param("couponId") Long couponId);

    /**
     * 발급 한도 소진된 쿠폰.
     */
    @Query("""
            SELECT c FROM Coupon c
             WHERE c.totalQuantity IS NOT NULL
               AND c.usedQuantity >= c.totalQuantity
            """)
    List<Coupon> findExhausted();

    /**
     * 만료 임박 쿠폰 (현재 ~ 3일 후 사이 만료).
     */
    @Query("""
            SELECT c FROM Coupon c
             WHERE c.isActive = true
               AND c.endDate BETWEEN :now AND :soon
            """)
    List<Coupon> findExpiringSoon(@Param("now") LocalDateTime now,
                                  @Param("soon") LocalDateTime soon);

    /**
     * 활성 쿠폰 페이징.
     */
    Page<Coupon> findByIsActiveTrueOrderByEndDateAsc(Pageable pageable);

    /**
     * 대상 사용자 유형별 쿠폰.
     */
    @Query("""
            SELECT c FROM Coupon c
             WHERE c.targetUserType = :type
               AND c.isActive = true
            """)
    List<Coupon> findActiveByTargetUserType(@Param("type") Coupon.TargetUserType type);

    /**
     * 쿠폰 검색 (이름/설명 like).
     */
    @Query("""
            SELECT c FROM Coupon c
             WHERE (c.name LIKE %:keyword% OR c.description LIKE %:keyword%)
            """)
    Page<Coupon> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 코드 중복 검사.
     */
    boolean existsByCode(String code);
}
