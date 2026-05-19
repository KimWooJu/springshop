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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * 상품 검색 서비스.
 *
 * <p>키워드/필터/정렬/페이징을 조합한 카탈로그 검색을 담당한다.
 * Stream Gatherers(Java 25)와 record pattern matching을 적극 활용한다.
 */
public interface ProductSearchService {

    Page<Product> search(SearchCondition condition, Pageable pageable);

    List<Product> findSimilar(Long productId, int limit);

    List<Product> findRelated(List<String> tags, int limit);

    /** 검색어 자동완성 후보 */
    List<String> suggest(String prefix, int limit);

    /** 페이지 단위로 그룹화하여 카탈로그 슬라이드 형태로 반환 */
    List<List<Product>> chunkByWindow(SearchCondition condition, int windowSize);

    sealed interface SortStrategy permits SortStrategy.Newest, SortStrategy.PriceAsc,
            SortStrategy.PriceDesc, SortStrategy.Popularity, SortStrategy.Rating {
        record Newest() implements SortStrategy {}
        record PriceAsc() implements SortStrategy {}
        record PriceDesc() implements SortStrategy {}
        record Popularity() implements SortStrategy {}
        record Rating() implements SortStrategy {}
    }

    record SearchCondition(
        String keyword,
        Long categoryId,
        Long brandId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<String> tags,
        Boolean inStockOnly,
        Boolean onSaleOnly,
        SortStrategy sort
    ) {
        public boolean hasKeyword() { return keyword != null && !keyword.isBlank(); }
        public boolean hasTags() { return tags != null && !tags.isEmpty(); }
    }

    @Slf4j
    @Service
    @RequiredArgsConstructor
    class Impl implements ProductSearchService {

        private final ProductRepository productRepository;

        @Override
        @Transactional(readOnly = true)
        public Page<Product> search(SearchCondition condition, Pageable pageable) {
            log.debug("상품 검색: condition={}", condition);
            var stream = productRepository.findAll().stream();

            if (condition.hasKeyword()) {
                var k = condition.keyword().toLowerCase();
                stream = stream.filter(p ->
                    (p.getName() != null && p.getName().toLowerCase().contains(k))
                    || (p.getDescription() != null && p.getDescription().toLowerCase().contains(k))
                    || (p.getSku() != null && p.getSku().toLowerCase().contains(k))
                );
            }
            if (condition.categoryId() != null) {
                stream = stream.filter(p -> condition.categoryId().equals(p.getCategoryId()));
            }
            if (condition.brandId() != null) {
                stream = stream.filter(p -> condition.brandId().equals(p.getBrandId()));
            }
            if (condition.minPrice() != null) {
                stream = stream.filter(p -> p.getPrice().compareTo(condition.minPrice()) >= 0);
            }
            if (condition.maxPrice() != null) {
                stream = stream.filter(p -> p.getPrice().compareTo(condition.maxPrice()) <= 0);
            }
            if (condition.hasTags()) {
                var wantedTags = condition.tags();
                stream = stream.filter(p -> p.getTags() != null
                    && p.getTags().stream().anyMatch(wantedTags::contains));
            }
            if (Boolean.TRUE.equals(condition.inStockOnly())) {
                stream = stream.filter(p -> p.getStatus() == ProductStatus.ON_SALE);
            }
            if (Boolean.TRUE.equals(condition.onSaleOnly())) {
                stream = stream.filter(Product::isOnDiscount);
            }

            var sorted = applySort(stream, condition.sort()).toList();

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), sorted.size());
            var content = start >= sorted.size() ? List.<Product>of() : sorted.subList(start, end);
            return new PageImpl<>(content, pageable, sorted.size());
        }

        @Override
        @Transactional(readOnly = true)
        public List<Product> findSimilar(Long productId, int limit) {
            var target = productRepository.findById(productId).orElse(null);
            if (target == null) return List.of();
            return productRepository.findAllByCategoryId(target.getCategoryId()).stream()
                .filter(p -> !p.getId().equals(productId))
                .filter(p -> p.getStatus() == ProductStatus.ON_SALE)
                .sorted(similarityComparator(target))
                .limit(limit)
                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<Product> findRelated(List<String> tags, int limit) {
            if (tags == null || tags.isEmpty()) return List.of();
            return productRepository.findAll().stream()
                .filter(p -> p.getStatus() == ProductStatus.ON_SALE)
                .filter(p -> p.getTags() != null
                    && p.getTags().stream().anyMatch(tags::contains))
                .sorted(Comparator.comparingLong(Product::getSalesCount).reversed())
                .limit(limit)
                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<String> suggest(String prefix, int limit) {
            if (prefix == null || prefix.isBlank()) return List.of();
            var lc = prefix.toLowerCase();
            return productRepository.findAll().stream()
                .map(Product::getName)
                .filter(Objects::nonNull)
                .filter(name -> name.toLowerCase().startsWith(lc))
                .distinct()
                .sorted()
                .limit(limit)
                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public List<List<Product>> chunkByWindow(SearchCondition condition, int windowSize) {
            var page = search(condition, Pageable.unpaged());
            // Java 25 Stream Gatherers — 고정 윈도우로 분할
            return page.getContent().stream()
                .gather(Gatherers.windowFixed(Math.max(1, windowSize)))
                .toList();
        }

        // ---- helpers ----

        private Stream<Product> applySort(Stream<Product> stream, SortStrategy strategy) {
            var resolved = strategy != null ? strategy : new SortStrategy.Newest();
            // Java 21+ pattern matching for switch
            return switch (resolved) {
                case SortStrategy.Newest _ ->
                    stream.sorted(Comparator.comparing(Product::getCreatedAt).reversed());
                case SortStrategy.PriceAsc _ ->
                    stream.sorted(Comparator.comparing(Product::getPrice));
                case SortStrategy.PriceDesc _ ->
                    stream.sorted(Comparator.comparing(Product::getPrice).reversed());
                case SortStrategy.Popularity _ ->
                    stream.sorted(Comparator.comparingLong(Product::getSalesCount).reversed());
                case SortStrategy.Rating _ ->
                    stream.sorted(Comparator.comparingDouble(Product::getAverageRating).reversed());
            };
        }

        private Comparator<Product> similarityComparator(Product target) {
            return Comparator.<Product>comparingInt(p -> {
                int score = 0;
                if (p.getBrandId() != null && p.getBrandId().equals(target.getBrandId())) score += 10;
                if (p.getTags() != null && target.getTags() != null) {
                    var common = new java.util.HashSet<>(p.getTags());
                    common.retainAll(target.getTags());
                    score += common.size() * 3;
                }
                if (p.getPrice() != null && target.getPrice() != null) {
                    var diff = p.getPrice().subtract(target.getPrice()).abs();
                    if (diff.compareTo(target.getPrice().multiply(BigDecimal.valueOf(0.2))) <= 0) score += 5;
                }
                return -score;
            });
        }
    }
}
