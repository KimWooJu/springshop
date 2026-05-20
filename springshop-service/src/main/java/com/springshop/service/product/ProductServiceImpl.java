package com.springshop.service.product;

import com.springshop.domain.product.Product;
import com.springshop.domain.product.ProductRepository;
import com.springshop.domain.product.ProductStatus;
import com.springshop.domain.product.ProductEvents.ProductCreatedEvent;
import com.springshop.domain.product.ProductEvents.ProductOutOfStockEvent;
import com.springshop.domain.product.ProductEvents.ProductPriceChangedEvent;
import com.springshop.domain.product.ProductEvents.ProductPublishedEvent;
import com.springshop.common.exception.DuplicateResourceException;
import com.springshop.common.exception.InvalidStateException;
import com.springshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link ProductService} 표준 구현.
 *
 * <p>주요 책임:
 * <ul>
 *   <li>상품 등록/수정/삭제 시 도메인 규칙 검증</li>
 *   <li>{@code @Cacheable}로 단건 조회 캐시</li>
 *   <li>조회수는 Redis HINCRBY로 비동기 누적, 일정 주기에 DB로 flush</li>
 *   <li>가격 변경/품절/공개 시 도메인 이벤트 발행</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final String VIEW_COUNTER_KEY = "product:views";
    private static final String VIEW_COUNTER_LAST_FLUSH = "product:views:lastFlush";

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    @CacheEvict(value = "product", allEntries = true)
    public Product createProduct(ProductCreateCommand command) {
        log.info("상품 등록 요청: sku={}, name={}", command.sku(), command.name());
        validateCreate(command);

        if (productRepository.existsBySku(command.sku())) {
            throw new DuplicateResourceException("이미 등록된 SKU입니다: " + command.sku());
        }

        var product = Product.create(
            command.sku(),
            command.name(),
            command.description(),
            command.price(),
            command.categoryId(),
            command.brandId(),
            command.initialStock(),
            command.mainImageUrl()
        );

        if (command.tags() != null) {
            command.tags().forEach(product::addTag);
        }

        var saved = productRepository.save(product);
        eventPublisher.publishEvent(ProductCreatedEvent.of(
            saved.getId(),
            saved.getName(),
            saved.getCategoryId()
        ));
        log.info("상품 등록 완료: id={}, sku={}", saved.getId(), saved.getSku());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product updateProduct(Long productId, ProductUpdateCommand command) {
        var product = loadProduct(productId);
        var oldPrice = product.getPrice();

        product.update(
            command.name(),
            command.description(),
            command.price(),
            command.mainImageUrl()
        );

        if (command.tags() != null) {
            product.replaceTags(command.tags());
        }

        var saved = productRepository.save(product);

        if (command.price() != null && command.price().compareTo(oldPrice) != 0) {
            eventPublisher.publishEvent(ProductPriceChangedEvent.of(productId, oldPrice, command.price()));
        }
        log.info("상품 수정 완료: id={}", productId);
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public void deleteProduct(Long productId, String requester) {
        var product = loadProduct(productId);
        productRepository.delete(product);
        log.warn("상품 영구 삭제: id={}, requester={}", productId, requester);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public void softDeleteProduct(Long productId, String requester) {
        var product = loadProduct(productId);
        product.softDelete(requester);
        productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product publish(Long productId) {
        var product = loadProduct(productId);
        if (product.getStatus() instanceof ProductStatus.Active) {
            log.debug("이미 판매중 상태: id={}", productId);
            return product;
        }
        product.publish();
        var saved = productRepository.save(product);
        eventPublisher.publishEvent(ProductPublishedEvent.of(productId, product.getName()));
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product unpublish(Long productId, String reason) {
        var product = loadProduct(productId);
        product.unpublish(reason);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product markOutOfStock(Long productId) {
        var product = loadProduct(productId);
        product.markOutOfStock();
        var saved = productRepository.save(product);
        eventPublisher.publishEvent(ProductOutOfStockEvent.of(productId, product.getName()));
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product markInStock(Long productId) {
        var product = loadProduct(productId);
        product.markInStock();
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product discontinue(Long productId, String reason) {
        var product = loadProduct(productId);
        product.discontinue(reason);
        return productRepository.save(product);
    }

    @Override
    @Cacheable(value = "product", key = "#productId")
    public Product findById(Long productId) {
        return loadProduct(productId);
    }

    @Override
    public Optional<Product> findOptionalById(Long productId) {
        return productRepository.findById(productId);
    }

    @Override
    public Product findBySku(String sku) {
        return productRepository.findBySku(sku)
            .orElseThrow(() -> new ResourceNotFoundException("SKU를 찾을 수 없습니다: " + sku));
    }

    @Override
    public Product findByIdAndCountView(Long productId) {
        var product = loadProduct(productId);
        try {
            redisTemplate.opsForHash().increment(VIEW_COUNTER_KEY, String.valueOf(productId), 1L);
        } catch (Exception e) {
            log.warn("조회수 증가 실패 (id={}): {}", productId, e.getMessage());
        }
        return product;
    }

    @Override
    public List<Product> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return productRepository.findAllById(ids);
    }

    @Override
    public Page<Product> listByCategory(Long categoryId, Pageable pageable) {
        return pageOf(productRepository.findAllByCategoryId(categoryId), pageable);
    }

    @Override
    public Page<Product> listByBrand(Long brandId, Pageable pageable) {
        return pageOf(productRepository.findAllByBrandId(brandId), pageable);
    }

    @Override
    public Page<Product> listByStatus(ProductStatus status, Pageable pageable) {
        var filtered = productRepository.findAll().stream()
            .filter(p -> p.getStatus().label().equals(status.label()))
            .toList();
        return pageOf(filtered, pageable);
    }

    @Override
    public Page<Product> listAll(Pageable pageable) {
        return pageOf(productRepository.findAll(), pageable);
    }

    @Override
    public List<Product> findRecentlyAdded(int limit) {
        return productRepository.findAll().stream()
            .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    public List<Product> findMostViewed(int limit) {
        var counters = redisTemplate.opsForHash().entries(VIEW_COUNTER_KEY);
        return counters.entrySet().stream()
            .sorted((a, b) -> Long.compare(parseLong(b.getValue()), parseLong(a.getValue())))
            .limit(limit)
            .map(e -> productRepository.findById(Long.parseLong(String.valueOf(e.getKey()))).orElse(null))
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    @Override
    public List<Product> findBestSellers(int limit, int days) {
        // 도메인이 sales count를 보관한다고 가정
        return productRepository.findAll().stream()
            .sorted(Comparator.comparingLong(Product::getSalesCount).reversed())
            .limit(limit)
            .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product changePrice(Long productId, BigDecimal newPrice, String reason) {
        var product = loadProduct(productId);
        if (newPrice == null || newPrice.signum() < 0)
            throw new InvalidStateException("가격은 0 이상이어야 합니다.");
        var oldPrice = product.getPrice();
        product.changePrice(newPrice, reason);
        var saved = productRepository.save(product);
        eventPublisher.publishEvent(ProductPriceChangedEvent.of(productId, oldPrice, newPrice));
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product applyDiscount(Long productId, BigDecimal discountedPrice, LocalDateTime untilWhen) {
        var product = loadProduct(productId);
        if (discountedPrice == null || discountedPrice.signum() < 0)
            throw new InvalidStateException("할인 가격은 0 이상이어야 합니다.");
        if (discountedPrice.compareTo(product.getPrice()) >= 0)
            throw new InvalidStateException("할인 가격은 원가보다 낮아야 합니다.");
        product.applyDiscount(discountedPrice, untilWhen);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product removeDiscount(Long productId) {
        var product = loadProduct(productId);
        product.removeDiscount();
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product syncStockQuantity(Long productId, long quantity) {
        var product = loadProduct(productId);
        product.syncStock(quantity);
        if (quantity == 0 && product.getStatus() instanceof ProductStatus.Active) {
            product.markOutOfStock();
            eventPublisher.publishEvent(ProductOutOfStockEvent.of(productId, product.getName()));
        }
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void reorderDisplayPriority(List<DisplayPriorityCommand> commands) {
        if (commands == null || commands.isEmpty()) return;
        commands.forEach(cmd -> productRepository.findById(cmd.productId()).ifPresent(p -> {
            p.changeDisplayOrder(cmd.displayOrder());
            productRepository.save(p);
        }));
        log.info("노출 순서 일괄 변경 완료: {}건", commands.size());
    }

    @Override
    public boolean isSkuTaken(String sku) {
        return productRepository.existsBySku(sku);
    }

    @Override
    @Transactional
    public Product duplicate(Long productId, String newSku, String newName) {
        var src = loadProduct(productId);
        if (productRepository.existsBySku(newSku))
            throw new DuplicateResourceException("이미 등록된 SKU: " + newSku);
        var copy = Product.create(
            newSku,
            newName,
            src.getDescription(),
            src.getPrice(),
            src.getCategoryId(),
            src.getBrandId(),
            0L,
            src.getMainImageUrl()
        );
        return productRepository.save(copy);
    }

    @Override
    public ProductStatsSummary getStatsSummary() {
        var all = productRepository.findAll();
        var today = LocalDate.now();
        long total = all.size();
        long onSale = all.stream().filter(p -> p.getStatus() instanceof ProductStatus.Active).count();
        long oos = all.stream().filter(p -> p.getStatus() instanceof ProductStatus.OutOfStock).count();
        long discontinued = all.stream().filter(p -> p.getStatus() instanceof ProductStatus.Discontinued).count();
        long newToday = all.stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().equals(today))
            .count();
        BigDecimal avg = total == 0 ? BigDecimal.ZERO :
            all.stream().map(Product::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new ProductStatsSummary(total, onSale, oos, discontinued, newToday, avg);
    }

    @Override
    public Page<Product> simpleSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) return listAll(pageable);
        var k = keyword.toLowerCase();
        var hits = productRepository.findAll().stream()
            .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(k)
                  || p.getDescription() != null && p.getDescription().toLowerCase().contains(k)
                  || p.getSku() != null && p.getSku().toLowerCase().contains(k))
            .toList();
        return pageOf(hits, pageable);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product changeCategory(Long productId, Long newCategoryId) {
        var product = loadProduct(productId);
        product.changeCategory(newCategoryId);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product changeBrand(Long productId, Long newBrandId) {
        var product = loadProduct(productId);
        product.changeBrand(newBrandId);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product markAsNew(Long productId, boolean newProduct) {
        var product = loadProduct(productId);
        product.markAsNew(newProduct);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public Product markAsRecommended(Long productId, boolean recommended) {
        var product = loadProduct(productId);
        product.markAsRecommended(recommended);
        return productRepository.save(product);
    }

    @Override
    public ProductDetailWithRecommendations findDetailWithRecommendations(
            Long productId, int recommendLimit) {
        var product = findByIdAndCountView(productId);
        var siblings = productRepository.findAllByCategoryId(product.getCategoryId()).stream()
            .filter(p -> !p.getId().equals(productId))
            .filter(p -> p.getStatus() instanceof ProductStatus.Active)
            .sorted(Comparator.comparingLong(Product::getSalesCount).reversed())
            .limit(Math.max(1, recommendLimit))
            .toList();
        return new ProductDetailWithRecommendations(product, siblings);
    }

    // ----- 내부 -----

    private Product loadProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다: " + productId));
    }

    private void validateCreate(ProductCreateCommand cmd) {
        if (cmd.sku() == null || cmd.sku().isBlank()) throw new InvalidStateException("SKU는 필수");
        if (cmd.name() == null || cmd.name().isBlank()) throw new InvalidStateException("상품명은 필수");
        if (cmd.price() == null || cmd.price().signum() < 0) throw new InvalidStateException("가격은 0 이상");
        if (cmd.categoryId() == null) throw new InvalidStateException("카테고리는 필수");
        if (cmd.initialStock() < 0) throw new InvalidStateException("초기 재고는 0 이상");
    }

    private Page<Product> pageOf(List<Product> all, Pageable pageable) {
        var sorted = all.stream()
            .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
            .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        var content = start >= sorted.size() ? List.<Product>of() : sorted.subList(start, end);
        return new PageImpl<>(content, pageable, sorted.size());
    }

    private long parseLong(Object o) {
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }
}
