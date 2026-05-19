package com.springshop.domain.order;

import com.springshop.domain.base.BaseEntity;
import com.springshop.domain.vo.Address;
import com.springshop.domain.vo.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 주문 배송 정보 엔티티.
 *
 * <p>주문 1건당 배송 정보 1건이 매핑된다. 운송장 번호, 운송사, 발송/도착 시각,
 * 수령인 정보, 배송 메모 등을 보관한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "order_shipping",
        uniqueConstraints = @UniqueConstraint(name = "uk_shipping_order", columnNames = "order_id")
)
public class OrderShipping extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "recipient_name", length = 50, nullable = false)
    private String recipientName;

    @Embedded
    private PhoneNumber recipientPhone;

    @Embedded
    private Address address;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "carrier", length = 50)
    private String carrier;

    @Column(name = "shipping_fee", nullable = false)
    private long shippingFee = 0L;

    @Column(name = "delivery_memo", length = 200)
    private String deliveryMemo;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    protected OrderShipping() {
        super();
    }

    public OrderShipping(Order order, String recipientName, PhoneNumber phone, Address address, long shippingFee) {
        super();
        this.order = Objects.requireNonNull(order, "주문 필수");
        this.recipientName = Objects.requireNonNull(recipientName, "수령인 필수");
        this.recipientPhone = phone;
        this.address = Objects.requireNonNull(address, "주소 필수");
        if (shippingFee < 0) throw new IllegalArgumentException("배송비는 0 이상");
        this.shippingFee = shippingFee;
    }

    public Order getOrder() {
        return order;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public PhoneNumber getRecipientPhone() {
        return recipientPhone;
    }

    public Address getAddress() {
        return address;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public long getShippingFee() {
        return shippingFee;
    }

    public String getDeliveryMemo() {
        return deliveryMemo;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public LocalDateTime getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    /**
     * 발송 처리.
     */
    public void ship(String carrier, String trackingNumber) {
        if (this.shippedAt != null) {
            throw new IllegalStateException("이미 발송 처리되었습니다");
        }
        if (carrier == null || carrier.isBlank()) {
            throw new IllegalArgumentException("운송사 필수");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("운송장 번호 필수");
        }
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.shippedAt = LocalDateTime.now();
        this.expectedDeliveryDate = shippedAt.plusDays(3);
    }

    /**
     * 배송 완료 처리.
     */
    public void markDelivered() {
        if (this.shippedAt == null) {
            throw new IllegalStateException("아직 발송되지 않은 주문입니다");
        }
        if (this.deliveredAt != null) {
            throw new IllegalStateException("이미 배송 완료된 주문입니다");
        }
        this.deliveredAt = LocalDateTime.now();
    }

    public void updateMemo(String memo) {
        if (memo != null && memo.length() > 200) {
            throw new IllegalArgumentException("배송 메모는 200자 이하");
        }
        this.deliveryMemo = memo;
    }

    public void updateAddress(Address newAddress) {
        if (shippedAt != null) {
            throw new IllegalStateException("발송된 주문의 주소는 변경할 수 없습니다");
        }
        this.address = Objects.requireNonNull(newAddress);
    }

    /**
     * 배송 소요 일수 (도착 기준).
     */
    public long deliveryDays() {
        if (shippedAt == null || deliveredAt == null) return -1;
        return java.time.Duration.between(shippedAt, deliveredAt).toDays();
    }

    public boolean isShipped() {
        return shippedAt != null;
    }

    public boolean isDelivered() {
        return deliveredAt != null;
    }
}
