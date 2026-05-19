package com.springshop.domain.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 결제 Repository.
 *
 * <p>결제 단건 조회, 사용자/주문/상태별 페이징 조회, 결제 통계 집계 메서드들을 제공한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 요청 ID로 단건 조회.
     */
    Optional<Payment> findByRequestId(String requestId);

    /**
     * 주문별 결제 시도 이력.
     */
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 주문별 최근 결제.
     */
    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * 주문 + 상태 단건 조회. 완료된 결제 찾기에 활용.
     */
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId AND p.statusLabel = :status")
    Optional<Payment> findByOrderIdAndStatus(@Param("orderId") Long orderId,
                                             @Param("status") String statusLabel);

    /**
     * 사용자별 결제 페이징.
     */
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 상태별 페이징.
     */
    @Query("SELECT p FROM Payment p WHERE p.statusLabel = :status ORDER BY p.createdAt DESC")
    Page<Payment> findByStatus(@Param("status") String statusLabel, Pageable pageable);

    /**
     * 결제 완료 + 기간 검색.
     */
    @Query("""
            SELECT p FROM Payment p
             WHERE (p.statusLabel = 'COMPLETED' OR p.statusLabel = 'PARTIALLY_REFUNDED' OR p.statusLabel = 'FULLY_REFUNDED')
               AND p.paidAt BETWEEN :from AND :to
             ORDER BY p.paidAt DESC
            """)
    List<Payment> findCompletedByDateRange(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    /**
     * 일자별 결제 총액 (취소/실패 제외).
     */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
             WHERE p.paidAt BETWEEN :from AND :to
               AND (p.statusLabel = 'COMPLETED' OR p.statusLabel = 'PARTIALLY_REFUNDED' OR p.statusLabel = 'FULLY_REFUNDED')
            """)
    BigDecimal sumCompletedAmountByDateRange(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    /**
     * 환불 누계 합.
     */
    @Query("""
            SELECT COALESCE(SUM(p.refundedAmount), 0) FROM Payment p
             WHERE p.paidAt BETWEEN :from AND :to
            """)
    BigDecimal sumRefundedAmountByDateRange(@Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    /**
     * 주문에 지정 상태 결제가 존재하는지.
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.orderId = :orderId AND p.statusLabel = :status")
    boolean existsByOrderIdAndStatus(@Param("orderId") Long orderId,
                                     @Param("status") String statusLabel);

    /**
     * 만료된 PENDING 결제 (자동 취소 대상).
     */
    @Query("""
            SELECT p FROM Payment p
             WHERE p.statusLabel = 'PENDING'
               AND p.statusChangedAt < :threshold
            """)
    List<Payment> findExpiredPending(@Param("threshold") LocalDateTime threshold);

    /**
     * PG 트랜잭션 ID로 조회.
     */
    Optional<Payment> findByPgTransactionId(String pgTransactionId);

    /**
     * 사용자 + 상태 카운트.
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.userId = :userId AND p.statusLabel = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String statusLabel);
}
