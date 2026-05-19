package com.springshop.domain.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 리뷰 Repository.
 *
 * <p>상품/사용자/상태별 조회 및 평균 별점·별점 분포 계산을 지원한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 상품 + 상태 별 페이징.
     */
    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.statusLabel = :status")
    Page<Review> findByProductIdAndStatus(@Param("productId") Long productId,
                                          @Param("status") String statusLabel,
                                          Pageable pageable);

    /**
     * 사용자 작성 리뷰.
     */
    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 상태별 페이징.
     */
    @Query("SELECT r FROM Review r WHERE r.statusLabel = :status ORDER BY r.createdAt DESC")
    Page<Review> findByStatus(@Param("status") String statusLabel, Pageable pageable);

    /**
     * 상품 평균 별점 (승인된 리뷰만 대상).
     */
    @Query("""
            SELECT AVG(r.rating)
              FROM Review r
             WHERE r.productId = :productId
               AND r.statusLabel = 'APPROVED'
            """)
    Optional<Double> calculateAverageRating(@Param("productId") Long productId);

    /**
     * 상품 리뷰 수 (전체).
     */
    long countByProductId(Long productId);

    /**
     * 상품 + 별점별 카운트.
     */
    @Query("""
            SELECT COUNT(r) FROM Review r
             WHERE r.productId = :productId
               AND r.rating = :rating
               AND r.statusLabel = 'APPROVED'
            """)
    long countByProductIdAndRating(@Param("productId") Long productId, @Param("rating") int rating);

    /**
     * 검증 구매 리뷰만 조회.
     */
    @Query("""
            SELECT r FROM Review r
             WHERE r.productId = :productId
               AND r.statusLabel = 'APPROVED'
               AND r.isVerifiedPurchase = true
             ORDER BY r.createdAt DESC
            """)
    Page<Review> findVerifiedPurchaseByProductId(@Param("productId") Long productId, Pageable pageable);

    /**
     * 가장 오래된 PENDING 리뷰부터 검토.
     */
    @Query("SELECT r FROM Review r WHERE r.statusLabel = 'PENDING' ORDER BY r.createdAt ASC")
    Page<Review> findPendingOrderByOldest(Pageable pageable);

    /**
     * 중복 리뷰 방지: 사용자가 같은 주문/상품 조합으로 리뷰를 작성했는지 검사.
     */
    boolean existsByUserIdAndOrderIdAndProductId(Long userId, Long orderId, Long productId);

    /**
     * 별점 분포 (별점별 카운트).
     *
     * <p>각 행: [rating(int), count(long)]</p>
     */
    @Query("""
            SELECT r.rating, COUNT(r)
              FROM Review r
             WHERE r.productId = :productId
               AND r.statusLabel = 'APPROVED'
             GROUP BY r.rating
             ORDER BY r.rating DESC
            """)
    List<Object[]> getRatingDistribution(@Param("productId") Long productId);

    /**
     * 이미지 즉시 로딩 조회.
     */
    @EntityGraph(attributePaths = {"images"})
    @Query("SELECT r FROM Review r WHERE r.id = :reviewId")
    Optional<Review> findWithImagesById(@Param("reviewId") Long reviewId);

    /**
     * 신고 누적된 리뷰 조회 (관리자 대시보드용).
     */
    @Query("SELECT r FROM Review r WHERE r.reportCount >= :threshold ORDER BY r.reportCount DESC")
    Page<Review> findReported(@Param("threshold") int threshold, Pageable pageable);

    /**
     * 베스트 리뷰 (도움됨이 가장 많은 순).
     */
    @Query("""
            SELECT r FROM Review r
             WHERE r.productId = :productId
               AND r.statusLabel = 'APPROVED'
             ORDER BY r.helpfulCount DESC
            """)
    Page<Review> findBestByProductId(@Param("productId") Long productId, Pageable pageable);

    /**
     * 사용자별 작성 리뷰 수.
     */
    long countByUserId(Long userId);
}
