package com.springshop.web.controller;

import com.springshop.web.dto.request.CreateProductRequest;
import com.springshop.web.dto.request.ProductSearchRequest;
import com.springshop.web.dto.request.UpdateProductRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.PageResponse;
import com.springshop.web.dto.response.ProductResponse;
import com.springshop.web.dto.response.ProductSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * 상품 API 컨트롤러.
 *
 * <p>상품 CRUD, 검색, 카테고리/브랜드별 조회, 베스트셀러/신상품, 이미지/변종 관리,
 * 통계 등 상품 도메인의 모든 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "상품 API", description = "상품 CRUD/검색/변종/이미지/통계")
public class ProductController {

    private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(10000);

    @Operation(summary = "상품 목록", description = "필터/정렬/페이징을 지원하는 상품 목록 조회.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "필터 형식 오류")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> listProducts(
            @Parameter(description = "페이지(0-base)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 키") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String status) {
        LOG.debug("상품 목록 - page={}, size={}, cat={}, brand={}", page, size, categoryId, brandId);
        List<ProductSummaryResponse> items = buildSampleSummaries(size);
        return ResponseEntity.ok(ApiResponse.Success.of(items,
                "총 " + items.size() + "건 (페이지 " + page + ", 정렬 " + sortBy + " " + sortDir + ")"));
    }

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상세 정보를 조회한다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @Parameter(description = "상품 ID", required = true, example = "10001")
            @PathVariable Long id) {
        LOG.debug("상품 상세 조회 - id={}", id);
        ProductResponse data = buildSampleProduct(id);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "상품 등록", description = "관리자가 신규 상품을 등록한다.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        LOG.info("상품 등록 - name={}", request.name());
        Long newId = ID_SEQ.incrementAndGet();
        ProductResponse data = new ProductResponse(
                newId, request.name(),
                request.slug() == null ? "product-" + newId : request.slug(),
                request.description(),
                request.price(),
                request.salePrice() == null ? request.price() : request.salePrice(),
                computeDiscount(request.price(), request.salePrice()),
                request.currency() == null ? "KRW" : request.currency(),
                "IN_STOCK", computeTotalStock(request),
                request.status() == null ? "DRAFT" : request.status(),
                new ProductResponse.CategoryRef(request.categoryId(), "카테고리", "cat-" + request.categoryId()),
                new ProductResponse.BrandRef(request.brandId(), "브랜드", null),
                List.of(), List.of(), request.tags(),
                0.0, 0L, 0L, 0L,
                LocalDateTime.now(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "상품 등록 완료"));
    }

    @Operation(summary = "상품 수정", description = "기존 상품의 정보를 수정한다.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        LOG.info("상품 수정 - id={}", id);
        ProductResponse data = buildSampleProduct(id);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "수정 완료"));
    }

    @Operation(summary = "상품 삭제", description = "상품을 비활성화(soft delete)한다.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id) {
        LOG.warn("상품 삭제 - id={}", id);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "상품 옵션 변종 목록", description = "특정 상품의 모든 변종을 조회한다.")
    @GetMapping("/{id}/variants")
    public ResponseEntity<ApiResponse<List<ProductResponse.VariantRef>>> getVariants(
            @PathVariable Long id) {
        LOG.debug("변종 조회 - productId={}", id);
        List<ProductResponse.VariantRef> variants = List.of(
                new ProductResponse.VariantRef(id * 10 + 1, "색상", "블랙",
                        "SKU-" + id + "-BK", BigDecimal.ZERO, 50, "ACTIVE"),
                new ProductResponse.VariantRef(id * 10 + 2, "색상", "화이트",
                        "SKU-" + id + "-WH", BigDecimal.ZERO, 30, "ACTIVE")
        );
        return ResponseEntity.ok(ApiResponse.Success.of(variants));
    }

    @Operation(summary = "상품 변종 추가", description = "상품에 새 옵션 변종을 추가한다.")
    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse.VariantRef>> addVariant(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest.VariantRequest request) {
        LOG.info("변종 추가 - productId={}, option={} {}", id, request.optionName(), request.optionValue());
        Long newVariantId = ID_SEQ.incrementAndGet();
        ProductResponse.VariantRef variant = new ProductResponse.VariantRef(
                newVariantId, request.optionName(), request.optionValue(),
                request.sku(), request.additionalPrice(), request.stockQuantity(),
                request.status() == null ? "ACTIVE" : request.status());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(variant, "변종 추가 완료"));
    }

    @Operation(summary = "상품 변종 수정", description = "변종의 추가 금액/재고/상태를 수정한다.")
    @PutMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse.VariantRef>> updateVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @Valid @RequestBody CreateProductRequest.VariantRequest request) {
        LOG.info("변종 수정 - productId={}, variantId={}", id, variantId);
        ProductResponse.VariantRef variant = new ProductResponse.VariantRef(
                variantId, request.optionName(), request.optionValue(),
                request.sku(), request.additionalPrice(), request.stockQuantity(),
                request.status());
        return ResponseEntity.ok(ApiResponse.Success.of(variant, "변종 수정 완료"));
    }

    @Operation(summary = "상품 이미지 추가", description = "상품에 이미지를 등록한다.")
    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse.ImageRef>> addImage(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest.ImageRequest request) {
        LOG.info("이미지 추가 - productId={}, url={}", id, request.url());
        ProductResponse.ImageRef image = new ProductResponse.ImageRef(
                ID_SEQ.incrementAndGet(), request.url(),
                request.altText(), request.displayOrder(), request.isMain());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(image, "이미지 등록 완료"));
    }

    @Operation(summary = "상품 이미지 삭제", description = "특정 상품 이미지를 삭제한다.")
    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId) {
        LOG.warn("이미지 삭제 - productId={}, imageId={}", id, imageId);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "상품 검색", description = "키워드 검색 + 필터 + 가격 범위 + 정렬.")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> searchProducts(
            @Valid @RequestBody ProductSearchRequest request) {
        LOG.debug("상품 검색 - keyword={}", request);
        List<ProductSummaryResponse> items = buildSampleSummaries(10);
        return ResponseEntity.ok(ApiResponse.Success.of(items, "검색 완료"));
    }

    @Operation(summary = "베스트셀러", description = "기간별 누적 판매 상위 상품.")
    @GetMapping("/top-selling")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> topSelling(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "WEEK") String period) {
        LOG.debug("베스트셀러 - limit={}, period={}", limit, period);
        List<ProductSummaryResponse> items = buildSampleSummaries(limit);
        return ResponseEntity.ok(ApiResponse.Success.of(items));
    }

    @Operation(summary = "신상품", description = "등록일 기준 신상품 목록.")
    @GetMapping("/new-arrivals")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> newArrivals(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "30") int withinDays) {
        LOG.debug("신상품 - limit={}, withinDays={}", limit, withinDays);
        List<ProductSummaryResponse> items = buildSampleSummaries(limit);
        return ResponseEntity.ok(ApiResponse.Success.of(items));
    }

    @Operation(summary = "카테고리별 상품", description = "특정 카테고리의 상품 목록.")
    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> byCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LOG.debug("카테고리별 상품 - categoryId={}, page={}", categoryId, page);
        List<ProductSummaryResponse> items = buildSampleSummaries(size);
        return ResponseEntity.ok(ApiResponse.Success.of(items));
    }

    @Operation(summary = "브랜드별 상품", description = "특정 브랜드의 상품 목록.")
    @GetMapping("/brands/{brandId}")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> byBrand(
            @PathVariable Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LOG.debug("브랜드별 상품 - brandId={}, page={}", brandId, page);
        List<ProductSummaryResponse> items = buildSampleSummaries(size);
        return ResponseEntity.ok(ApiResponse.Success.of(items));
    }

    @Operation(summary = "추천 상품", description = "사용자 맞춤 추천 상품.")
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> recommendations(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductSummaryResponse> items = buildSampleSummaries(limit);
        return ResponseEntity.ok(ApiResponse.Success.of(items));
    }

    @Operation(summary = "관련 상품", description = "특정 상품과 관련된 상품들.")
    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> relatedProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") int limit) {
        List<ProductSummaryResponse> items = buildSampleSummaries(limit);
        return ResponseEntity.ok(ApiResponse.Success.of(items));
    }

    @Operation(summary = "상품 통계 (관리자)", description = "상품의 조회수/판매량 통계.")
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> productStats(
            @PathVariable Long id) {
        java.util.Map<String, Object> stats = java.util.Map.of(
                "productId", id,
                "viewCount", 12380L,
                "soldCount", 432L,
                "wishlistCount", 89L,
                "averageRating", 4.5,
                "reviewCount", 128L,
                "conversionRate", 0.034
        );
        return ResponseEntity.ok(ApiResponse.Success.of(stats));
    }

    @Operation(summary = "상품 상태 변경", description = "상품 상태(DRAFT/ACTIVE/...)를 변경.")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> changeStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        LOG.info("상품 상태 변경 - id={}, status={}", id, status);
        ProductResponse data = buildSampleProduct(id);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "상태 변경 완료"));
    }

    @Operation(summary = "페이징 검색 (PageResponse)", description = "Spring Data Page → PageResponse 변환 예시.")
    @GetMapping("/page")
    public ResponseEntity<PageResponse<ProductSummaryResponse>> pageSearch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ProductSummaryResponse> items = buildSampleSummaries(size);
        return ResponseEntity.ok(PageResponse.single(items));
    }

    // ---------------------------------------
    // 헬퍼
    // ---------------------------------------

    private ProductResponse buildSampleProduct(Long id) {
        return new ProductResponse(
                id, "샘플 상품 " + id, "product-" + id,
                "샘플 상품 설명입니다.", new BigDecimal("19900"),
                new BigDecimal("17910"), 10, "KRW",
                "IN_STOCK", 100, "ACTIVE",
                new ProductResponse.CategoryRef(1L, "전자제품", "electronics"),
                new ProductResponse.BrandRef(1L, "샘플브랜드", "https://cdn.example.com/logo.png"),
                List.of(new ProductResponse.ImageRef(1L, "https://cdn.example.com/p1.jpg",
                        "메인", 0, true)),
                List.of(new ProductResponse.VariantRef(1L, "색상", "블랙",
                        "SKU-BK", BigDecimal.ZERO, 50, "ACTIVE")),
                List.of("신상품", "베스트"),
                4.7, 234L, 12380L, 850L,
                LocalDateTime.now().minusDays(30), LocalDateTime.now()
        );
    }

    private List<ProductSummaryResponse> buildSampleSummaries(int size) {
        List<ProductSummaryResponse> items = new ArrayList<>(size);
        IntStream.range(0, size).forEach(i -> {
            items.add(new ProductSummaryResponse(
                    (long) (10000 + i), "상품 " + i, "product-" + i,
                    new BigDecimal("19900"), new BigDecimal("17910"),
                    10, "https://cdn.example.com/p" + i + ".jpg",
                    "샘플브랜드", "전자제품",
                    4.5, 123L, 234L, true, i < 3, i < 2, true,
                    List.of("신상품"), LocalDateTime.now().minusDays(i)));
        });
        return items;
    }

    private int computeDiscount(BigDecimal price, BigDecimal salePrice) {
        if (price == null || salePrice == null || price.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return price.subtract(salePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(price, java.math.RoundingMode.DOWN)
                .intValue();
    }

    private int computeTotalStock(CreateProductRequest request) {
        if (request.variants() == null || request.variants().isEmpty()) return 0;
        return request.variants().stream()
                .mapToInt(CreateProductRequest.VariantRequest::stockQuantity).sum();
    }
}
