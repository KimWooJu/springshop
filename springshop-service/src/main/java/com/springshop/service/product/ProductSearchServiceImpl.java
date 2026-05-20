package com.springshop.service.product;

import com.springshop.domain.product.Product;
import com.springshop.domain.product.ProductRepository;
import com.springshop.domain.product.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * 상품 검색 보조 구현체.
 *
 * <p>표준 검색은 {@link ProductSearchService.Impl}에서 처리하며, 본 구현체는 다음을 담당한다:
 * <ul>
 *   <li>복합 필터 검색 — 키워드, 카테고리, 브랜드, 가격, 태그, 재고, 할인</li>
 *   <li>다양한 정렬 전략 — 최신순, 가격순(오름/내림), 인기순, 평점순</li>
 *   <li>Java 25 Stream Gatherers를 활용한 배치 처리</li>
 *   <li>인기 검색어 집계 (in-memory counter)</li>
 *   <li>자동완성 제안</li>
 *   <li>검색 결과 페이지네이션</li>
 * </ul>
 */
@Slf4j
@Service("productSearchServiceExtension")
@RequiredArgsConstructor
public class ProductSearchServiceImpl {

    /**
     * 배치 처리 기본 윈도우 크기.
     */
    public static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * 자동완성 최대 제안 수.
     */
    public static final int MAX_SUGGEST_LIMIT = 20;

    /**
     * 인기 검색어 통계 (in-memory, 운영에선 Redis 권장).
     */
    private final Map<String, AtomicLong> keywordHits = new ConcurrentHashMap<>();

    private final ProductRepository productRepository;

    /**
     * 정렬 전략 sealed interface.
     */
    public sealed interface SortBy permits SortBy.Newest, SortBy.Oldest,
            SortBy.PriceAsc, SortBy.PriceDesc, SortBy.Popular, SortBy.Rating, SortBy.NameAsc {

        record Newest() implements SortBy {}
        record Oldest() implements SortBy {}
        record PriceAsc() implements SortBy {}
        record PriceDesc() implements SortBy {}
        record Popular() implements SortBy {}
        record Rating() implements SortBy {}
        record NameAsc() implements SortBy {}
    }

    /**
     * 검색 조건 record.
     */
    public record SearchFilter(
        String keyword,
        Long categoryId,
        Long brandId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<String> tags,
        Boolean inStockOnly,
        Boolean onSaleOnly,
        SortBy sort
    ) {
        public boolean hasKeyword() {
            return keyword != null && !keyword.isBlank();
        }
        public boolean hasTags() {
            return tags != null && !tags.isEmpty();
        }
    }

    /**
     * 복합 필터 검색.
     */
    @Transactional(readOnly = true)
    public Page<Product> search(SearchFilter filter, Pageable pageable) {
        log.debug("검색 시도: filter={}", filter);
        if (filter.hasKeyword()) {
            recordKeywordHit(filter.keyword());
        }

        var stream = productRepository.findAll().stream();
        stream = applyKeywordFilter(stream, filter);
        stream = applyCategoryFilter(stream, filter);
        stream = applyBrandFilter(stream, filter);
        stream = applyPriceFilter(stream, filter);
        stream = applyTagFilter(stream, filter);
        stream = applyAvailabilityFilter(stream, filter);

        var sorted = applySort(stream, filter.sort()).toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        var content = start >= sorted.size() ? List.<Product>of() : sorted.subList(start, end);
        return new PageImpl<>(content, pageable, sorted.size());
    }

    /**
     * 자동완성 — 입력 접두사로 시작하는 상품명/태그를 우선 매칭.
     */
    @Transactional(readOnly = true)
    public List<String> autocomplete(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return List.of();
        int max = Math.min(limit, MAX_SUGGEST_LIMIT);
        var lc = prefix.toLowerCase();
        return productRepository.findAll().stream()
            .map(Product::getName)
            .filter(Objects::nonNull)
            .filter(name -> name.toLowerCase().startsWith(lc))
            .distinct()
            .sorted()
            .limit(max)
            .toList();
    }

    /**
     * 검색 결과를 고정 윈도우로 분할하여 배치 처리한다.
     *
     * <p>Java 25 Stream Gatherers의 {@code windowFixed} 사용 예시.
     */
    @Transactional(readOnly = true)
    public List<List<Product>> chunkByWindow(SearchFilter filter, int windowSize) {
        var page = search(filter, Pageable.unpaged());
        return page.getContent().stream()
            .gather(Gatherers.windowFixed(Math.max(1, windowSize)))
            .toList();
    }

    /**
     * 배치 처리 콜백 형태.
     */
    @Transactional(readOnly = true)
    public void processBatched(SearchFilter filter, int batchSize, BatchProcessor processor) {
        var page = search(filter, Pageable.unpaged());
        var batches = page.getContent().stream()
            .gather(Gatherers.windowFixed(Math.max(1, batchSize)))
            .toList();
        int batchNumber = 0;
        for (var batch : batches) {
            batchNumber++;
            try {
                processor.process(batchNumber, batch);
            } catch (Exception e) {
                log.error("배치 처리 실패: batchNumber={}, size={}", batchNumber, batch.size(), e);
                throw new RuntimeException("배치 처리 실패", e);
            }
        }
        log.info("배치 처리 완료: totalBatches={}, totalProducts={}", batchNumber, page.getContent().size());
    }

    /**
     * 콜백 인터페이스.
     */
    @FunctionalInterface
    public interface BatchProcessor {
        void process(int batchNumber, List<Product> batch);
    }

    /**
     * 인기 검색어 TOP N.
     */
    public List<KeywordStat> topKeywords(int limit) {
        return keywordHits.entrySet().stream()
            .sorted(Map.Entry.<String, AtomicLong>comparingByValue(
                Comparator.comparingLong(AtomicLong::get).reversed()))
            .limit(limit)
            .map(e -> new KeywordStat(e.getKey(), e.getValue().get()))
            .toList();
    }

    /** 키워드 통계 record. */
    public record KeywordStat(String keyword, long count) {}

    /**
     * 통계 초기화 (테스트/운영 작업용).
     */
    public void resetKeywordStats() {
        keywordHits.clear();
        log.info("인기 검색어 통계 초기화 완료");
    }

    /**
     * 검색 카운터 증가.
     */
    public void recordKeywordHit(String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        var normalized = keyword.trim().toLowerCase();
        keywordHits.computeIfAbsent(normalized, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 유사 상품 추천 (같은 카테고리/브랜드/태그 기반).
     */
    @Transactional(readOnly = true)
    public List<Product> findSimilar(Long productId, int limit) {
        var target = productRepository.findById(productId).orElse(null);
        if (target == null) return List.of();
        return productRepository.findAll().stream()
            .filter(p -> !p.getId().equals(productId))
            .filter(p -> p.getStatus() instanceof ProductStatus.Active)
            .filter(p -> matchesAnyDimension(p, target))
            .sorted(scoreComparator(target))
            .limit(limit)
            .toList();
    }

    /**
     * 카테고리별 검색 결과 분포.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> facetByCategory(SearchFilter filter) {
        var result = new LinkedHashMap<Long, Long>();
        var page = search(
            new SearchFilter(filter.keyword(), null, filter.brandId(), filter.minPrice(),
                filter.maxPrice(), filter.tags(), filter.inStockOnly(),
                filter.onSaleOnly(), filter.sort()),
            Pageable.unpaged()
        );
        for (var product : page.getContent()) {
            result.merge(product.getCategoryId(), 1L, Long::sum);
        }
        return result;
    }

    /**
     * 브랜드별 검색 결과 분포.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> facetByBrand(SearchFilter filter) {
        var result = new LinkedHashMap<Long, Long>();
        var page = search(
            new SearchFilter(filter.keyword(), filter.categoryId(), null, filter.minPrice(),
                filter.maxPrice(), filter.tags(), filter.inStockOnly(),
                filter.onSaleOnly(), filter.sort()),
            Pageable.unpaged()
        );
        for (var product : page.getContent()) {
            if (product.getBrandId() == null) continue;
            result.merge(product.getBrandId(), 1L, Long::sum);
        }
        return result;
    }

    /**
     * 가격대별 분포.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> facetByPriceRange(SearchFilter filter) {
        var result = new LinkedHashMap<String, Long>();
        result.put("0-10000", 0L);
        result.put("10000-50000", 0L);
        result.put("50000-100000", 0L);
        result.put("100000-500000", 0L);
        result.put("500000+", 0L);

        var page = search(
            new SearchFilter(filter.keyword(), filter.categoryId(), filter.brandId(), null, null,
                filter.tags(), filter.inStockOnly(), filter.onSaleOnly(), filter.sort()),
            Pageable.unpaged()
        );
        for (var product : page.getContent()) {
            var price = product.getPrice();
            if (price == null) continue;
            var bucket = priceBucket(price);
            result.merge(bucket, 1L, Long::sum);
        }
        return result;
    }

    /**
     * 최근 30일 트렌딩 상품 — 판매 카운트 기반.
     */
    @Transactional(readOnly = true)
    public List<Product> trendingProducts(int limit) {
        var cutoff = LocalDateTime.now().minusDays(30);
        return productRepository.findAll().stream()
            .filter(p -> p.getStatus() instanceof ProductStatus.Active)
            .filter(p -> p.getUpdatedAt() == null || p.getUpdatedAt().isAfter(cutoff))
            .sorted(Comparator.comparingLong(Product::getSalesCount).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * 신규 상품 (등록 N일 이내).
     */
    @Transactional(readOnly = true)
    public List<Product> newArrivals(int days, int limit) {
        var cutoff = LocalDateTime.now().minusDays(days);
        return productRepository.findAll().stream()
            .filter(p -> p.getStatus() instanceof ProductStatus.Active)
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(cutoff))
            .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
            .limit(limit)
            .toList();
    }

    // ---- filter helpers ----

    private Stream<Product> applyKeywordFilter(Stream<Product> stream, SearchFilter filter) {
        if (!filter.hasKeyword()) return stream;
        var k = filter.keyword().toLowerCase();
        return stream.filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(k))
            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(k))
            || (p.getSku() != null && p.getSku().toLowerCase().contains(k)));
    }

    private Stream<Product> applyCategoryFilter(Stream<Product> stream, SearchFilter filter) {
        if (filter.categoryId() == null) return stream;
        return stream.filter(p -> filter.categoryId().equals(p.getCategoryId()));
    }

    private Stream<Product> applyBrandFilter(Stream<Product> stream, SearchFilter filter) {
        if (filter.brandId() == null) return stream;
        return stream.filter(p -> filter.brandId().equals(p.getBrandId()));
    }

    private Stream<Product> applyPriceFilter(Stream<Product> stream, SearchFilter filter) {
        if (filter.minPrice() != null) {
            stream = stream.filter(p -> p.getPrice() != null
                && p.getPrice().compareTo(filter.minPrice()) >= 0);
        }
        if (filter.maxPrice() != null) {
            stream = stream.filter(p -> p.getPrice() != null
                && p.getPrice().compareTo(filter.maxPrice()) <= 0);
        }
        return stream;
    }

    private Stream<Product> applyTagFilter(Stream<Product> stream, SearchFilter filter) {
        if (!filter.hasTags()) return stream;
        var wanted = filter.tags();
        return stream.filter(p -> p.getTags() != null
            && p.getTags().stream().anyMatch(wanted::contains));
    }

    private Stream<Product> applyAvailabilityFilter(Stream<Product> stream, SearchFilter filter) {
        if (Boolean.TRUE.equals(filter.inStockOnly())) {
            stream = stream.filter(p -> p.getStatus() instanceof ProductStatus.Active);
        }
        if (Boolean.TRUE.equals(filter.onSaleOnly())) {
            stream = stream.filter(Product::isOnDiscount);
        }
        return stream;
    }

    /**
     * 정렬 적용 — Java 21+ pattern matching for switch.
     */
    private Stream<Product> applySort(Stream<Product> stream, SortBy sort) {
        var actual = sort != null ? sort : new SortBy.Newest();
        return switch (actual) {
            case SortBy.Newest _ ->
                stream.sorted(Comparator.comparing(Product::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            case SortBy.Oldest _ ->
                stream.sorted(Comparator.comparing(Product::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            case SortBy.PriceAsc _ ->
                stream.sorted(Comparator.comparing(Product::getPrice,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            case SortBy.PriceDesc _ ->
                stream.sorted(Comparator.comparing(Product::getPrice,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            case SortBy.Popular _ ->
                stream.sorted(Comparator.comparingLong(Product::getSalesCount).reversed());
            case SortBy.Rating _ ->
                stream.sorted(Comparator.comparingDouble(Product::getAverageRating).reversed());
            case SortBy.NameAsc _ ->
                stream.sorted(Comparator.comparing(Product::getName,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        };
    }

    private boolean matchesAnyDimension(Product candidate, Product target) {
        if (Objects.equals(candidate.getCategoryId(), target.getCategoryId())) return true;
        if (candidate.getBrandId() != null
            && candidate.getBrandId().equals(target.getBrandId())) return true;
        if (candidate.getTags() != null && target.getTags() != null
            && candidate.getTags().stream().anyMatch(target.getTags()::contains)) return true;
        return false;
    }

    private Comparator<Product> scoreComparator(Product target) {
        return Comparator.<Product>comparingInt(p -> {
            int score = 0;
            if (Objects.equals(p.getCategoryId(), target.getCategoryId())) score += 5;
            if (p.getBrandId() != null && p.getBrandId().equals(target.getBrandId())) score += 10;
            if (p.getTags() != null && target.getTags() != null) {
                var common = new HashMap<>(Map.of("count", 0L));
                p.getTags().stream()
                    .filter(target.getTags()::contains)
                    .forEach(t -> common.merge("count", 1L, (a, b) -> ((long) a) + ((long) b)));
                score += ((Number) common.get("count")).intValue() * 3;
            }
            return -score;
        });
    }

    private String priceBucket(BigDecimal price) {
        var p = price.intValue();
        if (p < 10_000) return "0-10000";
        if (p < 50_000) return "10000-50000";
        if (p < 100_000) return "50000-100000";
        if (p < 500_000) return "100000-500000";
        return "500000+";
    }
}
