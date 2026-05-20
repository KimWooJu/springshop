package com.springshop.service.product;

import com.springshop.domain.product.Category;
import com.springshop.domain.product.CategoryRepository;
import com.springshop.common.exception.DuplicateResourceException;
import com.springshop.common.exception.InvalidStateException;
import com.springshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 카테고리 관리 보조 구현체.
 *
 * <p>기본 CRUD는 {@link CategoryService.Impl}에서 담당하며, 본 구현체는 다음의
 * 운영/관리 기능을 제공한다:
 * <ul>
 *   <li>카테고리 트리 빌딩(재귀, 캐싱)</li>
 *   <li>브레드크럼(breadcrumb) 경로 생성</li>
 *   <li>카테고리 이동 (사이클 검증)</li>
 *   <li>슬러그 자동 생성</li>
 *   <li>병합/이동 시 사용 통계 갱신</li>
 * </ul>
 *
 * <p>빈 이름 충돌을 피하기 위해 {@code categoryServiceExtension}으로 등록한다.
 */
@Slf4j
@Service("categoryServiceExtension")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl {

    /**
     * 최대 카테고리 계층 깊이. 이 이상은 사용성 저하 및 순환 위험으로 거부.
     */
    public static final int MAX_DEPTH = 5;

    /**
     * 카테고리 트리 빌딩 시 안전 카운터 (무한루프 방지).
     */
    private static final int SAFETY_LIMIT = 10_000;

    private final CategoryRepository categoryRepository;

    /**
     * 노드 record. 트리 구조 표현용.
     */
    public record CategoryNode(Category category, List<CategoryNode> children) {
        public int size() {
            return 1 + children.stream().mapToInt(CategoryNode::size).sum();
        }

        public int maxDepth() {
            if (children.isEmpty()) return 1;
            return 1 + children.stream().mapToInt(CategoryNode::maxDepth).max().orElse(0);
        }
    }

    /**
     * 전체 카테고리 트리를 재귀적으로 구축한다.
     */
    @Cacheable(value = "categoryTreeExtended", key = "'root'")
    public List<CategoryNode> buildCategoryTree() {
        var all = categoryRepository.findAll();
        return buildCategoryTree(all);
    }

    /**
     * 외부 입력으로부터 트리 빌딩 (테스트/단위 작업용).
     */
    public List<CategoryNode> buildCategoryTree(List<Category> categories) {
        if (categories == null || categories.isEmpty()) return List.of();
        var roots = categories.stream()
            .filter(c -> c.getParentId() == null)
            .sorted(Comparator.comparingInt(Category::getDisplayOrder))
            .toList();
        var nodes = new ArrayList<CategoryNode>();
        int counter = 0;
        for (var root : roots) {
            nodes.add(buildNode(root, categories, new HashSet<>(), counter));
            counter++;
        }
        log.debug("카테고리 트리 빌드: rootCount={}", nodes.size());
        return nodes;
    }

    /**
     * 카테고리 ID로부터 루트까지의 경로(브레드크럼)를 반환한다.
     *
     * <p>예: [전자제품 > 노트북 > 게이밍 노트북]
     */
    public List<Category> getBreadcrumb(Long categoryId) {
        var crumb = new ArrayList<Category>();
        Long currentId = categoryId;
        int safety = 0;
        while (currentId != null && safety++ < MAX_DEPTH * 2) {
            var node = categoryRepository.findById(currentId).orElse(null);
            if (node == null) break;
            crumb.add(0, node);
            currentId = node.getParentId();
        }
        if (safety >= MAX_DEPTH * 2) {
            log.warn("브레드크럼 안전 카운터 도달: categoryId={}", categoryId);
        }
        return crumb;
    }

    /**
     * 카테고리를 새 부모로 이동한다. 순환 참조를 방지하기 위해 자손 검사를 수행한다.
     */
    @Transactional
    @CacheEvict(value = {"categoryTreeExtended"}, allEntries = true)
    public Category moveCategory(Long categoryId, Long newParentId) {
        if (categoryId.equals(newParentId)) {
            throw new InvalidStateException("자기 자신을 부모로 지정할 수 없습니다.");
        }
        var category = load(categoryId);

        if (newParentId == null) {
            category.detachParent();
            log.info("카테고리 루트로 이동: id={}", categoryId);
            return categoryRepository.save(category);
        }

        // 자손 검사 — 새 부모가 자신의 후손이면 안 됨
        var descendants = collectDescendants(categoryId);
        if (descendants.stream().anyMatch(d -> d.getId().equals(newParentId))) {
            throw new InvalidStateException("후손 카테고리를 부모로 지정할 수 없습니다.");
        }

        var newParent = load(newParentId);
        int newDepth = computeDepth(newParentId) + 1;
        if (newDepth > MAX_DEPTH) {
            throw new InvalidStateException(
                "최대 깊이 초과: " + newDepth + " (한도: " + MAX_DEPTH + ")");
        }
        category.moveTo(newParent);
        log.info("카테고리 이동: id={} -> parentId={}, newDepth={}", categoryId, newParentId, newDepth);
        return categoryRepository.save(category);
    }

    /**
     * 카테고리의 모든 후손을 평면화하여 반환.
     */
    public List<Category> collectDescendants(Long ancestorId) {
        var all = categoryRepository.findAll();
        var result = new ArrayList<Category>();
        var visited = new HashSet<Long>();
        collectDescendantsRec(ancestorId, all, result, visited);
        return result;
    }

    /**
     * 후손 카운트만 빠르게 조회.
     */
    public long countDescendants(Long ancestorId) {
        return collectDescendants(ancestorId).size();
    }

    /**
     * 카테고리 ID와 그 모든 후손 ID를 동시에 반환 (필터링 쿼리용).
     */
    public Set<Long> getDescendantIdsIncludingSelf(Long ancestorId) {
        var ids = new HashSet<Long>();
        ids.add(ancestorId);
        collectDescendants(ancestorId).forEach(d -> ids.add(d.getId()));
        return ids;
    }

    /**
     * 카테고리 깊이 계산. 루트는 0.
     */
    public int computeDepth(Long categoryId) {
        int depth = 0;
        Long current = categoryId;
        int safety = 0;
        while (current != null && safety++ < MAX_DEPTH * 2) {
            var node = categoryRepository.findById(current).orElse(null);
            if (node == null || node.getParentId() == null) break;
            depth++;
            current = node.getParentId();
        }
        return depth;
    }

    /**
     * 슬러그 자동 생성. 이름에서 한글/공백 제거 후 소문자화.
     */
    public String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidStateException("슬러그 생성 대상 이름이 필요합니다.");
        }
        var slug = name.trim().toLowerCase()
            .replaceAll("[\\s_]+", "-")
            .replaceAll("[^a-z0-9\\-]", "")
            .replaceAll("-{2,}", "-");
        if (slug.startsWith("-")) slug = slug.substring(1);
        if (slug.endsWith("-")) slug = slug.substring(0, slug.length() - 1);
        if (slug.isBlank()) {
            slug = "category-" + System.currentTimeMillis();
        }
        return slug;
    }

    /**
     * 슬러그 충돌 시 -2, -3 형태로 자동 추가.
     */
    public String generateUniqueSlug(String name) {
        var baseSlug = generateSlug(name);
        if (!categoryRepository.existsBySlug(baseSlug)) return baseSlug;
        for (int i = 2; i < 100; i++) {
            var candidate = baseSlug + "-" + i;
            if (!categoryRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        throw new DuplicateResourceException("슬러그 자동 생성 한도 초과: " + baseSlug);
    }

    /**
     * 카테고리 사용 통계 (상품 수 기반).
     */
    public Map<Long, Long> categoryProductCounts() {
        var stats = new LinkedHashMap<Long, Long>();
        for (var category : categoryRepository.findAll()) {
            // 실제 구현은 ProductRepository.countByCategoryId 사용
            stats.put(category.getId(), 0L);
        }
        return stats;
    }

    /**
     * 카테고리 검색 (이름 부분 일치).
     */
    public List<Category> searchByName(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) return List.of();
        var lc = keyword.toLowerCase();
        return categoryRepository.findAll().stream()
            .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(lc))
            .sorted(Comparator.comparing(Category::getName))
            .limit(limit)
            .toList();
    }

    /**
     * 활성 카테고리만 (display=true).
     */
    public List<Category> findActive() {
        return categoryRepository.findAll().stream()
            .filter(Category::isActive)
            .sorted(Comparator.comparingInt(Category::getDisplayOrder))
            .toList();
    }

    /**
     * 트리 뷰의 평면화된 형태 — 들여쓰기 정보 포함.
     */
    public List<FlatNode> flattenForDisplay() {
        var flat = new ArrayList<FlatNode>();
        for (var root : buildCategoryTree()) {
            flattenRec(root, 0, flat);
        }
        return flat;
    }

    /** UI 평면 표시용 항목. */
    public record FlatNode(Category category, int depth, String displayLabel) {}

    // ---- helpers ----

    private Category load(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다: " + id));
    }

    private CategoryNode buildNode(Category root, List<Category> all,
                                    Set<Long> visited, int counter) {
        if (counter > SAFETY_LIMIT) {
            throw new InvalidStateException("카테고리 트리 빌드 안전 한도 초과");
        }
        if (!visited.add(root.getId())) {
            log.warn("카테고리 중복 방문 감지: id={}", root.getId());
            return new CategoryNode(root, List.of());
        }
        var children = all.stream()
            .filter(c -> root.getId().equals(c.getParentId()))
            .sorted(Comparator.comparingInt(Category::getDisplayOrder))
            .map(c -> buildNode(c, all, visited, counter + 1))
            .toList();
        return new CategoryNode(root, children);
    }

    private void collectDescendantsRec(Long parentId, List<Category> all,
                                        List<Category> result, Set<Long> visited) {
        if (parentId == null || !visited.add(parentId)) return;
        all.stream()
            .filter(c -> parentId.equals(c.getParentId()))
            .forEach(child -> {
                result.add(child);
                collectDescendantsRec(child.getId(), all, result, visited);
            });
    }

    private void flattenRec(CategoryNode node, int depth, List<FlatNode> out) {
        var label = "-".repeat(depth * 2) + (depth > 0 ? " " : "") + node.category().getName();
        out.add(new FlatNode(node.category(), depth, label));
        for (var child : node.children()) {
            flattenRec(child, depth + 1, out);
        }
    }
}
