package com.springshop.domain.inventory;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 상품별 재고 알림 임계치.
 *
 * <p>같은 상품의 모든 옵션/창고를 합산한 가용 수량을 기준으로 경고/임박 알림을 발송한다.
 * 알림 폭주를 막기 위해 마지막 발송 시간 이후 {@code alertIntervalHours} 시간이 경과해야
 * 다시 알림을 보낸다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "stock_alert_thresholds",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_alert_pv",
                columnNames = {"product_id", "variant_id"}),
        indexes = {
                @Index(name = "idx_alert_product", columnList = "product_id"),
                @Index(name = "idx_alert_active", columnList = "is_active")
        }
)
public class StockAlertThreshold extends BaseAuditEntity {

    /**
     * 기본 알림 발송 간격 (시간).
     */
    public static final int DEFAULT_ALERT_INTERVAL_HOURS = 24;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "warning_threshold", nullable = false)
    private int warningThreshold;

    @Column(name = "critical_threshold", nullable = false)
    private int criticalThreshold;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "last_alerted_at")
    private LocalDateTime lastAlertedAt;

    @Column(name = "alert_interval_hours", nullable = false)
    private int alertIntervalHours = DEFAULT_ALERT_INTERVAL_HOURS;

    @Column(name = "notify_emails", length = 1000)
    private String notifyEmails;

    protected StockAlertThreshold() {
        super();
    }

    private StockAlertThreshold(Long productId, Long variantId, int warning, int critical) {
        super();
        if (productId == null) throw new IllegalArgumentException("productId 필수");
        if (warning <= 0) throw new IllegalArgumentException("warningThreshold는 양수여야 합니다");
        if (critical <= 0) throw new IllegalArgumentException("criticalThreshold는 양수여야 합니다");
        if (critical > warning) throw new IllegalArgumentException("critical은 warning보다 작아야 합니다");
        this.productId = productId;
        this.variantId = variantId;
        this.warningThreshold = warning;
        this.criticalThreshold = critical;
        this.isActive = true;
        this.alertIntervalHours = DEFAULT_ALERT_INTERVAL_HOURS;
    }

    public static StockAlertThreshold of(Long productId, Long variantId, int warning, int critical) {
        return new StockAlertThreshold(productId, variantId, warning, critical);
    }

    /**
     * 임계치 수정.
     */
    public void updateThresholds(int warning, int critical) {
        if (warning <= 0 || critical <= 0) {
            throw new IllegalArgumentException("임계치는 양수여야 합니다");
        }
        if (critical > warning) {
            throw new IllegalArgumentException("critical은 warning보다 작아야 합니다");
        }
        this.warningThreshold = warning;
        this.criticalThreshold = critical;
    }

    public void updateAlertInterval(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("간격은 양수여야 합니다");
        }
        this.alertIntervalHours = hours;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateNotifyEmails(String notifyEmails) {
        this.notifyEmails = notifyEmails;
    }

    /**
     * 알림 발송 후 발송 시각 갱신.
     */
    public void markAlerted() {
        this.lastAlertedAt = LocalDateTime.now();
    }

    public Long getProductId() {
        return productId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public int getWarningThreshold() {
        return warningThreshold;
    }

    public int getCriticalThreshold() {
        return criticalThreshold;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getLastAlertedAt() {
        return lastAlertedAt;
    }

    public int getAlertIntervalHours() {
        return alertIntervalHours;
    }

    public String getNotifyEmails() {
        return notifyEmails;
    }

    /**
     * 현재 재고가 경고 수준인지.
     */
    public boolean isWarning(int currentQuantity) {
        return currentQuantity <= warningThreshold && currentQuantity > criticalThreshold;
    }

    /**
     * 현재 재고가 임박 수준인지.
     */
    public boolean isCritical(int currentQuantity) {
        return currentQuantity <= criticalThreshold;
    }

    /**
     * 알림 발송 가능 시점인지.
     */
    public boolean shouldAlert() {
        if (!isActive) return false;
        if (lastAlertedAt == null) return true;
        long elapsed = ChronoUnit.HOURS.between(lastAlertedAt, LocalDateTime.now());
        return elapsed >= alertIntervalHours;
    }

    @Override
    public String toString() {
        return "StockAlertThreshold[id=%s, product=%s, variant=%s, warn=%d, crit=%d, active=%s]"
                .formatted(getId(), productId, variantId, warningThreshold, criticalThreshold, isActive);
    }
}
