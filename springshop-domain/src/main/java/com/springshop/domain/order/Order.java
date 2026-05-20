package com.springshop.domain.order;

import com.springshop.domain.base.BaseAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 주문 애그리거트 루트 엔티티.
 *
 * <p>주문 항목(OrderItem)과 배송 정보(OrderShipping)를 자식으로 가진다.
 * 주문 상태는 {@link OrderStatus} sealed interface로 관리되며, 각 상태 전이마다
 * 도메인 이벤트를 발행한다.</p>
 *
 * <p>총액 계산은 항목 합계 + 배송비 - 할인 + 쿠폰을 모두 반영한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_number", columnNames = "order_number"),
        indexes = {
                @Index(name = "idx_order_user", columnList = "user_id"),
                @Index(name = "idx_order_status", columnList = "status_label"),
                @Index(name = "idx_order_created", columnList = "created_at")
        }
)
public class Order extends BaseAuditEntity {

    @Column(name = "order_number", length = 30, nullable = false)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status_label", length = 30, nullable = false)
    private String statusLabel = "PENDING";

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt = LocalDateTime.now();

    @Column(name = "status_meta", length = 500)
    private String statusMeta;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OrderShipping shipping;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", precision = 19, scale = 2, nullable = false)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "memo", length = 500)
    private String memo;

    protected Order() {
        super();
    }

    private Order(Long userId) {
        super();
        this.userId = Objects.requireNonNull(userId, "사용자 ID 필수");
        this.orderNumber = generateOrderNumber();
        this.statusLabel = "PENDING";
        this.statusChangedAt = LocalDateTime.now();
    }

    public static Order create(Long userId) {
        return new Order(userId);
    }

    private static String generateOrderNumber() {
        // 형식: YYYYMMDD-XXXXXXXX
        String date = java.time.LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return date + "-" + suffix;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        LocalDateTime ts = statusChangedAt;
        return switch (statusLabel) {
            case "PENDING" -> new OrderStatus.Pending(ts);
            case "PAYMENT_PENDING" -> new OrderStatus.PaymentPending(ts);
            case "CONFIRMED" -> new OrderStatus.Confirmed(ts, statusMeta == null ? "SYSTEM" : statusMeta);
            case "PROCESSING" -> new OrderStatus.Processing(ts);
            case "SHIPPED" -> new OrderStatus.Shipped(statusMeta == null ? "UNKNOWN" : statusMeta, ts);
            case "DELIVERED" -> new OrderStatus.Delivered(ts);
            case "CANCELLED" -> new OrderStatus.Cancelled(statusMeta, ts);
            case "RETURN_REQUESTED" -> new OrderStatus.ReturnRequested(statusMeta, ts);
            case "RETURNED" -> new OrderStatus.Returned(ts);
            default -> throw new IllegalStateException("알 수 없는 주문 상태: " + statusLabel);
        };
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public OrderShipping getShipping() {
        return shipping;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getMemo() {
        return memo;
    }

    /**
     * 주문 항목 추가. 합계는 자동 재계산.
     */
    public void addItem(OrderItem item) {
        Objects.requireNonNull(item, "주문 항목 필수");
        if (!"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("대기 상태에서만 항목 추가 가능");
        }
        this.items.add(item);
        recalculateTotals();
    }

    public void removeItem(OrderItem item) {
        if (!"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("대기 상태에서만 항목 제거 가능");
        }
        this.items.remove(item);
        recalculateTotals();
    }

    public void setShipping(OrderShipping shipping) {
        this.shipping = Objects.requireNonNull(shipping, "배송 정보 필수");
        this.shippingFee = BigDecimal.valueOf(shipping.getShippingFee());
        recalculateTotals();
    }

    public void applyCoupon(String couponCode, BigDecimal discount) {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("할인 금액은 0 이상");
        }
        this.couponCode = couponCode;
        this.discountAmount = discount;
        recalculateTotals();
    }

    public void requestPayment() {
        if (!"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("대기 상태에서만 결제 요청 가능");
        }
        applyStatus("PAYMENT_PENDING", null);
        registerEvent(OrderEvents.OrderPlacedEvent.of(getId(), userId, finalAmount));
    }

    public void confirm(String confirmedBy) {
        if (!"PAYMENT_PENDING".equals(statusLabel) && !"PENDING".equals(statusLabel)) {
            throw new IllegalStateException("결제 대기/대기 상태에서만 확인 가능");
        }
        applyStatus("CONFIRMED", confirmedBy);
        registerEvent(OrderEvents.OrderConfirmedEvent.of(getId(), confirmedBy));
    }

    public void process() {
        if (!"CONFIRMED".equals(statusLabel)) {
            throw new IllegalStateException("확인된 주문만 처리 가능");
        }
        applyStatus("PROCESSING", null);
    }

    public void ship(String trackingNumber, String carrier) {
        if (!"PROCESSING".equals(statusLabel) && !"CONFIRMED".equals(statusLabel)) {
            throw new IllegalStateException("처리/확인 상태에서만 발송 가능");
        }
        if (shipping != null) {
            shipping.ship(carrier, trackingNumber);
        }
        applyStatus("SHIPPED", trackingNumber);
        registerEvent(OrderEvents.OrderShippedEvent.of(getId(), trackingNumber));
    }

    public void deliver() {
        if (!"SHIPPED".equals(statusLabel)) {
            throw new IllegalStateException("발송된 주문만 배송 완료 처리 가능");
        }
        if (shipping != null) {
            shipping.markDelivered();
        }
        applyStatus("DELIVERED", null);
        registerEvent(OrderEvents.OrderDeliveredEvent.of(getId()));
    }

    public void cancel(String reason) {
        OrderStatus current = getStatus();
        if (!current.isCancellable()) {
            throw new IllegalStateException("취소 불가 상태: " + current.label());
        }
        applyStatus("CANCELLED", reason);
        registerEvent(OrderEvents.OrderCancelledEvent.of(getId(), reason));
    }

    public void requestReturn(String reason) {
        if (!"DELIVERED".equals(statusLabel)) {
            throw new IllegalStateException("배송 완료된 주문만 반품 신청 가능");
        }
        applyStatus("RETURN_REQUESTED", reason);
    }

    public void completeReturn() {
        if (!"RETURN_REQUESTED".equals(statusLabel)) {
            throw new IllegalStateException("반품 요청 상태에서만 반품 완료 가능");
        }
        applyStatus("RETURNED", null);
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public BigDecimal calculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem item : items) {
            sum = sum.add(item.calculateTotal());
        }
        return sum;
    }

    public int itemCount() {
        return items.size();
    }

    public int totalQuantity() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    private void recalculateTotals() {
        this.totalAmount = calculateTotal();
        BigDecimal subtotalAfterDiscount = totalAmount.subtract(discountAmount);
        if (subtotalAfterDiscount.compareTo(BigDecimal.ZERO) < 0) {
            subtotalAfterDiscount = BigDecimal.ZERO;
        }
        this.finalAmount = subtotalAfterDiscount.add(shippingFee);
    }

    private void applyStatus(String label, String meta) {
        this.statusLabel = label;
        this.statusMeta = meta;
        this.statusChangedAt = LocalDateTime.now();
    }
}
