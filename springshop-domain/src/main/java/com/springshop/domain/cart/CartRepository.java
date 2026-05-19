package com.springshop.domain.cart;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 장바구니 Repository.
 *
 * <p>사용자 단위로 활성 장바구니를 조회·관리한다. 만료된(오래되고 결제되지 않은)
 * 장바구니의 일괄 비활성화 메서드도 제공한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * 사용자의 활성 장바구니 조회. 사용자당 1개만 존재해야 한다.
     */
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.active = true")
    Optional<Cart> findActiveByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 모든 장바구니(과거 포함) 조회.
     */
    List<Cart> findByUserId(Long userId);

    /**
     * 사용자의 모든 장바구니 페이징 조회.
     */
    Page<Cart> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자의 활성 장바구니 존재 여부.
     */
    @Query("SELECT COUNT(c) > 0 FROM Cart c WHERE c.userId = :userId AND c.active = true")
    boolean existsActiveByUserId(@Param("userId") Long userId);

    /**
     * 특정 장바구니의 항목 개수.
     */
    @Query("SELECT COUNT(i) FROM Cart c JOIN c.items i WHERE c.id = :cartId")
    long countItemsByCartId(@Param("cartId") Long cartId);

    /**
     * EntityGraph로 항목 즉시 로딩.
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT c FROM Cart c WHERE c.id = :cartId")
    Optional<Cart> findWithItemsById(@Param("cartId") Long cartId);

    /**
     * 사용자별 항목 포함 활성 장바구니 조회.
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.active = true")
    Optional<Cart> findActiveWithItemsByUserId(@Param("userId") Long userId);

    /**
     * 오래된 활성 장바구니 일괄 비활성화. 보통 배치로 실행된다.
     */
    @Modifying
    @Query("""
            UPDATE Cart c
               SET c.active = false,
                   c.deactivatedAt = CURRENT_TIMESTAMP,
                   c.lastModifiedAction = 'EXPIRED'
             WHERE c.active = true
               AND c.updatedAt < :threshold
            """)
    int deactivateStaleCarts(@Param("threshold") LocalDateTime threshold);

    /**
     * 특정 상품을 담은 장바구니 수. 인기/관심 지표로 활용 가능.
     */
    @Query("""
            SELECT COUNT(DISTINCT c.id)
              FROM Cart c JOIN c.items i
             WHERE i.productId = :productId
               AND c.active = true
            """)
    long countActiveCartsContainingProduct(@Param("productId") Long productId);

    /**
     * 활성 장바구니 중 항목이 0개인 빈 장바구니.
     */
    @Query("""
            SELECT c FROM Cart c
             WHERE c.active = true
               AND SIZE(c.items) = 0
            """)
    List<Cart> findEmptyActiveCarts(Pageable pageable);

    /**
     * 사용자 단위 일괄 비활성화. 회원 탈퇴 시 사용.
     */
    @Modifying
    @Query("""
            UPDATE Cart c
               SET c.active = false,
                   c.deactivatedAt = CURRENT_TIMESTAMP,
                   c.lastModifiedAction = 'USER_WITHDRAWN'
             WHERE c.userId = :userId
               AND c.active = true
            """)
    int deactivateAllByUserId(@Param("userId") Long userId);

    /**
     * 활성 상태인 장바구니 총 수. 운영 대시보드용.
     */
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.active = true")
    long countActiveCarts();
}
