package com.springshop.domain.product;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

/**
 * 브랜드 엔티티.
 *
 * <p>상품의 제조사/브랜드 정보를 보관한다. 활성 브랜드만 상품 등록 시 선택 가능하다.
 * 브랜드명은 시스템 내 유니크하다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "brands",
        uniqueConstraints = @UniqueConstraint(name = "uk_brand_name", columnNames = "name")
)
public class Brand extends BaseEntity {

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium = false;

    @Column(name = "product_count", nullable = false)
    private long productCount = 0L;

    @Column(name = "founded_year")
    private Integer foundedYear;

    protected Brand() {
        super();
    }

    public Brand(String name, String country) {
        super();
        this.name = validateName(name);
        this.country = country;
    }

    public static Brand of(String name, String country) {
        return new Brand(name, country);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("브랜드명은 필수입니다");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("브랜드명은 100자 이하여야 합니다");
        }
        return name.trim();
    }

    public String getName() {
        return name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getCountry() {
        return country;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public long getProductCount() {
        return productCount;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public void rename(String newName) {
        this.name = validateName(newName);
    }

    public void updateLogo(String url) {
        if (url != null && url.length() > 500) {
            throw new IllegalArgumentException("로고 URL은 500자 이하");
        }
        this.logoUrl = url;
    }

    public void updateDescription(String desc) {
        this.description = desc;
    }

    public void updateCountry(String country) {
        this.country = country;
    }

    public void updateWebsite(String url) {
        this.websiteUrl = url;
    }

    public void setFoundedYear(Integer year) {
        if (year != null && (year < 1500 || year > java.time.Year.now().getValue())) {
            throw new IllegalArgumentException("설립 연도가 유효하지 않습니다: " + year);
        }
        this.foundedYear = year;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void promoteToPremium() {
        this.isPremium = true;
    }

    public void demoteFromPremium() {
        this.isPremium = false;
    }

    public void incrementProductCount() {
        this.productCount++;
    }

    public void decrementProductCount() {
        if (this.productCount > 0) this.productCount--;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Brand other)) return false;
        if (getId() != null && other.getId() != null) {
            return Objects.equals(getId(), other.getId());
        }
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), name);
    }
}
