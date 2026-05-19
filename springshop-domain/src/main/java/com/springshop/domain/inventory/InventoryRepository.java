package com.springshop.domain.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 재고 Repository.
 *
 * <p>상품/옵션/창고 단위 조회 및 잠금 조회를 제공한다. 결제 시 잠금 조회와 함께
 * 예약을 수행하여 race condition 을 방지한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 상품 ID로 모든 옵션/창고의 재고 조회.
     */
    List<Inventory> findByProductId(Long productId);

    /**
     * 옵션 ID로 조회.
     */
    List<Inventory> findByVariantId(Long variantId);

    /**
     * 상품 + 옵션 조합으로 모든 창고 재고 조회.
     */
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.variantId = :variantId")
    List<Inventory> findByProductIdAndVariantId(@Param("productId") Long productId,
                                                @Param("variantId") Long variantId);

    /**
     * 단일 창고 + 단일 상품/옵션. 일반적으로 유일 조회.
     */
    @Query("""
            SELECT i FROM Inventory i
             WHERE i.productId = :productId
               AND (i.variantId = :variantId OR (:variantId IS NULL AND i.variantId IS NULL))
               AND i.warehouseId = :warehouseId
            """)
    Optional<Inventory> findUnique(@Param("productId") Long productId,
                                   @Param("variantId") Long variantId,
                                   @Param("warehouseId") Long warehouseId);

    /**
     * 가용 수량이 임계치 이하인 재고들.
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= :threshold")
    List<Inventory> findLowStock(@Param("threshold") int threshold);

    /**
     * 창고별 페이징 조회.
     */
    Page<Inventory> findByWarehouseId(Long warehouseId, Pageable pageable);

    /**
     * 낙관적 락 강제 증가 잠금으로 조회.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findWithLockById(@Param("id") Long id);

    /**
     * 비관적 쓰기 잠금. 결제 시 안전한 예약을 위해 사용.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.id = :id")
    Optional<Inventory> findWithPessimisticLockById(@Param("id") Long id);

    /**
     * 상품 전체 가용 수량 합계.
     */
    @Query("SELECT COALESCE(SUM(i.availableQuantity), 0) FROM Inventory i WHERE i.productId = :productId")
    long sumAvailableByProductId(@Param("productId") Long productId);

    /**
     * 상품 전체 총 수량 합계.
     */
    @Query("SELECT COALESCE(SUM(i.totalQuantity), 0) FROM Inventory i WHERE i.productId = :productId")
    long sumTotalByProductId(@Param("productId") Long productId);

    /**
     * 임박 재고(가용 5개 이하).
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= 5")
    List<Inventory> findCriticalStock();

    /**
     * 품절 재고.
     */
    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity <= 0")
    List<Inventory> findOutOfStock(Pageable pageable);

    /**
     * 상품 + 창고 조합 카운트. 등록 여부 확인.
     */
    long countByProductIdAndWarehouseId(Long productId, Long warehouseId);

    /**
     * 특정 창고의 활성 재고 수.
     */
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.warehouseId = :warehouseId AND i.totalQuantity > 0")
    long countActiveByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * 예약 잔여 재고 합계 (전체 시스템).
     */
    @Query("SELECT COALESCE(SUM(i.reservedQuantity), 0) FROM Inventory i")
    long sumAllReserved();
}
