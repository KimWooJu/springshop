package com.springshop.domain.review;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * 리뷰 첨부 이미지.
 *
 * <p>한 리뷰에 최대 10장까지 등록 가능하며, displayOrder 로 노출 순서가 결정된다.
 * 원본 이미지 URL 과 썸네일 URL 을 모두 저장한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "review_images",
        indexes = {
                @Index(name = "idx_review_image_review", columnList = "review_id"),
                @Index(name = "idx_review_image_order", columnList = "display_order")
        }
)
public class ReviewImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "review_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_image_review"))
    private Review review;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    protected ReviewImage() {
        super();
    }

    private ReviewImage(Review review, String imageUrl, String thumbnailUrl, int displayOrder) {
        super();
        this.review = Objects.requireNonNull(review, "리뷰는 null일 수 없습니다");
        this.imageUrl = Objects.requireNonNull(imageUrl, "이미지 URL은 null일 수 없습니다");
        this.thumbnailUrl = thumbnailUrl;
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder는 0 이상이어야 합니다: " + displayOrder);
        }
        this.displayOrder = displayOrder;
    }

    /**
     * 정적 팩토리.
     */
    public static ReviewImage of(Review review, String imageUrl, int displayOrder) {
        return new ReviewImage(review, imageUrl, null, displayOrder);
    }

    public static ReviewImage of(Review review, String imageUrl, String thumbnailUrl, int displayOrder) {
        return new ReviewImage(review, imageUrl, thumbnailUrl, displayOrder);
    }

    public Review getReview() {
        return review;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public void updateMetadata(long fileSize, String mimeType, int width, int height) {
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    public void updateOrder(int newOrder) {
        if (newOrder < 0) {
            throw new IllegalArgumentException("displayOrder는 0 이상이어야 합니다: " + newOrder);
        }
        this.displayOrder = newOrder;
    }

    public void updateThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    @Override
    public String toString() {
        return "ReviewImage[id=%s, reviewId=%s, order=%d]"
                .formatted(getId(), review == null ? null : review.getId(), displayOrder);
    }
}
