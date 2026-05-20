package com.springshop.domain.product;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * 상품 이미지 엔티티.
 *
 * <p>하나의 상품은 여러 이미지를 가질 수 있으며, 그 중 하나가 메인 이미지로
 * 지정된다. 정렬 순서(displayOrder)에 따라 갤러리에 표시된다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "product_images",
        indexes = @Index(name = "idx_image_product", columnList = "product_id")
)
public class ProductImage extends BaseEntity {

    public static final int MAX_IMAGES_PER_PRODUCT = 10;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    protected ProductImage() {
        super();
    }

    public ProductImage(Product product, String imageUrl, String thumbnailUrl, String altText, int displayOrder) {
        super();
        this.product = Objects.requireNonNull(product, "상품 필수");
        this.imageUrl = validateUrl(imageUrl);
        this.thumbnailUrl = thumbnailUrl;
        this.altText = altText;
        this.displayOrder = Math.max(0, displayOrder);
    }

    public static ProductImage mainImage(Product product, String url, String thumbUrl, String alt) {
        ProductImage img = new ProductImage(product, url, thumbUrl, alt, 0);
        img.isMain = true;
        return img;
    }

    private static String validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("이미지 URL은 필수입니다");
        }
        if (!url.matches("^https?://.+")) {
            throw new IllegalArgumentException("URL은 http:// 또는 https:// 로 시작해야 합니다: " + url);
        }
        return url;
    }

    public Product getProduct() {
        return product;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getAltText() {
        return altText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isMain() {
        return isMain;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public Integer getWidthPx() {
        return widthPx;
    }

    public Integer getHeightPx() {
        return heightPx;
    }

    public void markAsMain() {
        this.isMain = true;
    }

    public void unmarkAsMain() {
        this.isMain = false;
    }

    public void changeOrder(int newOrder) {
        if (newOrder < 0) throw new IllegalArgumentException("표시 순서는 0 이상");
        this.displayOrder = newOrder;
    }

    public void updateMetadata(Long sizeBytes, Integer widthPx, Integer heightPx) {
        this.fileSizeBytes = sizeBytes;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
    }

    public void updateAltText(String alt) {
        if (alt != null && alt.length() > 200) {
            throw new IllegalArgumentException("대체 텍스트는 200자 이하");
        }
        this.altText = alt;
    }

    public Long getProductId() {
        return product.getId();
    }

    public void updateUrl(String imageUrl, String altText) {
        this.imageUrl = validateUrl(imageUrl);
        this.altText = altText;
    }

    public void changeDisplayOrder(int order) {
        changeOrder(order);
    }

    /**
     * 종횡비를 반환한다(미설정 시 null).
     */
    public Double aspectRatio() {
        if (widthPx == null || heightPx == null || heightPx == 0) return null;
        return widthPx.doubleValue() / heightPx.doubleValue();
    }
}
