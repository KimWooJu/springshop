package com.springshop.service.product;

import com.springshop.domain.product.Tag;
import com.springshop.domain.product.TagRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 상품 태그 서비스. 태그는 사용자 검색/필터/추천의 핵심 메타데이터.
 *
 * <p>태그 이름은 정규화(소문자, 공백 → 하이픈)되어 저장되며,
 * 동일 이름은 단일 인스턴스로 유지된다.
 */
public interface TagService {

    Tag create(String name);

    Tag findOrCreate(String name);

    Tag rename(Long tagId, String newName);

    void delete(Long tagId);

    Tag findById(Long tagId);

    Optional<Tag> findByName(String name);

    List<Tag> listAll();

    List<Tag> listPopular(int limit);

    /** 부분 일치 자동완성 */
    List<Tag> suggest(String prefix, int limit);

    /** 상품 등록/수정시 호출되는 태그 카운트 증가 */
    void incrementUsage(Long tagId);

    /** 상품 삭제 시 호출되는 태그 카운트 감소 */
    void decrementUsage(Long tagId);

    @Slf4j
    @Service
    @RequiredArgsConstructor
    class Impl implements TagService {

        private static final Pattern INVALID_CHARS = Pattern.compile("[\\s_]+");

        private final TagRepository tagRepository;

        @Override
        @Transactional
        @CacheEvict(value = "tag", allEntries = true)
        public Tag create(String name) {
            var normalized = normalize(name);
            if (tagRepository.existsByName(normalized))
                throw new DuplicateResourceException("이미 존재하는 태그: " + normalized);
            return tagRepository.save(Tag.create(normalized));
        }

        @Override
        @Transactional
        public Tag findOrCreate(String name) {
            var normalized = normalize(name);
            return tagRepository.findByName(normalized).orElseGet(() ->
                tagRepository.save(Tag.create(normalized))
            );
        }

        @Override
        @Transactional
        @CacheEvict(value = "tag", allEntries = true)
        public Tag rename(Long tagId, String newName) {
            var tag = load(tagId);
            var normalized = normalize(newName);
            if (!normalized.equals(tag.getName()) && tagRepository.existsByName(normalized))
                throw new DuplicateResourceException("이미 존재하는 태그: " + normalized);
            tag.rename(normalized);
            return tagRepository.save(tag);
        }

        @Override
        @Transactional
        @CacheEvict(value = "tag", allEntries = true)
        public void delete(Long tagId) {
            var tag = load(tagId);
            if (tag.getUsageCount() > 0)
                throw new InvalidStateException("사용 중인 태그는 삭제할 수 없습니다.");
            tagRepository.delete(tag);
        }

        @Override
        @Cacheable(value = "tag", key = "#tagId")
        public Tag findById(Long tagId) {
            return load(tagId);
        }

        @Override
        public Optional<Tag> findByName(String name) {
            return tagRepository.findByName(normalize(name));
        }

        @Override
        @Cacheable(value = "tag", key = "'all'")
        public List<Tag> listAll() {
            return tagRepository.findAll().stream()
                .sorted(Comparator.comparing(Tag::getName))
                .toList();
        }

        @Override
        public List<Tag> listPopular(int limit) {
            return tagRepository.findAll().stream()
                .sorted(Comparator.comparingLong(Tag::getUsageCount).reversed())
                .limit(limit)
                .toList();
        }

        @Override
        public List<Tag> suggest(String prefix, int limit) {
            if (prefix == null || prefix.isBlank()) return List.of();
            var lc = prefix.toLowerCase();
            return tagRepository.findAll().stream()
                .filter(t -> t.getName() != null && t.getName().startsWith(lc))
                .sorted(Comparator.comparingLong(Tag::getUsageCount).reversed())
                .limit(limit)
                .toList();
        }

        @Override
        @Transactional
        public void incrementUsage(Long tagId) {
            tagRepository.findById(tagId).ifPresent(t -> {
                t.incrementUsage();
                tagRepository.save(t);
            });
        }

        @Override
        @Transactional
        public void decrementUsage(Long tagId) {
            tagRepository.findById(tagId).ifPresent(t -> {
                t.decrementUsage();
                tagRepository.save(t);
            });
        }

        // helpers
        private Tag load(Long tagId) {
            return tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("태그를 찾을 수 없습니다: " + tagId));
        }

        private String normalize(String raw) {
            if (raw == null) throw new InvalidStateException("태그 이름은 필수");
            var trimmed = raw.trim().toLowerCase();
            var hyphenated = INVALID_CHARS.matcher(trimmed).replaceAll("-");
            if (hyphenated.isEmpty() || hyphenated.length() > 50)
                throw new InvalidStateException("태그 이름은 1~50자");
            return hyphenated;
        }
    }
}
