package com.springshop.domain.product;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 상품 태그 엔티티.
 *
 * <p>전 상품을 통틀어 사용되는 표준 태그를 관리한다. 사용 횟수 통계를 유지하여
 * 인기 태그 노출에 사용한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_tag_slug", columnNames = "slug")
)
public class Tag extends BaseEntity {

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "slug", length = 60, nullable = false)
    private String slug;

    @Column(name = "use_count", nullable = false)
    private long useCount = 0L;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @Column(name = "color_hex", length = 7)
    private String colorHex;

    protected Tag() {
        super();
    }

    public Tag(String name) {
        super();
        this.name = validateName(name);
        this.slug = toSlug(name);
    }

    public static Tag of(String name) {
        return new Tag(name);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("태그명은 비어있을 수 없습니다");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("태그명은 50자 이하");
        }
        return trimmed;
    }

    private static String toSlug(String name) {
        return name.trim().toLowerCase()
                .replaceAll("[^a-z0-9가-힣]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public long getUseCount() {
        return useCount;
    }

    public boolean isFeatured() {
        return isFeatured;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void incrementUseCount() {
        this.useCount++;
    }

    public void decrementUseCount() {
        if (this.useCount > 0) this.useCount--;
    }

    public void markFeatured() {
        this.isFeatured = true;
    }

    public void unmarkFeatured() {
        this.isFeatured = false;
    }

    public void changeColor(String hex) {
        if (hex != null && !hex.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("색상은 #RRGGBB 형식이어야 합니다: " + hex);
        }
        this.colorHex = hex;
    }

    public void rename(String newName) {
        this.name = validateName(newName);
        this.slug = toSlug(newName);
    }

    /**
     * 인기 태그 판정 기준 (사용 횟수 >= 100).
     */
    public boolean isPopular() {
        return this.useCount >= 100;
    }

    public static Tag create(String name) { return of(name); }
    public long getUsageCount() { return useCount; }
    public void incrementUsage() { incrementUseCount(); }
    public void decrementUsage() { decrementUseCount(); }
}
