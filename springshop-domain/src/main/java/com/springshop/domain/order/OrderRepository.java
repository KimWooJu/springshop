package com.springshop.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 Repository.
 *
 * <p>사용자별 주문 조회, 상태별 조회, 기간 조회, EntityGraph를 통한 항목 즉시 로딩 등을 지원한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 번호로 조회.
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 사용자 주문 목록(최신순).
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자 주문 모두.
     */
    List<Order> findByUserId(Long userId);

    /**
     * 상태별 조회.
     */
    @Query("SELECT o FROM Order o WHERE o.statusLabel = :status")
    Page<Order> findByStatus(@Param("status") String statusLabel, Pageable pageable);

    /**
     * 사용자 + 상태.
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.statusLabel = :status")
    Page<Order> findByUserIdAndStatus(@Param("userId") Long userId,
                                       @Param("status") String statusLabel,
                                       Pageable pageable);

    /**
     * 기간 검색.
     */
    @Query("""
            SELECT o FROM Order o
            WHERE o.createdAt BETWEEN :from AND :to
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findByDateRange(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                Pageable pageable);

    /**
     * 사용자 주문 수.
     */
    long countByUserId(Long userId);

    /**
     * 사용자 + 상태 카운트.
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.statusLabel = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * EntityGraph로 items 즉시 로딩.
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findWithItemsById(@Param("orderId") Long orderId);

    /**
     * 발송 가능한 주문(확정 상태).
     */
    @Query("SELECT o FROM Order o WHERE o.statusLabel = 'CONFIRMED' ORDER BY o.createdAt ASC")
    List<Order> findShippable(Pageable pageable);

    /**
     * 만료된 결제 대기 주문 (자동 취소 후보).
     */
    @Query("SELECT o FROM Order o WHERE o.statusLabel = 'PAYMENT_PENDING' AND o.statusChangedAt < :threshold")
    List<Order> findExpiredPaymentPending(@Param("threshold") LocalDateTime threshold);
}
