package com.springshop.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 브랜드 Repository.
 *
 * <p>활성 브랜드 조회, 이름 검색, 프리미엄 브랜드 필터링, 국가별 분류 등을 지원한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /**
     * 활성 브랜드 목록.
     */
    List<Brand> findByIsActive(boolean isActive);

    /**
     * 활성 브랜드 페이징.
     */
    @Query("SELECT b FROM Brand b WHERE b.isActive = true ORDER BY b.name")
    Page<Brand> findActiveBrands(Pageable pageable);

    /**
     * 이름 포함 검색.
     */
    @Query("SELECT b FROM Brand b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Brand> findByNameContaining(@Param("keyword") String keyword);

    /**
     * 이름으로 단건 조회.
     */
    Optional<Brand> findByName(String name);

    /**
     * 이름 중복 검사.
     */
    boolean existsByName(String name);

    /**
     * 프리미엄 브랜드 조회.
     */
    @Query("SELECT b FROM Brand b WHERE b.isPremium = true AND b.isActive = true")
    List<Brand> findPremiumBrands();

    /**
     * 국가별 브랜드.
     */
    List<Brand> findByCountry(String country);

    /**
     * 상품 수가 가장 많은 브랜드.
     */
    @Query("SELECT b FROM Brand b WHERE b.isActive = true ORDER BY b.productCount DESC")
    List<Brand> findTopByProductCount(Pageable pageable);
}
