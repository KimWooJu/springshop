package com.springshop.web.mapper;

import com.springshop.domain.product.Category;
import com.springshop.web.dto.response.CategoryBreadcrumbResponse;
import com.springshop.web.dto.response.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 카테고리 엔티티 ↔ 응답 DTO 매퍼.
 *
 * <p>카테고리 트리 구조를 재귀적으로 변환하고,
 * 브레드크럼 경로를 생성한다.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Category 엔티티를 응답 DTO로 변환한다.
     *
     * @param category 변환할 카테고리 엔티티
     * @return CategoryResponse DTO
     */
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "productCount", ignore = true)
    CategoryResponse toResponse(Category category);

    /**
     * Category 엔티티 목록을 응답 DTO 목록으로 변환한다.
     */
    List<CategoryResponse> toResponseList(List<Category> categories);

    /**
     * Category 엔티티를 브레드크럼 응답 DTO로 변환한다.
     * 브레드크럼은 루트에서 현재 카테고리까지의 경로를 나타낸다.
     *
     * @param category 변환할 카테고리 엔티티
     * @return CategoryBreadcrumbResponse DTO
     */
    @Mapping(target = "depth", ignore = true)
    @Mapping(target = "path", ignore = true)
    CategoryBreadcrumbResponse toBreadcrumbResponse(Category category);

    /**
     * Category 엔티티 목록을 브레드크럼 응답 DTO 목록으로 변환한다.
     */
    List<CategoryBreadcrumbResponse> toBreadcrumbResponseList(List<Category> categories);

    /**
     * 카테고리 slug를 URL 경로로 변환한다.
     */
    default String buildPath(Category category) {
        if (category == null) return "/";
        return "/" + category.getName().toLowerCase().replace(" ", "-");
    }
}
