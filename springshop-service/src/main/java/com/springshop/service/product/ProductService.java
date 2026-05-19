package com.springshop.service.product;

import com.springshop.domain.product.Product;
import com.springshop.domain.product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 상품 핵심 서비스.
 *
 * <p>등록/수정/삭제/상태 전이/조회/통계 등 카탈로그 관련 유스케이스를
 * 모두 노출한다.
 */
public interface ProductService {

    Product createProduct(ProductCreateCommand command);

    Product updateProduct(Long productId, ProductUpdateCommand command);

    void deleteProduct(Long productId, String requester);

    void softDeleteProduct(Long productId, String requester);

    Product publish(Long productId);

    Product unpublish(Long productId, String reason);

    Product markOutOfStock(Long productId);

    Product markInStock(Long productId);

    Product discontinue(Long productId, String reason);

    Product findById(Long productId);

    Optional<Product> findOptionalById(Long productId);

    Product findBySku(String sku);

    /** 조회 시 조회수를 증가시킨다 (Redis HINCRBY). */
    Product findByIdAndCountView(Long productId);

    List<Product> findAllByIds(List<Long> ids);

    Page<Product> listByCategory(Long categoryId, Pageable pageable);

    Page<Product> listByBrand(Long brandId, Pageable pageable);

    Page<Product> listByStatus(ProductStatus status, Pageable pageable);

    Page<Product> listAll(Pageable pageable);

    /** 새로 등록된 상품 N개 */
    List<Product> findRecentlyAdded(int limit);

    /** 가장 많이 본 상품 N개 */
    List<Product> findMostViewed(int limit);

    /** 베스트셀러 N개 (최근 30일) */
    List<Product> findBestSellers(int limit, int days);

    /** 가격 변경 */
    Product changePrice(Long productId, BigDecimal newPrice, String reason);

    /** 할인 적용/해제 */
    Product applyDiscount(Long productId, BigDecimal discountedPrice, LocalDateTime untilWhen);

    Product removeDiscount(Long productId);

    /** 재고 정보 갱신 (창고 통합 등) */
    Product syncStockQuantity(Long productId, long quantity);

    /** 노출 순서 변경 (관리자) */
    void reorderDisplayPriority(List<DisplayPriorityCommand> commands);

    /** SKU 중복 여부 */
    boolean isSkuTaken(String sku);

    /** 상품 사본 생성 (옵션 다른 신규 등록 용도) */
    Product duplicate(Long productId, String newSku, String newName);

    /** 통계 요약 */
    ProductStatsSummary getStatsSummary();

    /** 검색 (간단 키워드) */
    Page<Product> simpleSearch(String keyword, Pageable pageable);

    /** 카테고리 변경 */
    Product changeCategory(Long productId, Long newCategoryId);

    /** 브랜드 변경 */
    Product changeBrand(Long productId, Long newBrandId);

    /** 신상품 마크/해제 */
    Product markAsNew(Long productId, boolean newProduct);

    /** 추천 상품 마크/해제 */
    Product markAsRecommended(Long productId, boolean recommended);

    /** 상품 상세 + 관련 추천 상품 (동일 카테고리 상위) 동시 조회 */
    ProductDetailWithRecommendations findDetailWithRecommendations(Long productId, int recommendLimit);

    record ProductCreateCommand(
        String sku,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        Long brandId,
        long initialStock,
        String mainImageUrl,
        List<String> tags
    ) {}

    record ProductUpdateCommand(
        String name,
        String description,
        BigDecimal price,
        String mainImageUrl,
        List<String> tags
    ) {}

    record DisplayPriorityCommand(Long productId, int displayOrder) {}

    record ProductStatsSummary(
        long total,
        long onSale,
        long outOfStock,
        long discontinued,
        long newToday,
        BigDecimal averagePrice
    ) {}

    record ProductDetailWithRecommendations(
        Product product,
        List<Product> recommendations
    ) {}
}
