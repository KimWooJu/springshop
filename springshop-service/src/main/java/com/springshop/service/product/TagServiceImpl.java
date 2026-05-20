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
 * 태그 관리 보조 구현체.
 *
 * <p>기본 CRUD는 {@link TagService.Impl}이 담당. 본 구현체의 책임:
 * <ul>
 *   <li>태그 이름 정규화 (소문자, 공백 → 하이픈)</li>
 *   <li>인기 태그 조회 (사용 횟수 기준)</li>
 *   <li>자동완성 제안 (prefix 매칭)</li>
 *   <li>태그 사용 카운트 증감</li>
 *   <li>일괄 등록 / 일괄 삭제</li>
 * </ul>
 */
@Slf4j
@Service("tagServiceExtension")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl {

    /**
     * 태그 이름 정규식 (정규화 후).
     */
    private static final Pattern VALID_TAG = Pattern.compile("^[a-z0-9가-힣\\-]{1,50}$");

    /**
     * 정규화 시 변환 대상 (공백, 언더스코어).
     */
    private static final Pattern NORMALIZE_TARGET = Pattern.compile("[\\s_]+");

    private final TagRepository tagRepository;

    /**
     * 태그 생성.
     */
    @Transactional
    @CacheEvict(value = {"tag.popular", "tag.all"}, allEntries = true)
    public Tag create(String name) {
        var normalized = normalize(name);
        if (tagRepository.existsByName(normalized)) {
            throw new DuplicateResourceException("이미 존재하는 태그: " + normalized);
        }
        var tag = Tag.create(normalized);
        log.info("태그 생성: name={}", normalized);
        return tagRepository.save(tag);
    }

    /**
     * 태그 조회 또는 생성 (find or create).
     */
    @Transactional
    public Tag findOrCreate(String name) {
        var normalized = normalize(name);
        return tagRepository.findByName(normalized)
            .orElseGet(() -> {
                var tag = Tag.create(normalized);
                log.debug("태그 자동 생성: name={}", normalized);
                return tagRepository.save(tag);
            });
    }

    /**
     * 태그 일괄 등록 (중복 무시).
     */
    @Transactional
    @CacheEvict(value = {"tag.popular", "tag.all"}, allEntries = true)
    public List<Tag> findOrCreateAll(List<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        return names.stream()
            .filter(n -> n != null && !n.isBlank())
            .map(this::findOrCreate)
            .toList();
    }

    /**
     * 태그 이름 변경.
     */
    @Transactional
    @CacheEvict(value = {"tag.popular", "tag.all"}, allEntries = true)
    public Tag rename(Long tagId, String newName) {
        var tag = load(tagId);
        var normalized = normalize(newName);
        if (!normalized.equals(tag.getName())
            && tagRepository.existsByName(normalized)) {
            throw new DuplicateResourceException("이미 존재하는 태그: " + normalized);
        }
        tag.rename(normalized);
        return tagRepository.save(tag);
    }

    /**
     * 태그 삭제 (사용 중인 태그는 거부).
     */
    @Transactional
    @CacheEvict(value = {"tag.popular", "tag.all"}, allEntries = true)
    public void delete(Long tagId) {
        var tag = load(tagId);
        if (tag.getUsageCount() > 0) {
            throw new InvalidStateException(
                "사용 중인 태그는 삭제할 수 없습니다: usageCount=" + tag.getUsageCount());
        }
        tagRepository.delete(tag);
        log.info("태그 삭제: id={}", tagId);
    }

    /**
     * 인기 태그 (사용 횟수 기준 TOP N).
     */
    @Cacheable("tag.popular")
    public List<Tag> findPopular(int limit) {
        return tagRepository.findAll().stream()
            .sorted(Comparator.comparingLong(Tag::getUsageCount).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * 자동완성 제안 (prefix 매칭).
     */
    public List<Tag> suggest(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return List.of();
        var lc = prefix.toLowerCase();
        return tagRepository.findAll().stream()
            .filter(t -> t.getName() != null && t.getName().startsWith(lc))
            .sorted(Comparator.comparingLong(Tag::getUsageCount).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * 부분 일치 검색.
     */
    public List<Tag> searchByContains(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) return List.of();
        var lc = keyword.toLowerCase();
        return tagRepository.findAll().stream()
            .filter(t -> t.getName() != null && t.getName().contains(lc))
            .sorted(Comparator.comparingLong(Tag::getUsageCount).reversed())
            .limit(limit)
            .toList();
    }

    /**
     * 단건 조회.
     */
    public Tag findById(Long tagId) {
        return load(tagId);
    }

    /**
     * 이름으로 조회.
     */
    public Optional<Tag> findByName(String name) {
        return tagRepository.findByName(normalize(name));
    }

    /**
     * 전체 태그 (캐시).
     */
    @Cacheable("tag.all")
    public List<Tag> listAll() {
        return tagRepository.findAll().stream()
            .sorted(Comparator.comparing(Tag::getName))
            .toList();
    }

    /**
     * 태그 사용 카운트 증가 (상품 등록 시).
     */
    @Transactional
    public void incrementUsage(Long tagId) {
        tagRepository.findById(tagId).ifPresent(t -> {
            t.incrementUsage();
            tagRepository.save(t);
        });
    }

    /**
     * 태그 사용 카운트 감소 (상품 삭제 시).
     */
    @Transactional
    public void decrementUsage(Long tagId) {
        tagRepository.findById(tagId).ifPresent(t -> {
            t.decrementUsage();
            tagRepository.save(t);
        });
    }

    // ---- helpers ----

    private Tag load(Long tagId) {
        return tagRepository.findById(tagId)
            .orElseThrow(() -> new ResourceNotFoundException("태그를 찾을 수 없습니다: " + tagId));
    }

    private String normalize(String raw) {
        if (raw == null) {
            throw new InvalidStateException("태그 이름은 필수입니다.");
        }
        var trimmed = raw.trim().toLowerCase();
        var hyphenated = NORMALIZE_TARGET.matcher(trimmed).replaceAll("-");
        if (hyphenated.isEmpty() || hyphenated.length() > 50) {
            throw new InvalidStateException("태그 이름은 1~50자여야 합니다.");
        }
        if (!VALID_TAG.matcher(hyphenated).matches()) {
            throw new InvalidStateException(
                "태그 이름은 영문/숫자/한글/하이픈만 허용: " + hyphenated);
        }
        return hyphenated;
    }
}
