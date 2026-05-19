package com.springshop.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 Repository.
 *
 * <p>카테고리/브랜드 별 조회, 상태 기반 조회, 키워드 검색, 가격 범위 검색, 인기 순위 등을 제공한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    List<Product> findByBrandId(Long brandId);

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.statusLabel = :status AND p.deleted = false")
    Page<Product> findByStatus(@Param("status") String statusLabel, Pageable pageable);

    /**
     * 상품명 또는 설명 키워드 검색.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.deleted = false
              AND (p.name LIKE %:keyword% OR p.description LIKE %:keyword%)
              AND p.statusLabel IN ('ACTIVE', 'OUT_OF_STOCK')
            """)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 조회수 기준 상위 N개.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.statusLabel = 'ACTIVE'
            ORDER BY p.viewCount DESC
            """)
    List<Product> findTopByViewCount(Pageable pageable);

    /**
     * 판매량 기준 상위 N개.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.statusLabel = 'ACTIVE'
            ORDER BY p.soldCount DESC
            """)
    List<Product> findTopBySoldCount(Pageable pageable);

    /**
     * 가격 범위 검색.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.basePrice BETWEEN :min AND :max
              AND p.statusLabel = 'ACTIVE'
            """)
    Page<Product> findByPriceBetween(@Param("min") BigDecimal min,
                                     @Param("max") BigDecimal max,
                                     Pageable pageable);

    /**
     * 평점 기준 인기 상품.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.statusLabel = 'ACTIVE'
              AND p.reviewCount >= :minReviewCount
            ORDER BY p.rating DESC, p.reviewCount DESC
            """)
    Page<Product> findHighlyRated(@Param("minReviewCount") int minReviews, Pageable pageable);

    /**
     * 태그를 포함하는 상품.
     */
    @Query("""
            SELECT DISTINCT p FROM Product p
            JOIN p.tags t
            WHERE t = :tag AND p.statusLabel = 'ACTIVE'
            """)
    Page<Product> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * 카테고리 별 상품 개수.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.categoryId = :categoryId AND p.deleted = false")
    long countByCategoryId(@Param("categoryId") Long categoryId);
}
