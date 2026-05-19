package com.springshop.service.product;

import com.springshop.domain.product.Brand;
import com.springshop.domain.product.BrandRepository;
import com.springshop.domain.common.exception.DuplicateResourceException;
import com.springshop.domain.common.exception.InvalidStateException;
import com.springshop.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 브랜드 관리 서비스.
 *
 * <p>브랜드 식별자(slug)는 전역 유일하며,
 * 활성 브랜드만 상품에 연결할 수 있다.
 */
public interface BrandService {

    Brand create(BrandCreateCommand command);

    Brand update(Long brandId, BrandUpdateCommand command);

    void delete(Long brandId);

    Brand activate(Long brandId);

    Brand deactivate(Long brandId, String reason);

    Brand findById(Long brandId);

    Optional<Brand> findBySlug(String slug);

    List<Brand> listAll();

    List<Brand> listActive();

    List<Brand> findByCountry(String country);

    record BrandCreateCommand(
        String name,
        String slug,
        String countryCode,
        String description,
        String logoUrl,
        String websiteUrl
    ) {}

    record BrandUpdateCommand(
        String name,
        String description,
        String logoUrl,
        String websiteUrl
    ) {}

    @Slf4j
    @Service
    @RequiredArgsConstructor
    class Impl implements BrandService {

        private final BrandRepository brandRepository;

        @Override
        @Transactional
        @CacheEvict(value = "brand", allEntries = true)
        public Brand create(BrandCreateCommand command) {
            validate(command.name(), command.slug());
            if (brandRepository.existsBySlug(command.slug()))
                throw new DuplicateResourceException("이미 사용 중인 슬러그: " + command.slug());
            var brand = Brand.create(
                command.name(),
                command.slug(),
                command.countryCode(),
                command.description(),
                command.logoUrl(),
                command.websiteUrl()
            );
            return brandRepository.save(brand);
        }

        @Override
        @Transactional
        @CacheEvict(value = "brand", key = "#brandId")
        public Brand update(Long brandId, BrandUpdateCommand command) {
            var brand = load(brandId);
            brand.update(command.name(), command.description(), command.logoUrl(), command.websiteUrl());
            return brandRepository.save(brand);
        }

        @Override
        @Transactional
        @CacheEvict(value = "brand", key = "#brandId")
        public void delete(Long brandId) {
            var brand = load(brandId);
            if (brand.getProductCount() > 0)
                throw new InvalidStateException("연결된 상품이 있는 브랜드는 삭제할 수 없습니다.");
            brandRepository.delete(brand);
            log.info("브랜드 삭제: id={}", brandId);
        }

        @Override
        @Transactional
        @CacheEvict(value = "brand", key = "#brandId")
        public Brand activate(Long brandId) {
            var brand = load(brandId);
            brand.activate();
            return brandRepository.save(brand);
        }

        @Override
        @Transactional
        @CacheEvict(value = "brand", key = "#brandId")
        public Brand deactivate(Long brandId, String reason) {
            var brand = load(brandId);
            brand.deactivate(reason);
            return brandRepository.save(brand);
        }

        @Override
        @Cacheable(value = "brand", key = "#brandId")
        public Brand findById(Long brandId) {
            return load(brandId);
        }

        @Override
        public Optional<Brand> findBySlug(String slug) {
            return brandRepository.findBySlug(slug);
        }

        @Override
        @Cacheable(value = "brand", key = "'all'")
        public List<Brand> listAll() {
            return brandRepository.findAll().stream()
                .sorted(Comparator.comparing(Brand::getName))
                .toList();
        }

        @Override
        @Cacheable(value = "brand", key = "'active'")
        public List<Brand> listActive() {
            return brandRepository.findAll().stream()
                .filter(Brand::isActive)
                .sorted(Comparator.comparing(Brand::getName))
                .toList();
        }

        @Override
        public List<Brand> findByCountry(String country) {
            return brandRepository.findAll().stream()
                .filter(b -> country != null && country.equalsIgnoreCase(b.getCountryCode()))
                .sorted(Comparator.comparing(Brand::getName))
                .toList();
        }

        // helpers
        private Brand load(Long brandId) {
            return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("브랜드를 찾을 수 없습니다: " + brandId));
        }

        private void validate(String name, String slug) {
            if (name == null || name.isBlank()) throw new InvalidStateException("브랜드명은 필수");
            if (slug == null || !slug.matches("^[a-z0-9\\-]{1,60}$"))
                throw new InvalidStateException("슬러그는 소문자/숫자/하이픈만 허용 (1~60자)");
        }
    }
}
