package com.springshop.web.mapper;

import com.springshop.domain.product.Product;
import com.springshop.web.dto.response.ProductResponse;
import com.springshop.web.dto.response.ProductSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * 상품 엔티티 ↔ 응답 DTO 매퍼.
 *
 * <p>MapStruct를 사용하여 컴파일 타임에 매핑 코드를 생성한다.
 * 엔티티의 복합 필드(status, tags 등)는 {@link Named} 메서드로 변환한다.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    /**
     * Product 엔티티를 상세 응답 DTO로 변환한다.
     *
     * @param product 변환할 상품 엔티티
     * @return ProductResponse DTO
     */
    @Mapping(target = "status", source = "statusLabel")
    @Mapping(target = "price", source = "basePrice")
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "brandName", ignore = true)
    @Mapping(target = "stockQuantity", source = "totalStock")
    ProductResponse toResponse(Product product);

    /**
     * Product 엔티티를 요약 응답 DTO로 변환한다.
     *
     * @param product 변환할 상품 엔티티
     * @return ProductSummaryResponse DTO
     */
    @Mapping(target = "status", source = "statusLabel")
    @Mapping(target = "price", source = "basePrice")
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    ProductSummaryResponse toSummaryResponse(Product product);

    /**
     * Product 엔티티 목록을 상세 응답 DTO 목록으로 변환한다.
     *
     * @param products 변환할 상품 엔티티 목록
     * @return ProductResponse DTO 목록
     */
    List<ProductResponse> toResponseList(List<Product> products);

    /**
     * Product 엔티티 목록을 요약 응답 DTO 목록으로 변환한다.
     *
     * @param products 변환할 상품 엔티티 목록
     * @return ProductSummaryResponse DTO 목록
     */
    List<ProductSummaryResponse> toSummaryResponseList(List<Product> products);

    /**
     * statusLabel을 표준 상태 문자열로 변환한다.
     *
     * @param statusLabel 엔티티의 상태 레이블
     * @return 표준화된 상태 문자열
     */
    @Named("mapStatus")
    default String mapStatus(String statusLabel) {
        if (statusLabel == null) return "UNKNOWN";
        return switch (statusLabel.toUpperCase()) {
            case "DRAFT"       -> "DRAFT";
            case "ACTIVE"      -> "ACTIVE";
            case "INACTIVE"    -> "INACTIVE";
            case "OUT_OF_STOCK" -> "OUT_OF_STOCK";
            case "DISCONTINUED" -> "DISCONTINUED";
            default            -> statusLabel;
        };
    }
}
