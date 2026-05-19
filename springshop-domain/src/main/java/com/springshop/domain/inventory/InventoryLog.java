package com.springshop.domain.inventory;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 재고 변동 이력 엔티티.
 *
 * <p>모든 재고 변동(입고/출고/예약/해제/조정/반품)은 이 테이블에 추적용으로 기록된다.
 * 감사/회계/분석 목적으로 활용되며 한 번 생성된 로그는 변경되지 않는다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "inventory_logs",
        indexes = {
                @Index(name = "idx_inv_log_inventory", columnList = "inventory_id"),
                @Index(name = "idx_inv_log_type", columnList = "change_type"),
                @Index(name = "idx_inv_log_order", columnList = "order_id"),
                @Index(name = "idx_inv_log_created", columnList = "created_at")
        }
)
public class InventoryLog extends BaseEntity {

    @Column(name = "inventory_id", nullable = false, updatable = false)
    private Long inventoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 20, nullable = false, updatable = false)
    private ChangeType changeType;

    @Column(name = "before_quantity", nullable = false, updatable = false)
    private int beforeQuantity;

    @Column(name = "after_quantity", nullable = false, updatable = false)
    private int afterQuantity;

    @Column(name = "change_quantity", nullable = false, updatable = false)
    private int changeQuantity;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    /**
     * 재고 변동 유형.
     */
    public enum ChangeType {
        /** 입고. */
        RECEIVE,
        /** 출고 (확정 출고). */
        SHIP,
        /** 반품 입고. */
        RETURN,
        /** 수동 조정. */
        ADJUST,
        /** 예약. */
        RESERVE,
        /** 예약 해제. */
        RELEASE,
        /** 파손 처리. */
        DAMAGE,
        /** 유효기간 만료 폐기. */
        EXPIRED
    }

    protected InventoryLog() {
        super();
    }

    private InventoryLog(Long inventoryId,
                         ChangeType changeType,
                         int beforeQuantity,
                         int afterQuantity,
                         String reason,
                         Long orderId,
                         String referenceId,
                         String createdBy) {
        super();
        this.inventoryId = Objects.requireNonNull(inventoryId, "inventoryId 필수");
        this.changeType = Objects.requireNonNull(changeType, "changeType 필수");
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.changeQuantity = afterQuantity - beforeQuantity;
        this.reason = reason;
        this.orderId = orderId;
        this.referenceId = referenceId;
        this.createdBy = Objects.requireNonNullElse(createdBy, "SYSTEM");
    }

    public static InventoryLog ofReceive(Long inventoryId, int before, int after, String reason, String createdBy) {
        return new InventoryLog(inventoryId, ChangeType.RECEIVE, before, after, reason, null, null, createdBy);
    }

    public static InventoryLog ofShip(Long inventoryId, int before, int after, Long orderId, String createdBy) {
        return new InventoryLog(inventoryId, ChangeType.SHIP, before, after, "주문 출고", orderId, null, createdBy);
    }

    public static InventoryLog ofReturn(Long inventoryId, int before, int after, Long orderId, String reason, String createdBy) {
        return new InventoryLog(inventoryId, ChangeType.RETURN, before, after, reason, orderId, null, createdBy);
    }

    public static InventoryLog ofAdjust(Long inventoryId, int before, int after, String reason, String createdBy) {
        return new InventoryLog(inventoryId, ChangeType.ADJUST, before, after, reason, null, null, createdBy);
    }

    public static InventoryLog ofReserve(Long inventoryId, int before, int after, Long orderId, String createdBy) {
        return new InventoryLog(inventoryId, ChangeType.RESERVE, before, after, "주문 예약", orderId, null, createdBy);
    }

    public static InventoryLog ofRelease(Long inventoryId, int before, int after, Long orderId, String reason, String createdBy) {
        return new InventoryLog(inventoryId, ChangeType.RELEASE, before, after, reason, orderId, null, createdBy);
    }

    public static InventoryLog ofDamage(Long inventoryId, int before, int after, String reason, String createdBy) {
        return new InventoryLog(inventoryId, ChangeType.DAMAGE, before, after, reason, null, null, createdBy);
    }

    public static InventoryLog ofExpired(Long inventoryId, int before, int after, String reason, String createdBy) {
        return new InventoryLog(inventoryId, ChangeType.EXPIRED, before, after, reason, null, null, createdBy);
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public int getBeforeQuantity() {
        return beforeQuantity;
    }

    public int getAfterQuantity() {
        return afterQuantity;
    }

    public int getChangeQuantity() {
        return changeQuantity;
    }

    public String getReason() {
        return reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    /**
     * 증가 트랜잭션인지 여부.
     */
    public boolean isIncrease() {
        return changeQuantity > 0;
    }

    public boolean isDecrease() {
        return changeQuantity < 0;
    }

    @Override
    public String toString() {
        return "InventoryLog[id=%s, inv=%s, type=%s, delta=%+d, before=%d, after=%d]"
                .formatted(getId(), inventoryId, changeType, changeQuantity, beforeQuantity, afterQuantity);
    }
}
