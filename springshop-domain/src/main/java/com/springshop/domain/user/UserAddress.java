package com.springshop.domain.user;

import com.springshop.domain.base.BaseEntity;
import com.springshop.domain.vo.Address;
import com.springshop.domain.vo.PhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * 사용자가 등록한 배송지(주소) 엔티티.
 *
 * <p>사용자별로 여러 배송지를 가질 수 있으며, 그 중 하나만 기본 배송지로 지정된다.
 * 별칭(addressName)으로 "집", "회사" 등 사용자가 인지하기 쉬운 이름을 부여한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(name = "user_addresses")
public class UserAddress extends BaseEntity {

    public static final int MAX_ADDRESSES_PER_USER = 10;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "address_name", length = 50, nullable = false)
    private String addressName;

    @Embedded
    private Address address;

    @Column(name = "recipient_name", length = 50, nullable = false)
    private String recipientName;

    @Embedded
    private PhoneNumber recipientPhone;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "delivery_memo", length = 200)
    private String deliveryMemo;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    protected UserAddress() {
        super();
    }

    public UserAddress(User user, String addressName, Address address,
                       String recipientName, PhoneNumber recipientPhone) {
        super();
        this.user = Objects.requireNonNull(user, "사용자 필수");
        this.addressName = validateAddressName(addressName);
        this.address = Objects.requireNonNull(address, "주소 필수");
        this.recipientName = Objects.requireNonNull(recipientName, "수령인 필수");
        this.recipientPhone = recipientPhone;
    }

    public static UserAddress create(User user, String name, Address address, String recipient, PhoneNumber phone) {
        return new UserAddress(user, name, address, recipient, phone);
    }

    private static String validateAddressName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("배송지 별칭은 필수입니다");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("배송지 별칭은 50자 이하여야 합니다");
        }
        return name.trim();
    }

    public User getUser() {
        return user;
    }

    public String getAddressName() {
        return addressName;
    }

    public Address getAddress() {
        return address;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public PhoneNumber getRecipientPhone() {
        return recipientPhone;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getDeliveryMemo() {
        return deliveryMemo;
    }

    public boolean isActive() {
        return isActive;
    }

    public void markAsDefault() {
        this.isDefault = true;
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
    }

    public void updateAddress(Address newAddress) {
        this.address = Objects.requireNonNull(newAddress, "주소가 null입니다");
    }

    public void updateRecipient(String name, PhoneNumber phone) {
        this.recipientName = Objects.requireNonNull(name, "수령인 이름은 필수입니다");
        this.recipientPhone = phone;
    }

    public void updateMemo(String memo) {
        if (memo != null && memo.length() > 200) {
            throw new IllegalArgumentException("배송 메모는 200자 이하여야 합니다");
        }
        this.deliveryMemo = memo;
    }

    public void rename(String newName) {
        this.addressName = validateAddressName(newName);
    }

    public void deactivate() {
        if (isDefault) {
            throw new IllegalStateException("기본 배송지는 비활성화할 수 없습니다");
        }
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public Long getUserId() { return user.getId(); }
    public boolean isDefaultAddress() { return isDefault; }
    public void setAsDefault() { markAsDefault(); }
    public void unsetDefault() { unmarkAsDefault(); }

    // ---- Address VO delegate getters ----
    public String getState() { return address != null ? address.getProvince() : null; }
    public String getCity() { return address != null ? address.getCity() : null; }
    public String getAddressLine1() { return address != null ? address.getStreet() : null; }
    public String getPhoneNumber() { return recipientPhone != null ? recipientPhone.getE164() : null; }

    public void update(String recipientName, String phoneNumber, String postalCode,
                       String addressLine1, String addressLine2,
                       String city, String state, String country, String deliveryNote) {
        if (recipientName != null && !recipientName.isBlank()) this.recipientName = recipientName;
        if (deliveryNote != null) updateMemo(deliveryNote);
    }

    /**
     * 다른 배송지와 같은 주소인지 검사한다(중복 등록 방지).
     */
    public boolean hasSameAddress(UserAddress other) {
        if (other == null) return false;
        return Objects.equals(this.address, other.address);
    }

    public String summary() {
        return "%s | %s | %s".formatted(addressName, recipientName, address.fullAddress());
    }
}
