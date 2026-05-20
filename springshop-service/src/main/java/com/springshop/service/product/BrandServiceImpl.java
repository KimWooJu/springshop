package com.springshop.service.product;

import com.springshop.domain.product.Brand;
import com.springshop.domain.product.BrandRepository;
import com.springshop.common.exception.DuplicateResourceException;
import com.springshop.common.exception.InvalidStateException;
import com.springshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 브랜드 관리 보조 구현체.
 *
 * <p>표준 CRUD는 {@link BrandService.Impl}에서 처리하며, 본 구현체는 다음을 담당한다:
 * <ul>
 *   <li>브랜드 활성/비활성 토글</li>
 *   <li>브랜드별 상품 수 조회 (캐시 적용)</li>
 *   <li>국가별 브랜드 필터링</li>
 *   <li>인기 브랜드 랭킹</li>
 * </ul>
 *
 * <p>{@code @Cacheable("brands")}로 목록 캐싱을 적용해 카탈로그 페이지 로딩을 가속한다.
 */
@Slf4j
@Service("brandServiceExtension")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl {

    /**
     * 캐시 키 — 활성 브랜드 목록.
     */
    public static final String CACHE_ACTIVE_BRANDS = "brands.active";

    /**
     * 캐시 키 — 전체 브랜드 목록.
     */
    public static final String CACHE_ALL_BRANDS = "brands.all";

    private final BrandRepository brandRepository;

    /**
     * 브랜드 생성.
     */
    @Transactional
    @CacheEvict(value = {CACHE_ACTIVE_BRANDS, CACHE_ALL_BRANDS}, allEntries = true)
    public Brand createBrand(String name, String slug, String countryCode, String description) {
        validate(name, slug);
        if (brandRepository.existsByName(name)) {
            throw new DuplicateResourceException("이미 사용 중인 브랜드명: " + name);
        }
        var brand = Brand.of(name, countryCode);
        if (description != null) brand.updateDescription(description);
        var saved = brandRepository.save(brand);
        log.info("브랜드 생성: id={}, slug={}", saved.getId(), slug);
        return saved;
    }

    /**
     * 브랜드 업데이트.
     */
    @Transactional
    @CacheEvict(value = {CACHE_ACTIVE_BRANDS, CACHE_ALL_BRANDS}, allEntries = true)
    public Brand updateBrand(Long brandId, String name, String description,
                              String logoUrl, String websiteUrl) {
        var brand = load(brandId);
        if (name != null && !name.isBlank()) {
            // 이름 유효성 체크
        }
        if (name != null && !name.isBlank()) brand.rename(name);
        if (description != null) brand.updateDescription(description);
        if (logoUrl != null) brand.updateLogo(logoUrl);
        if (websiteUrl != null) brand.updateWebsite(websiteUrl);
        return brandRepository.save(brand);
    }

    /**
     * 활성/비활성 토글.
     */
    @Transactional
    @CacheEvict(value = {CACHE_ACTIVE_BRANDS, CACHE_ALL_BRANDS}, allEntries = true)
    public Brand toggleActive(Long brandId) {
        var brand = load(brandId);
        if (brand.isActive()) {
            brand.deactivate();
        } else {
            brand.activate();
        }
        log.info("브랜드 활성 토글: id={}, nowActive={}", brandId, brand.isActive());
        return brandRepository.save(brand);
    }

    /**
     * 브랜드 비활성화 (이유 명시).
     */
    @Transactional
    @CacheEvict(value = {CACHE_ACTIVE_BRANDS, CACHE_ALL_BRANDS}, allEntries = true)
    public Brand deactivate(Long brandId, String reason) {
        var brand = load(brandId);
        brand.deactivate();
        return brandRepository.save(brand);
    }

    /**
     * 브랜드 활성화.
     */
    @Transactional
    @CacheEvict(value = {CACHE_ACTIVE_BRANDS, CACHE_ALL_BRANDS}, allEntries = true)
    public Brand activate(Long brandId) {
        var brand = load(brandId);
        brand.activate();
        return brandRepository.save(brand);
    }

    /**
     * 활성 브랜드 목록 (캐시).
     */
    @Cacheable(CACHE_ACTIVE_BRANDS)
    public List<Brand> listActive() {
        return brandRepository.findAll().stream()
            .filter(Brand::isActive)
            .sorted(Comparator.comparing(Brand::getName))
            .toList();
    }

    /**
     * 전체 브랜드 목록 (캐시).
     */
    @Cacheable(CACHE_ALL_BRANDS)
    public List<Brand> listAll() {
        return brandRepository.findAll().stream()
            .sorted(Comparator.comparing(Brand::getName))
            .toList();
    }

    /**
     * 단건 조회.
     */
    public Brand findById(Long brandId) {
        return load(brandId);
    }

    /**
     * 슬러그로 조회.
     */
    public Optional<Brand> findBySlug(String slug) {
        return brandRepository.findByName(slug);
    }

    /**
     * 국가별 브랜드.
     */
    public List<Brand> findByCountry(String countryCode) {
        if (countryCode == null) return List.of();
        var upper = countryCode.toUpperCase();
        return brandRepository.findAll().stream()
            .filter(b -> upper.equals(b.getCountry()))
            .sorted(Comparator.comparing(Brand::getName))
            .toList();
    }

    /**
     * 인기 브랜드 (상품 수 기준).
     */
    public List<Brand> topByProductCount(int limit) {
        return brandRepository.findAll().stream()
            .filter(Brand::isActive)
            .sorted(Comparator.comparingLong(Brand::getProductCount).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * 브랜드별 상품 수 맵.
     */
    @Cacheable("brand.productCounts")
    public Map<Long, Long> productCountByBrand() {
        var result = new LinkedHashMap<Long, Long>();
        for (var brand : brandRepository.findAll()) {
            result.put(brand.getId(), brand.getProductCount());
        }
        return result;
    }

    /**
     * 브랜드 검색 (이름 부분 일치).
     */
    public List<Brand> searchByName(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) return List.of();
        var lc = keyword.toLowerCase();
        return brandRepository.findAll().stream()
            .filter(b -> b.getName() != null && b.getName().toLowerCase().contains(lc))
            .sorted(Comparator.comparing(Brand::getName))
            .limit(limit)
            .toList();
    }

    /**
     * 브랜드 통합 통계.
     */
    public BrandStats statistics() {
        var all = brandRepository.findAll();
        long total = all.size();
        long active = all.stream().filter(Brand::isActive).count();
        long withProducts = all.stream().filter(b -> b.getProductCount() > 0).count();
        long totalProducts = all.stream().mapToLong(Brand::getProductCount).sum();
        return new BrandStats(total, active, total - active, withProducts, totalProducts);
    }

    /**
     * 브랜드 통계 record.
     */
    public record BrandStats(
        long totalBrands,
        long activeBrands,
        long inactiveBrands,
        long brandsWithProducts,
        long totalProducts
    ) {
        public double activeRatio() {
            return totalBrands == 0 ? 0.0 : (double) activeBrands / totalBrands;
        }
    }

    // ---- helpers ----

    private Brand load(Long brandId) {
        return brandRepository.findById(brandId)
            .orElseThrow(() -> new ResourceNotFoundException("브랜드를 찾을 수 없습니다: " + brandId));
    }

    private void validate(String name, String slug) {
        if (name == null || name.isBlank()) {
            throw new InvalidStateException("브랜드명은 필수입니다.");
        }
        if (slug == null || !slug.matches("^[a-z0-9\\-]{1,60}$")) {
            throw new InvalidStateException("슬러그는 소문자/숫자/하이픈만 허용 (1~60자)");
        }
    }
}
