package com.springshop.domain.notification;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 알림 엔티티.
 *
 * <p>사용자에게 전달되는 단일 알림 한 건을 표현한다. {@link NotificationType} sealed 타입의
 * 코드/페이로드를 저장하고 표시 메시지를 함께 보존한다. 읽음 상태와 우선순위, 추적용 링크를
 * 가진다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notif_user_read", columnList = "user_id, is_read"),
                @Index(name = "idx_notif_type", columnList = "type_code"),
                @Index(name = "idx_notif_created", columnList = "created_at")
        }
)
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type_code", length = 30, nullable = false)
    private String typeCode;

    @Column(name = "type_payload", columnDefinition = "TEXT")
    private String typePayload;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "content", length = 2000, nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "link", length = 500)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10, nullable = false)
    private Priority priority = Priority.NORMAL;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 알림 우선순위.
     */
    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    protected Notification() {
        super();
    }

    private Notification(Long userId, String typeCode, String typePayload, String title, String content, Priority priority) {
        super();
        this.userId = Objects.requireNonNull(userId, "userId 필수");
        this.typeCode = Objects.requireNonNull(typeCode, "typeCode 필수");
        this.typePayload = typePayload;
        this.title = Objects.requireNonNull(title, "title 필수");
        this.content = Objects.requireNonNull(content, "content 필수");
        this.priority = Objects.requireNonNullElse(priority, Priority.NORMAL);
        this.isRead = false;
    }

    /**
     * 주문 상태 변경 알림.
     */
    public static Notification forOrderStatus(Long userId, NotificationType.OrderStatusChanged event) {
        String title = "주문 상태가 변경되었습니다";
        String content = "주문 #%s 의 상태가 [%s] 로 변경되었습니다. %s"
                .formatted(event.orderId(), event.status(), event.message());
        Notification n = new Notification(
                userId,
                event.getCategoryCode(),
                "orderId=%s;status=%s".formatted(event.orderId(), event.status()),
                title,
                content,
                event.recommendedPriority()
        );
        n.link = "/orders/" + event.orderId();
        return n;
    }

    /**
     * 결제 완료 알림.
     */
    public static Notification forPayment(Long userId, NotificationType.PaymentCompleted event) {
        String title = "결제가 완료되었습니다";
        String content = "주문 #%s 의 결제 %,d원이 완료되었습니다"
                .formatted(event.orderId(), event.amount().longValue());
        Notification n = new Notification(
                userId,
                event.getCategoryCode(),
                "orderId=%s;amount=%s".formatted(event.orderId(), event.amount()),
                title,
                content,
                event.recommendedPriority()
        );
        n.link = "/orders/" + event.orderId();
        return n;
    }

    /**
     * 재고 알림.
     */
    public static Notification forStockAlert(Long userId, NotificationType.StockAlert event) {
        String title = "재고가 부족합니다";
        String content = "상품 [%s] 재고가 %d 개 남았습니다"
                .formatted(event.productName(), event.remainingQuantity());
        Notification n = new Notification(
                userId,
                event.getCategoryCode(),
                "productId=%s;remain=%d".formatted(event.productId(), event.remainingQuantity()),
                title,
                content,
                event.recommendedPriority()
        );
        n.link = "/products/" + event.productId();
        return n;
    }

    /**
     * 리뷰 승인 알림.
     */
    public static Notification forReview(Long userId, NotificationType.ReviewApproved event) {
        String title = "리뷰가 승인되었습니다";
        String content = "작성하신 리뷰가 노출됩니다. 감사합니다!";
        Notification n = new Notification(
                userId,
                event.getCategoryCode(),
                "reviewId=%s;productId=%s".formatted(event.reviewId(), event.productId()),
                title,
                content,
                event.recommendedPriority()
        );
        n.link = "/reviews/" + event.reviewId();
        return n;
    }

    /**
     * 시스템 공지 알림.
     */
    public static Notification forSystem(Long userId, NotificationType.SystemNotice event) {
        Notification n = new Notification(
                userId,
                event.getCategoryCode(),
                event.message(),
                "시스템 공지",
                event.message(),
                event.recommendedPriority()
        );
        n.link = event.actionUrl();
        return n;
    }

    /**
     * 가격 인하 알림.
     */
    public static Notification forPriceDrop(Long userId, NotificationType.PriceDropAlert event) {
        BigDecimal diff = event.getDiscountAmount();
        String title = "관심상품 가격 인하!";
        String content = "[%s] %,d원 인하 (현재가 %,d원)"
                .formatted(event.productName(), diff.longValue(), event.newPrice().longValue());
        Notification n = new Notification(
                userId,
                event.getCategoryCode(),
                "productId=%s;old=%s;new=%s".formatted(event.productId(), event.oldPrice(), event.newPrice()),
                title,
                content,
                event.recommendedPriority()
        );
        n.link = "/products/" + event.productId();
        return n;
    }

    /**
     * 쿠폰 발급 알림.
     */
    public static Notification forCoupon(Long userId, NotificationType.CouponIssued event) {
        String title = "쿠폰이 발급되었습니다";
        String content = "[%s] 쿠폰이 발급되었습니다. 만료: %s"
                .formatted(event.couponName(), event.expiryDate());
        Notification n = new Notification(
                userId,
                event.getCategoryCode(),
                "code=%s".formatted(event.couponCode()),
                title,
                content,
                event.recommendedPriority()
        );
        n.link = "/coupons/" + event.couponCode();
        n.expiresAt = event.expiryDate();
        return n;
    }

    /**
     * 배송 업데이트 알림.
     */
    public static Notification forDelivery(Long userId, NotificationType.DeliveryUpdate event) {
        String title = "배송 상태가 업데이트되었습니다";
        String content = "[%s] %s 운송장 %s - %s"
                .formatted(event.carrier(), event.status(), event.trackingNumber(), event.orderId());
        Notification n = new Notification(
                userId,
                event.getCategoryCode(),
                "orderId=%s;tracking=%s".formatted(event.orderId(), event.trackingNumber()),
                title,
                content,
                event.recommendedPriority()
        );
        n.link = "/orders/" + event.orderId() + "/tracking";
        return n;
    }

    public void markAsRead() {
        if (!isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * 최근(hours 시간 이내) 발생한 알림인지.
     */
    public boolean isRecent(int hours) {
        if (getCreatedAt() == null) return false;
        return Duration.between(getCreatedAt(), LocalDateTime.now()).toHours() < hours;
    }

    /**
     * 만료된 알림인지.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getTypePayload() {
        return typePayload;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public String getLink() {
        return link;
    }

    public Priority getPriority() {
        return priority;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
