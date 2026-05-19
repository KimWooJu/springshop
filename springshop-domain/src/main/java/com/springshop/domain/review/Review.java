package com.springshop.domain.review;

import com.springshop.domain.base.BaseAuditEntity;
import com.springshop.domain.base.DomainEvent;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 리뷰 애그리거트 루트.
 *
 * <p>구매한 상품에 대한 사용자의 평가(별점 1~5)와 텍스트, 첨부 이미지를 관리한다.
 * 상태는 {@link ReviewStatus} sealed 타입으로 표현되며 승인된 리뷰만 일반 노출된다.
 * 신고 누적 5회 이상이면 자동으로 숨김 처리된다.</p>
 *
 * <p>리뷰는 등록 후 30일 이내에만 수정 가능하다. 도움됨/안됨, 신고 카운터는 별도로 관리된다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_review_product_status", columnList = "product_id, status_label"),
                @Index(name = "idx_review_user", columnList = "user_id"),
                @Index(name = "idx_review_order", columnList = "order_id"),
                @Index(name = "idx_review_rating", columnList = "rating")
        }
)
public class Review extends BaseAuditEntity {

    /**
     * 별점 최소값.
     */
    public static final int MIN_RATING = 1;

    /**
     * 별점 최대값.
     */
    public static final int MAX_RATING = 5;

    /**
     * 자동 숨김 처리 임계 신고 수.
     */
    public static final int AUTO_HIDE_REPORT_THRESHOLD = 5;

    /**
     * 수정 가능 기한(일).
     */
    public static final int EDITABLE_DAYS = 30;

    /**
     * 이미지 최대 개수.
     */
    public static final int MAX_IMAGES = 10;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "status_label", length = 20, nullable = false)
    private String statusLabel = "PENDING";

    @Column(name = "status_meta", length = 500)
    private String statusMeta;

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt = LocalDateTime.now();

    @Column(name = "status_actor", length = 100)
    private String statusActor;

    @Column(name = "verified_purchase", nullable = false)
    private boolean isVerifiedPurchase;

    @Column(name = "helpful_count", nullable = false)
    private int helpfulCount = 0;

    @Column(name = "not_helpful_count", nullable = false)
    private int notHelpfulCount = 0;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    @Column(name = "admin_comment", length = 1000)
    private String adminComment;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<ReviewImage> images = new ArrayList<>();

    protected Review() {
        super();
    }

    private Review(Long userId, Long productId, Long orderId, int rating, String title, String content, boolean verifiedPurchase) {
        super();
        this.userId = Objects.requireNonNull(userId, "userId 필수");
        this.productId = Objects.requireNonNull(productId, "productId 필수");
        this.orderId = Objects.requireNonNull(orderId, "orderId 필수");
        validateRating(rating);
        Objects.requireNonNull(content, "content 필수");
        if (content.isBlank()) {
            throw new IllegalArgumentException("리뷰 내용이 비어있습니다");
        }
        this.rating = rating;
        this.title = title;
        this.content = content;
        this.isVerifiedPurchase = verifiedPurchase;
        this.statusLabel = "PENDING";
        this.statusChangedAt = LocalDateTime.now();
    }

    /**
     * 새 리뷰 생성. 검증된 구매로 표시된 경우 신뢰도가 더 높게 노출된다.
     */
    public static Review write(Long userId, Long productId, Long orderId, int rating, String title, String content, boolean verifiedPurchase) {
        return new Review(userId, productId, orderId, rating, title, content, verifiedPurchase);
    }

    private static void validateRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException(
                    "별점은 %d~%d 사이여야 합니다: %d".formatted(MIN_RATING, MAX_RATING, rating));
        }
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public int getRating() {
        return rating;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isVerifiedPurchase() {
        return isVerifiedPurchase;
    }

    public int getHelpfulCount() {
        return helpfulCount;
    }

    public int getNotHelpfulCount() {
        return notHelpfulCount;
    }

    public int getReportCount() {
        return reportCount;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public List<ReviewImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public LocalDateTime getStatusChangedAt() {
        return statusChangedAt;
    }

    public String getStatusActor() {
        return statusActor;
    }

    /**
     * 현재 상태 sealed record 복원.
     */
    public ReviewStatus getStatus() {
        return switch (statusLabel) {
            case "PENDING" -> new ReviewStatus.Pending(statusChangedAt);
            case "APPROVED" -> new ReviewStatus.Approved(statusChangedAt, statusActor);
            case "REJECTED" -> new ReviewStatus.Rejected(
                    statusMeta == null ? "사유 미상" : statusMeta,
                    statusChangedAt,
                    statusActor);
            case "HIDDEN" -> new ReviewStatus.Hidden(
                    statusMeta == null ? "사유 미상" : statusMeta,
                    statusChangedAt);
            default -> throw new IllegalStateException("알 수 없는 리뷰 상태: " + statusLabel);
        };
    }

    /**
     * 리뷰 승인.
     */
    public void approve(String approvedBy) {
        if (!"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("PENDING 상태에서만 승인 가능: " + statusLabel);
        }
        applyStatus("APPROVED", null, Objects.requireNonNullElse(approvedBy, "SYSTEM"));
        registerEvent(DomainEvent.GenericEvent.of(
                getId(),
                "ReviewApproved: productId=%s, rating=%d, approvedBy=%s"
                        .formatted(productId, rating, this.statusActor)));
    }

    /**
     * 리뷰 거부.
     */
    public void reject(String reason, String rejectedBy) {
        if (!"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("PENDING 상태에서만 거부 가능: " + statusLabel);
        }
        Objects.requireNonNull(reason, "거부 사유 필수");
        applyStatus("REJECTED", reason, Objects.requireNonNullElse(rejectedBy, "SYSTEM"));
    }

    /**
     * 리뷰 숨김 처리.
     */
    public void hide(String reason) {
        if ("HIDDEN".equals(statusLabel)) {
            throw new IllegalStateException("이미 숨김 처리됨");
        }
        Objects.requireNonNull(reason, "숨김 사유 필수");
        applyStatus("HIDDEN", reason, statusActor);
    }

    /**
     * 도움됨 카운터 증가.
     */
    public void markHelpful() {
        this.helpfulCount++;
    }

    /**
     * 안됨 카운터 증가.
     */
    public void markNotHelpful() {
        this.notHelpfulCount++;
    }

    /**
     * 신고 카운터 증가. 임계치 도달 시 자동 숨김.
     */
    public void report() {
        this.reportCount++;
        if (this.reportCount >= AUTO_HIDE_REPORT_THRESHOLD && !"HIDDEN".equals(statusLabel)) {
            hide("신고 누적 %d회 자동 숨김".formatted(this.reportCount));
        }
    }

    /**
     * 리뷰 내용 수정. 수정 가능 기간이 지나면 거부된다.
     */
    public void update(int rating, String title, String content) {
        if (!isEditable()) {
            throw new IllegalStateException("수정 가능 기간(%d일)이 지났습니다".formatted(EDITABLE_DAYS));
        }
        if ("REJECTED".equals(statusLabel) || "HIDDEN".equals(statusLabel)) {
            throw new IllegalStateException("거부/숨김 처리된 리뷰는 수정할 수 없습니다");
        }
        validateRating(rating);
        Objects.requireNonNull(content, "content 필수");
        if (content.isBlank()) {
            throw new IllegalArgumentException("리뷰 내용이 비어있습니다");
        }
        this.rating = rating;
        this.title = title;
        this.content = content;
        // 재검토 필요 시 PENDING으로 되돌릴 수 있다
        if ("APPROVED".equals(statusLabel)) {
            applyStatus("PENDING", "수정 후 재검토", null);
        }
    }

    /**
     * 수정 가능 여부.
     */
    public boolean isEditable() {
        return getCreatedAt() != null
                && getCreatedAt().plusDays(EDITABLE_DAYS).isAfter(LocalDateTime.now());
    }

    /**
     * 이미지 추가.
     */
    public void addImage(String imageUrl, String thumbnailUrl) {
        if (images.size() >= MAX_IMAGES) {
            throw new IllegalStateException("이미지 최대 %d개 초과".formatted(MAX_IMAGES));
        }
        int order = images.size();
        ReviewImage image = ReviewImage.of(this, imageUrl, thumbnailUrl, order);
        this.images.add(image);
    }

    /**
     * 이미지 모두 제거.
     */
    public void clearImages() {
        this.images.clear();
    }

    public void updateAdminComment(String comment) {
        this.adminComment = comment;
    }

    private void applyStatus(String label, String meta, String actor) {
        this.statusLabel = label;
        this.statusMeta = meta;
        this.statusActor = actor;
        this.statusChangedAt = LocalDateTime.now();
    }

    /**
     * 도움됨 비율.
     */
    public double helpfulRatio() {
        int total = helpfulCount + notHelpfulCount;
        if (total == 0) return 0.0;
        return (double) helpfulCount / total;
    }
}
