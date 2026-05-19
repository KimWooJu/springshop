package com.springshop.service.product;

import com.springshop.domain.product.Category;
import com.springshop.domain.product.CategoryRepository;
import com.springshop.domain.common.exception.DuplicateResourceException;
import com.springshop.domain.common.exception.InvalidStateException;
import com.springshop.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 카테고리 트리 관리 서비스.
 *
 * <p>중첩된 카테고리 트리를 생성/조회/이동/병합하며,
 * 카테고리 조회는 캐시 적용으로 빈번한 검색 부하를 줄인다.
 */
public interface CategoryService {

    Category createRoot(String name, String slug);

    Category createChild(Long parentId, String name, String slug);

    Category rename(Long categoryId, String newName);

    Category changeSlug(Long categoryId, String newSlug);

    void delete(Long categoryId);

    Category move(Long categoryId, Long newParentId);

    Category findById(Long categoryId);

    Category findBySlug(String slug);

    /** 전체 트리 조회 (루트부터 재귀) */
    List<CategoryNode> getTree();

    /** 특정 카테고리의 자식만 (1뎁스) */
    List<Category> getDirectChildren(Long parentId);

    /** 특정 카테고리의 모든 후손 (재귀) */
    List<Category> getAllDescendants(Long ancestorId);

    /** 경로(breadcrumb) 조회 */
    List<Category> getPath(Long categoryId);

    /** 카테고리 노드(자식 포함) */
    record CategoryNode(Category category, List<CategoryNode> children) {}

    @Slf4j
    @Service
    @RequiredArgsConstructor
    class Impl implements CategoryService {

        private final CategoryRepository categoryRepository;

        @Override
        @Transactional
        @CacheEvict(value = "categoryTree", allEntries = true)
        public Category createRoot(String name, String slug) {
            validate(name, slug);
            if (categoryRepository.existsBySlug(slug))
                throw new DuplicateResourceException("이미 사용 중인 슬러그: " + slug);
            var category = Category.createRoot(name, slug);
            return categoryRepository.save(category);
        }

        @Override
        @Transactional
        @CacheEvict(value = "categoryTree", allEntries = true)
        public Category createChild(Long parentId, String name, String slug) {
            validate(name, slug);
            var parent = loadCategory(parentId);
            if (categoryRepository.existsBySlug(slug))
                throw new DuplicateResourceException("이미 사용 중인 슬러그: " + slug);
            var category = Category.createChild(parent, name, slug);
            return categoryRepository.save(category);
        }

        @Override
        @Transactional
        @CacheEvict(value = "categoryTree", allEntries = true)
        public Category rename(Long categoryId, String newName) {
            var category = loadCategory(categoryId);
            category.rename(newName);
            return categoryRepository.save(category);
        }

        @Override
        @Transactional
        @CacheEvict(value = "categoryTree", allEntries = true)
        public Category changeSlug(Long categoryId, String newSlug) {
            var category = loadCategory(categoryId);
            if (!newSlug.equals(category.getSlug()) && categoryRepository.existsBySlug(newSlug))
                throw new DuplicateResourceException("이미 사용 중인 슬러그: " + newSlug);
            category.changeSlug(newSlug);
            return categoryRepository.save(category);
        }

        @Override
        @Transactional
        @CacheEvict(value = "categoryTree", allEntries = true)
        public void delete(Long categoryId) {
            var category = loadCategory(categoryId);
            var descendants = getAllDescendants(categoryId);
            if (!descendants.isEmpty()) {
                throw new InvalidStateException(
                    "하위 카테고리가 있는 카테고리는 삭제할 수 없습니다. 자식 수: " + descendants.size()
                );
            }
            categoryRepository.delete(category);
            log.info("카테고리 삭제: id={}", categoryId);
        }

        @Override
        @Transactional
        @CacheEvict(value = "categoryTree", allEntries = true)
        public Category move(Long categoryId, Long newParentId) {
            var category = loadCategory(categoryId);
            if (newParentId == null) {
                category.detachParent();
                return categoryRepository.save(category);
            }
            if (categoryId.equals(newParentId))
                throw new InvalidStateException("자기 자신을 부모로 지정할 수 없습니다.");

            // 사이클 검사: 새 부모가 자기 자신의 후손이어선 안 됨
            var descendants = getAllDescendants(categoryId);
            if (descendants.stream().anyMatch(d -> d.getId().equals(newParentId))) {
                throw new InvalidStateException("후손 카테고리를 부모로 지정할 수 없습니다.");
            }
            var newParent = loadCategory(newParentId);
            category.moveTo(newParent);
            return categoryRepository.save(category);
        }

        @Override
        @Cacheable(value = "category", key = "#categoryId")
        public Category findById(Long categoryId) {
            return loadCategory(categoryId);
        }

        @Override
        public Category findBySlug(String slug) {
            return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("슬러그를 찾을 수 없습니다: " + slug));
        }

        @Override
        @Cacheable(value = "categoryTree", key = "'root'")
        public List<CategoryNode> getTree() {
            var all = categoryRepository.findAll();
            var roots = all.stream().filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .toList();
            return roots.stream().map(r -> buildNode(r, all)).toList();
        }

        @Override
        public List<Category> getDirectChildren(Long parentId) {
            return categoryRepository.findAll().stream()
                .filter(c -> parentId != null && parentId.equals(c.getParentId()))
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .toList();
        }

        @Override
        public List<Category> getAllDescendants(Long ancestorId) {
            var all = categoryRepository.findAll();
            var result = new ArrayList<Category>();
            var visited = new HashSet<Long>();
            collectDescendants(ancestorId, all, result, visited);
            return result;
        }

        @Override
        public List<Category> getPath(Long categoryId) {
            var path = new ArrayList<Category>();
            Long currentId = categoryId;
            int safetyCounter = 0;
            while (currentId != null && safetyCounter++ < 50) {
                var node = categoryRepository.findById(currentId).orElse(null);
                if (node == null) break;
                path.add(0, node);
                currentId = node.getParentId();
            }
            return path;
        }

        // ---- helpers ----

        private Category loadCategory(Long id) {
            return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다: " + id));
        }

        private CategoryNode buildNode(Category root, List<Category> all) {
            var children = all.stream()
                .filter(c -> root.getId().equals(c.getParentId()))
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .map(c -> buildNode(c, all))
                .toList();
            return new CategoryNode(root, children);
        }

        private void collectDescendants(Long parentId, List<Category> all,
                                         List<Category> result, Set<Long> visited) {
            if (parentId == null || !visited.add(parentId)) return;
            all.stream()
                .filter(c -> parentId.equals(c.getParentId()))
                .forEach(child -> {
                    result.add(child);
                    collectDescendants(child.getId(), all, result, visited);
                });
        }

        private void validate(String name, String slug) {
            if (name == null || name.isBlank()) throw new InvalidStateException("이름은 필수");
            if (slug == null || !slug.matches("^[a-z0-9\\-]{1,80}$"))
                throw new InvalidStateException("슬러그는 소문자/숫자/하이픈만 허용 (1~80자)");
        }
    }
}
