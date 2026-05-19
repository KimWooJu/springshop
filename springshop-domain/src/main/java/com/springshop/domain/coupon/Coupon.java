package com.springshop.domain.coupon;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 쿠폰 애그리거트 루트.
 *
 * <p>발급된 쿠폰 정의를 표현한다. 코드(code)로 식별되며 활성/비활성, 사용 기간,
 * 발급량/사용량, 사용자 유형 제한, 사용자당 사용 한도를 관리한다.</p>
 *
 * <p>{@link DiscountType}을 칼럼에 저장하고 추가 필드(rate, maxDiscount, minOrder 등)와
 * 함께 {@link DiscountPolicy} sealed 타입을 합성한다.</p>
 *
 * <p>동시성 제어: 사용량 증가는 낙관적 락(BaseEntity#version)을 통해 보호되며,
 * 실패 시 재시도 정책은 호출자에서 처리한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "coupons",
        uniqueConstraints = @UniqueConstraint(name = "uk_coupon_code", columnNames = "code"),
        indexes = {
                @Index(name = "idx_coupon_active", columnList = "is_active"),
                @Index(name = "idx_coupon_period", columnList = "start_date, end_date")
        }
)
public class Coupon extends BaseAuditEntity {

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20, nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 19, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount_amount", precision = 19, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "used_quantity", nullable = false)
    private int usedQuantity = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_user_type", length = 20, nullable = false)
    private TargetUserType targetUserType = TargetUserType.ALL;

    @Column(name = "max_usage_per_user", nullable = false)
    private int maxUsagePerUser = 1;

    /**
     * 할인 타입.
     */
    public enum DiscountType {
        FIXED_AMOUNT,
        PERCENTAGE,
        FREE_SHIPPING,
        BUY_X_GET_Y
    }

    /**
     * 대상 사용자 유형.
     */
    public enum TargetUserType {
        ALL,
        NEW_USER,
        VIP
    }

    protected Coupon() {
        super();
    }

    private Coupon(String code, String name, DiscountType type, BigDecimal discountValue,
                   LocalDateTime startDate, LocalDateTime endDate) {
        super();
        this.code = Objects.requireNonNull(code, "code 필수");
        this.name = Objects.requireNonNull(name, "name 필수");
        this.discountType = Objects.requireNonNull(type, "discountType 필수");
        Objects.requireNonNull(discountValue, "discountValue 필수");
        if (discountValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("discountValue는 0 이상이어야 합니다: " + discountValue);
        }
        this.discountValue = discountValue;
        this.startDate = Objects.requireNonNull(startDate, "startDate 필수");
        this.endDate = Objects.requireNonNull(endDate, "endDate 필수");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate는 startDate 이후여야 합니다");
        }
        this.isActive = true;
        this.targetUserType = TargetUserType.ALL;
        this.maxUsagePerUser = 1;
    }

    public static Coupon create(String code, String name, DiscountType type, BigDecimal discountValue,
                                LocalDateTime startDate, LocalDateTime endDate) {
        return new Coupon(code, name, type, discountValue, startDate, endDate);
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateMinOrderAmount(BigDecimal min) {
        if (min == null) {
            this.minOrderAmount = BigDecimal.ZERO;
        } else if (min.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minOrderAmount는 0 이상이어야 합니다");
        } else {
            this.minOrderAmount = min;
        }
    }

    public void updateMaxDiscountAmount(BigDecimal max) {
        this.maxDiscountAmount = max;
    }

    public void updateTotalQuantity(Integer total) {
        if (total != null && total < usedQuantity) {
            throw new IllegalArgumentException("totalQuantity는 usedQuantity 이상이어야 합니다");
        }
        this.totalQuantity = total;
    }

    public void updateTargetUserType(TargetUserType type) {
        this.targetUserType = Objects.requireNonNullElse(type, TargetUserType.ALL);
    }

    public void updateMaxUsagePerUser(int max) {
        if (max <= 0) {
            throw new IllegalArgumentException("maxUsagePerUser는 양수여야 합니다");
        }
        this.maxUsagePerUser = max;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public TargetUserType getTargetUserType() {
        return targetUserType;
    }

    public int getMaxUsagePerUser() {
        return maxUsagePerUser;
    }

    /**
     * 현재 사용 가능한 상태인지.
     */
    public boolean isValid() {
        if (!isActive) return false;
        if (isExpired()) return false;
        if (isExhausted()) return false;
        return true;
    }

    /**
     * 만료 여부.
     */
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(startDate) || now.isAfter(endDate);
    }

    /**
     * 발급 한도 소진 여부.
     */
    public boolean isExhausted() {
        return totalQuantity != null && usedQuantity >= totalQuantity;
    }

    /**
     * 특정 사용자가 사용 가능한지.
     *
     * @param userId 사용자 ID
     * @param userUsageCount 사용자의 누적 사용 횟수
     */
    public boolean canUserUse(Long userId, int userUsageCount) {
        Objects.requireNonNull(userId, "userId 필수");
        if (!isValid()) return false;
        if (userUsageCount >= maxUsagePerUser) return false;
        return true;
    }

    /**
     * 쿠폰 사용 처리(카운터 증가). 동시성 보호는 호출자가 책임진다.
     */
    public synchronized void use() {
        if (isExhausted()) {
            throw new IllegalStateException("쿠폰이 모두 소진되었습니다");
        }
        if (!isActive) {
            throw new IllegalStateException("비활성 쿠폰입니다");
        }
        if (isExpired()) {
            throw new IllegalStateException("만료된 쿠폰입니다");
        }
        this.usedQuantity++;
    }

    /**
     * 비활성화.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 활성화.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 주문 금액에 대해 적용될 할인액 계산.
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        Objects.requireNonNull(orderAmount, "orderAmount 필수");
        if (orderAmount.compareTo(minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        return switch (discountType) {
            case FIXED_AMOUNT -> discountValue.min(orderAmount);
            case PERCENTAGE -> {
                BigDecimal discount = orderAmount.multiply(discountValue)
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                yield maxDiscountAmount != null ? discount.min(maxDiscountAmount) : discount;
            }
            case FREE_SHIPPING -> BigDecimal.ZERO;
            case BUY_X_GET_Y -> BigDecimal.ZERO;
        };
    }

    /**
     * 잔여 발급 가능 수량. null 이면 무제한.
     */
    public Integer getRemainingQuantity() {
        if (totalQuantity == null) return null;
        return totalQuantity - usedQuantity;
    }

    /**
     * 도메인 DiscountPolicy로 변환.
     */
    public DiscountPolicy toPolicy() {
        return switch (discountType) {
            case FIXED_AMOUNT -> new DiscountPolicy.FixedAmount(discountValue, java.util.Currency.getInstance("KRW"));
            case PERCENTAGE -> new DiscountPolicy.Percentage(discountValue.intValue(), maxDiscountAmount);
            case FREE_SHIPPING -> new DiscountPolicy.FreeShipping(minOrderAmount);
            case BUY_X_GET_Y -> new DiscountPolicy.BuyXGetY(
                    discountValue.intValue(),
                    1,
                    maxDiscountAmount == null ? BigDecimal.ZERO : maxDiscountAmount);
        };
    }

    @Override
    public String toString() {
        return "Coupon[id=%s, code=%s, type=%s, value=%s, valid=%s]"
                .formatted(getId(), code, discountType, discountValue, isValid());
    }
}
