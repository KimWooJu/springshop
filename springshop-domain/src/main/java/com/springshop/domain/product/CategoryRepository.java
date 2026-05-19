package com.springshop.domain.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 Repository.
 *
 * <p>루트 카테고리, 특정 부모의 자식 카테고리, 슬러그 기반 조회, 깊이별 조회 등을 제공한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 루트 카테고리 목록(부모 없음).
     */
    @Query("SELECT c FROM Category c WHERE c.parentId IS NULL AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findRootCategories();

    /**
     * 특정 부모의 직속 자식 카테고리.
     */
    @Query("SELECT c FROM Category c WHERE c.parentId = :parentId AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findByParentId(@Param("parentId") Long parentId);

    /**
     * 슬러그 기반 단건 조회.
     */
    Optional<Category> findBySlug(String slug);

    /**
     * 특정 깊이의 카테고리 모두.
     */
    @Query("SELECT c FROM Category c WHERE c.level = :level AND c.isActive = true ORDER BY c.displayOrder")
    List<Category> findByLevel(@Param("level") int level);

    /**
     * 경로 prefix 기반 검색(특정 카테고리의 모든 하위).
     */
    @Query("SELECT c FROM Category c WHERE c.fullPath LIKE CONCAT(:pathPrefix, '%') AND c.isActive = true")
    List<Category> findDescendants(@Param("pathPrefix") String pathPrefix);

    /**
     * 이름 검색.
     */
    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword% AND c.isActive = true")
    List<Category> searchByName(@Param("keyword") String keyword);

    /**
     * 슬러그 중복 검사.
     */
    boolean existsBySlug(String slug);

    /**
     * 부모 카테고리가 가진 자식 수.
     */
    @Query("SELECT COUNT(c) FROM Category c WHERE c.parentId = :parentId")
    long countByParentId(@Param("parentId") Long parentId);
}
