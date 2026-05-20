package com.springshop.domain.product;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 상품 카테고리 엔티티 (계층 구조).
 *
 * <p>self-join 대신 parentId(Long) 와 fullPath("/전자제품/노트북/게이밍노트북") 형태의
 * 경로 문자열로 계층을 표현한다. 깊이(level)와 경로를 함께 보관하여 트리 쿼리를
 * 효율화한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_category_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_category_parent", columnList = "parent_id"),
                @Index(name = "idx_category_path", columnList = "full_path")
        }
)
public class Category extends BaseEntity {

    public static final String PATH_SEPARATOR = "/";
    public static final int MAX_DEPTH = 5;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "slug", length = 120, nullable = false)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "full_path", length = 500, nullable = false)
    private String fullPath;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "product_count", nullable = false)
    private long productCount = 0L;

    protected Category() {
        super();
    }

    public Category(String name, String slug, Long parentId, int level, String parentPath) {
        super();
        this.name = validateName(name);
        this.slug = validateSlug(slug);
        this.parentId = parentId;
        this.level = level;
        if (level < 1 || level > MAX_DEPTH) {
            throw new IllegalArgumentException("카테고리 깊이는 1~%d 사이: %d".formatted(MAX_DEPTH, level));
        }
        this.fullPath = buildPath(parentPath, name);
    }

    public static Category root(String name, String slug) {
        return new Category(name, slug, null, 1, "");
    }

    public static Category child(String name, String slug, Category parent) {
        Objects.requireNonNull(parent, "부모 카테고리는 필수");
        return new Category(name, slug, parent.getId(), parent.level + 1, parent.fullPath);
    }

    private static String buildPath(String parentPath, String name) {
        if (parentPath == null || parentPath.isBlank()) {
            return PATH_SEPARATOR + name;
        }
        return parentPath + PATH_SEPARATOR + name;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름 필수");
        }
        if (name.contains(PATH_SEPARATOR)) {
            throw new IllegalArgumentException("이름에 '%s' 사용 불가".formatted(PATH_SEPARATOR));
        }
        return name.trim();
    }

    private static String validateSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("슬러그 필수");
        }
        if (!slug.matches("^[a-z0-9-]+$")) {
            throw new IllegalArgumentException("슬러그는 소문자/숫자/하이픈만 허용: " + slug);
        }
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public Long getParentId() {
        return parentId;
    }

    public int getLevel() {
        return level;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public long getProductCount() {
        return productCount;
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public boolean isLeaf() {
        // 실제로는 자식 존재 여부를 Repository에서 조회해야 하나, 빠른 휴리스틱 제공.
        return productCount > 0;
    }

    /**
     * 경로상의 조상 카테고리 이름 목록 반환(자기 자신 제외).
     */
    public List<String> getAncestorNames() {
        if (fullPath == null || fullPath.isBlank()) return List.of();
        String[] parts = fullPath.split(PATH_SEPARATOR);
        // parts[0] 은 빈 문자열(앞에 / 가 있으므로)
        return Arrays.stream(parts)
                .filter(s -> !s.isBlank())
                .limit(Math.max(0, level - 1))
                .collect(Collectors.toList());
    }

    public void rename(String newName) {
        this.name = validateName(newName);
        // 부모 경로를 유지한 채 끝부분만 교체
        int lastSep = fullPath.lastIndexOf(PATH_SEPARATOR);
        if (lastSep >= 0) {
            this.fullPath = fullPath.substring(0, lastSep + 1) + newName;
        }
    }

    public void changeSlug(String newSlug) {
        this.slug = validateSlug(newSlug);
    }

    public void updateDescription(String desc) {
        this.description = desc;
    }

    public void changeOrder(int order) {
        this.displayOrder = order;
    }

    public void updateIcon(String url) {
        this.iconUrl = url;
    }

    public void detachParent() {
        this.parentId = null;
        this.level = 1;
        this.fullPath = PATH_SEPARATOR + this.name;
    }

    public void moveTo(Category newParent) {
        Objects.requireNonNull(newParent, "newParent 필수");
        this.parentId = newParent.getId();
        this.level = newParent.level + 1;
        if (this.level > MAX_DEPTH) {
            throw new IllegalArgumentException("최대 깊이 초과: " + this.level);
        }
        this.fullPath = buildPath(newParent.fullPath, this.name);
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void incrementProductCount() {
        this.productCount++;
    }

    public void decrementProductCount() {
        if (this.productCount > 0) this.productCount--;
    }
}
