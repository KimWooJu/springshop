package com.springshop.web.controller;

import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.CategoryBreadcrumbResponse;
import com.springshop.web.dto.response.CategoryResponse;
import com.springshop.web.dto.response.ProductSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

/**
 * 카테고리 API 컨트롤러.
 *
 * <p>카테고리 트리 조회, 단건 조회, 자식 조회, 빵부스러기, 카테고리별 상품 목록,
 * 관리자용 CRUD 등 카테고리 도메인의 모든 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "카테고리 API", description = "카테고리 트리/CRUD/빵부스러기")
public class CategoryController {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(300);

    @Operation(summary = "루트 카테고리 목록", description = "최상위 레벨 카테고리만 반환한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listRoots() {
        LOG.debug("루트 카테고리 조회");
        List<CategoryResponse> roots = List.of(
                CategoryResponse.leaf(1L, "전자제품", "electronics", 0),
                CategoryResponse.leaf(2L, "패션", "fashion", 0),
                CategoryResponse.leaf(3L, "식품", "food", 0),
                CategoryResponse.leaf(4L, "생활용품", "lifestyle", 0)
        );
        return ResponseEntity.ok(ApiResponse.Success.of(roots));
    }

    @Operation(summary = "카테고리 트리 전체", description = "재귀적 자식 포함 카테고리 트리 전체를 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "트리 응답")
    })
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> tree() {
        LOG.debug("카테고리 트리 조회");
        CategoryResponse electronics = CategoryResponse.branch(1L, "전자제품", "electronics", 0,
                List.of(
                        CategoryResponse.branch(11L, "노트북", "laptops", 1,
                                List.of(
                                        CategoryResponse.leaf(111L, "게이밍 노트북", "gaming-laptops", 2),
                                        CategoryResponse.leaf(112L, "비즈니스 노트북", "business-laptops", 2)
                                )),
                        CategoryResponse.leaf(12L, "스마트폰", "smartphones", 1),
                        CategoryResponse.leaf(13L, "태블릿", "tablets", 1)
                ));
        CategoryResponse fashion = CategoryResponse.branch(2L, "패션", "fashion", 0,
                List.of(
                        CategoryResponse.leaf(21L, "여성 의류", "womens", 1),
                        CategoryResponse.leaf(22L, "남성 의류", "mens", 1)
                ));
        return ResponseEntity.ok(ApiResponse.Success.of(List.of(electronics, fashion)));
    }

    @Operation(summary = "카테고리 상세", description = "카테고리 ID로 단건 조회.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @Parameter(description = "카테고리 ID", required = true)
            @PathVariable Long id) {
        LOG.debug("카테고리 상세 - id={}", id);
        CategoryResponse data = new CategoryResponse(
                id, "카테고리 " + id, "category-" + id,
                "카테고리 설명", "https://cdn.example.com/cat-" + id + ".png",
                1, 0, null, List.of(), 1283L, true);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "직속 자식 카테고리", description = "지정 카테고리의 1단계 자식들만 반환한다.")
    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> children(
            @PathVariable Long id) {
        LOG.debug("자식 카테고리 - parentId={}", id);
        List<CategoryResponse> children = List.of(
                CategoryResponse.leaf(id * 10 + 1, "하위 카테고리 1", "child-1", 1),
                CategoryResponse.leaf(id * 10 + 2, "하위 카테고리 2", "child-2", 1),
                CategoryResponse.leaf(id * 10 + 3, "하위 카테고리 3", "child-3", 1)
        );
        return ResponseEntity.ok(ApiResponse.Success.of(children));
    }

    @Operation(summary = "카테고리별 상품", description = "해당 카테고리(+하위) 상품 목록을 조회한다.")
    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> productsByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean includeSubcategories) {
        LOG.debug("카테고리 상품 조회 - categoryId={}, includeSub={}", id, includeSubcategories);
        List<ProductSummaryResponse> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(new ProductSummaryResponse(
                    (long) (id * 1000 + i),
                    "카테고리" + id + " 상품 " + i, "cat-" + id + "-product-" + i,
                    new BigDecimal("29900"), new BigDecimal("23900"),
                    20, "https://cdn.example.com/img" + i + ".jpg",
                    "브랜드", "카테고리 " + id, 4.6, 89L, 234L,
                    true, false, true, true, List.of("인기"),
                    LocalDateTime.now().minusDays(i)));
        }
        return ResponseEntity.ok(ApiResponse.Success.of(items, "카테고리 " + id + " 상품"));
    }

    @Operation(summary = "빵부스러기(breadcrumb)", description = "특정 카테고리의 부모 경로를 반환한다.")
    @GetMapping("/breadcrumb/{id}")
    public ResponseEntity<ApiResponse<CategoryBreadcrumbResponse>> breadcrumb(
            @PathVariable Long id) {
        LOG.debug("빵부스러기 - currentId={}", id);
        List<CategoryBreadcrumbResponse.CategoryItem> path = List.of(
                CategoryBreadcrumbResponse.CategoryItem.of(1L, "전자제품", "electronics", 0),
                CategoryBreadcrumbResponse.CategoryItem.of(11L, "노트북", "laptops", 1),
                CategoryBreadcrumbResponse.CategoryItem.currentOf(id, "현재 카테고리", "current", 2)
        );
        return ResponseEntity.ok(ApiResponse.Success.of(CategoryBreadcrumbResponse.of(id, path)));
    }

    @Operation(summary = "카테고리 등록 (관리자)", description = "신규 카테고리를 등록한다.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @RequestBody CreateCategoryRequest request) {
        LOG.info("카테고리 등록 - name={}", request.name());
        Long newId = ID_SEQ.incrementAndGet();
        CategoryResponse data = new CategoryResponse(
                newId, request.name(), request.slug(),
                request.description(), request.iconUrl(),
                request.parentId() == null ? 0 : 1, 0,
                request.parentId(), List.of(), 0L, true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "카테고리 등록 완료"));
    }

    @Operation(summary = "카테고리 수정 (관리자)", description = "카테고리 정보를 수정한다.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CreateCategoryRequest request) {
        LOG.info("카테고리 수정 - id={}", id);
        CategoryResponse data = new CategoryResponse(
                id, request.name(), request.slug(),
                request.description(), request.iconUrl(),
                1, 0, request.parentId(), List.of(), 0L, true);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "수정 완료"));
    }

    @Operation(summary = "카테고리 삭제 (관리자)", description = "카테고리를 비활성화한다.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id) {
        LOG.warn("카테고리 삭제 - id={}", id);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "카테고리 정렬 순서 변경 (관리자)")
    @PutMapping("/{id}/order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorder(
            @PathVariable Long id,
            @RequestParam int displayOrder) {
        LOG.info("카테고리 순서 변경 - id={}, order={}", id, displayOrder);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "카테고리 검색", description = "이름으로 카테고리를 검색한다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> searchCategories(
            @RequestParam String keyword) {
        LOG.debug("카테고리 검색 - keyword={}", keyword);
        List<CategoryResponse> results = List.of(
                CategoryResponse.leaf(1L, "전자제품", "electronics", 0),
                CategoryResponse.leaf(11L, "노트북", "laptops", 1)
        );
        return ResponseEntity.ok(ApiResponse.Success.of(results));
    }

    /** 간이 카테고리 등록/수정 요청 (인라인 record) */
    public record CreateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 100) String slug,
            @Size(max = 500) String description,
            @Size(max = 500) String iconUrl,
            Long parentId,
            int displayOrder) {
    }
}
