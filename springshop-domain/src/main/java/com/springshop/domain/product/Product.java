package com.springshop.domain.product;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 상품 엔티티 (애그리거트 루트).
 *
 * <p>상품의 기본 정보(이름, 설명, 가격, 카테고리/브랜드), 통계(조회수, 판매수, 평점),
 * 태그 및 상태를 관리한다. 가격은 BigDecimal + 통화 코드 컬럼으로 직접 저장한다.</p>
 *
 * <p>상태 전이는 비즈니스 메서드를 통해서만 가능하며, 변경 시 도메인 이벤트를
 * 발행한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_category", columnList = "category_id"),
                @Index(name = "idx_product_brand", columnList = "brand_id"),
                @Index(name = "idx_product_status", columnList = "status_label")
        }
)
public class Product extends BaseAuditEntity {

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "base_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "KRW";

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "status_label", length = 20, nullable = false)
    private String statusLabel = "DRAFT";

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt = LocalDateTime.now();

    @Column(name = "status_reason", length = 200)
    private String statusReason;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(name = "sold_count", nullable = false)
    private long soldCount = 0L;

    @Column(name = "rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "total_stock", nullable = false)
    private int totalStock = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag", length = 50)
    private Set<String> tags = new HashSet<>();

    protected Product() {
        super();
    }

    private Product(String name, String description, BigDecimal basePrice, String currency,
                    Long categoryId, Long brandId) {
        super();
        this.name = validateName(name);
        this.description = description;
        this.basePrice = validatePrice(basePrice);
        this.currency = (currency == null || currency.isBlank()) ? "KRW" : currency;
        this.categoryId = Objects.requireNonNull(categoryId, "카테고리는 필수입니다");
        this.brandId = brandId;
        this.statusLabel = "DRAFT";
        this.statusChangedAt = LocalDateTime.now();
        registerEvent(ProductEvents.ProductCreatedEvent.of(null, name, categoryId));
    }

    public static Product createDraft(String name, String description, BigDecimal basePrice,
                                      Long categoryId, Long brandId) {
        return new Product(name, description, basePrice, "KRW", categoryId, brandId);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("상품명은 200자 이하여야 합니다");
        }
        return name.trim();
    }

    private static BigDecimal validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다");
        }
        return price;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getBrandId() {
        return brandId;
    }

    public ProductStatus getStatus() {
        return switch (statusLabel) {
            case "DRAFT" -> new ProductStatus.Draft(statusChangedAt);
            case "UNDER_REVIEW" -> new ProductStatus.UnderReview(statusChangedAt);
            case "ACTIVE" -> new ProductStatus.Active(statusChangedAt);
            case "OUT_OF_STOCK" -> new ProductStatus.OutOfStock(statusChangedAt);
            case "DISCONTINUED" -> new ProductStatus.Discontinued(statusChangedAt);
            case "DELETED" -> new ProductStatus.Deleted(statusChangedAt, statusReason);
            default -> throw new IllegalStateException("알 수 없는 상품 상태: " + statusLabel);
        };
    }

    public long getViewCount() {
        return viewCount;
    }

    public long getSoldCount() {
        return soldCount;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public void submitForReview() {
        if (!"DRAFT".equals(statusLabel)) {
            throw new IllegalStateException("DRAFT 상태에서만 검토 요청 가능");
        }
        applyStatus("UNDER_REVIEW", null);
    }

    public void publish() {
        if (!"UNDER_REVIEW".equals(statusLabel) && !"OUT_OF_STOCK".equals(statusLabel)) {
            throw new IllegalStateException("검토 완료 또는 품절 상태에서만 게시 가능: " + statusLabel);
        }
        applyStatus("ACTIVE", null);
        registerEvent(ProductEvents.ProductPublishedEvent.of(getId(), name));
    }

    public void unpublish(String reason) {
        if (!"ACTIVE".equals(statusLabel)) {
            throw new IllegalStateException("ACTIVE 상태에서만 미게시 가능");
        }
        applyStatus("DRAFT", reason);
    }

    public void markOutOfStock() {
        if (!"ACTIVE".equals(statusLabel)) {
            return;
        }
        applyStatus("OUT_OF_STOCK", null);
        registerEvent(ProductEvents.ProductOutOfStockEvent.of(getId(), name));
    }

    public void discontinue(String reason) {
        applyStatus("DISCONTINUED", reason);
        registerEvent(ProductEvents.ProductDiscontinuedEvent.of(getId(), name, reason));
    }

    public void updatePrice(BigDecimal newPrice) {
        this.basePrice = validatePrice(newPrice);
    }

    public void updateBasicInfo(String name, String description) {
        this.name = validateName(name);
        this.description = description;
    }

    public void changeCategory(Long newCategoryId) {
        this.categoryId = Objects.requireNonNull(newCategoryId, "카테고리는 필수");
    }

    public void changeBrand(Long newBrandId) {
        this.brandId = newBrandId;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementSoldCount(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("판매 수량은 양수여야 합니다");
        this.soldCount += quantity;
    }

    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("태그는 비어있을 수 없습니다");
        }
        if (tags.size() >= 20) {
            throw new IllegalStateException("태그는 최대 20개까지");
        }
        this.tags.add(tag.trim().toLowerCase());
    }

    public void removeTag(String tag) {
        if (tag != null) this.tags.remove(tag.trim().toLowerCase());
    }

    public void updateStock(int newTotalStock) {
        if (newTotalStock < 0) throw new IllegalArgumentException("재고는 0 이상");
        this.totalStock = newTotalStock;
        if (newTotalStock == 0 && "ACTIVE".equals(statusLabel)) {
            markOutOfStock();
        } else if (newTotalStock > 0 && "OUT_OF_STOCK".equals(statusLabel)) {
            publish();
        }
    }

    public void updateRating(BigDecimal newRating, int newReviewCount) {
        if (newRating == null || newRating.compareTo(BigDecimal.ZERO) < 0 || newRating.compareTo(new BigDecimal("5")) > 0) {
            throw new IllegalArgumentException("평점은 0~5 사이여야 합니다");
        }
        this.rating = newRating;
        this.reviewCount = newReviewCount;
    }

    public String getStatusLabel() { return statusLabel; }
    public BigDecimal getPrice() { return basePrice; }
    public long getSalesCount() { return soldCount; }
    public double getAverageRating() { return rating == null ? 0.0 : rating.doubleValue(); }
    public String getSku() { return null; }
    public boolean isOnDiscount() { return false; }
    public String getMainImageUrl() { return null; }

    public static Product create(String sku, String name, String description, BigDecimal basePrice,
            Long categoryId, Long brandId, long initialStock, String mainImageUrl) {
        return createDraft(name, description, basePrice, categoryId, brandId);
    }

    public static Product create(String sku, String name, String description, BigDecimal basePrice,
            Long categoryId, Long brandId, Long initialStock, String mainImageUrl) {
        return createDraft(name, description, basePrice, categoryId, brandId);
    }

    public void update(String name, String description, BigDecimal price, String mainImageUrl) {
        if (name != null && !name.isBlank()) updateBasicInfo(name, description);
        if (price != null) updatePrice(price);
    }

    public void replaceTags(java.util.Collection<String> newTags) {
        this.tags.clear();
        if (newTags != null) {
            newTags.forEach(t -> { if (t != null) { try { addTag(t); } catch (Exception ignored) {} } });
        }
    }

    public void softDelete(String requester) {
        discontinue("삭제: " + (requester != null ? requester : "unknown"));
    }

    public void markInStock() {
        if ("OUT_OF_STOCK".equals(statusLabel)) publish();
    }

    public void changePrice(BigDecimal newPrice, String reason) {
        updatePrice(newPrice);
    }

    public void applyDiscount(BigDecimal discountedPrice, java.time.LocalDateTime untilWhen) {}
    public void removeDiscount() {}

    public void syncStock(long quantity) {
        updateStock((int) Math.min(quantity, Integer.MAX_VALUE));
    }

    public void changeDisplayOrder(int order) {}
    public void markAsNew(boolean isNew) {}
    public void markAsRecommended(boolean recommended) {}

    private void applyStatus(String label, String reason) {
        this.statusLabel = label;
        this.statusReason = reason;
        this.statusChangedAt = LocalDateTime.now();
    }
}
