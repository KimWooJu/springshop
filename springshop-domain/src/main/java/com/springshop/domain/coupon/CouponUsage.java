package com.springshop.domain.coupon;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 쿠폰 사용 이력 엔티티.
 *
 * <p>한 사용자가 같은 주문에 같은 쿠폰을 중복 사용하지 못하도록 (couponId, userId, orderId)
 * 조합에 유니크 제약을 둔다. 이를 통해 결제 재시도 시 중복 차감을 방지한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "coupon_usages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coupon_usage",
                columnNames = {"coupon_id", "user_id", "order_id"}),
        indexes = {
                @Index(name = "idx_usage_coupon", columnList = "coupon_id"),
                @Index(name = "idx_usage_user", columnList = "user_id"),
                @Index(name = "idx_usage_order", columnList = "order_id")
        }
)
public class CouponUsage extends BaseEntity {

    @Column(name = "coupon_id", nullable = false, updatable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "discount_amount", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal discountAmount;

    @CreatedDate
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    @Column(name = "is_cancelled", nullable = false)
    private boolean isCancelled = false;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    protected CouponUsage() {
        super();
    }

    private CouponUsage(Long couponId, Long userId, Long orderId, BigDecimal discountAmount) {
        super();
        this.couponId = Objects.requireNonNull(couponId, "couponId 필수");
        this.userId = Objects.requireNonNull(userId, "userId 필수");
        this.orderId = Objects.requireNonNull(orderId, "orderId 필수");
        Objects.requireNonNull(discountAmount, "discountAmount 필수");
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("discountAmount는 0 이상이어야 합니다: " + discountAmount);
        }
        this.discountAmount = discountAmount;
        this.isCancelled = false;
    }

    /**
     * 정적 팩토리.
     */
    public static CouponUsage of(Long couponId, Long userId, Long orderId, BigDecimal discountAmount) {
        return new CouponUsage(couponId, userId, orderId, discountAmount);
    }

    /**
     * 사용 취소 처리. 주문 취소 시 호출.
     */
    public void cancel(String reason) {
        if (isCancelled) {
            throw new IllegalStateException("이미 취소된 쿠폰 사용 이력입니다");
        }
        this.isCancelled = true;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = Objects.requireNonNullElse(reason, "주문 취소");
    }

    public Long getCouponId() {
        return couponId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    @Override
    public String toString() {
        return "CouponUsage[id=%s, coupon=%s, user=%s, order=%s, amount=%s, cancelled=%s]"
                .formatted(getId(), couponId, userId, orderId, discountAmount, isCancelled);
    }
}
