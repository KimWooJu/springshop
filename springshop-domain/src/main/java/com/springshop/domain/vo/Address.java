package com.springshop.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * 주소를 표현하는 @Embeddable 값 객체.
 *
 * <p>우편번호(zipCode), 도로명/번지(street), 시(city), 도/광역시(province), 국가(country)
 * 다섯 가지 필드를 가지며, 전체 주소 포맷팅, 우편번호 검증 등의 메서드를 제공한다.</p>
 *
 * <p>JPA @Embeddable로 엔티티 내부에 임베디드 컬럼들로 저장된다.</p>
 *
 * @author SpringShop Domain Team
 */
@Embeddable
public final class Address {

    public static final String DEFAULT_COUNTRY = "KR";

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "street", length = 200)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "country", length = 50)
    private String country;

    @Column(name = "address_detail", length = 200)
    private String detail;

    protected Address() {
        // JPA
    }

    public Address(String zipCode, String street, String city, String province, String country, String detail) {
        this.zipCode = validateZipCode(zipCode);
        this.street = requireNonEmpty(street, "도로명");
        this.city = requireNonEmpty(city, "시");
        this.province = requireNonEmpty(province, "도");
        this.country = (country == null || country.isBlank()) ? DEFAULT_COUNTRY : country;
        this.detail = detail == null ? "" : detail.trim();
    }

    public static Address of(String zipCode, String street, String city, String province) {
        return new Address(zipCode, street, city, province, DEFAULT_COUNTRY, "");
    }

    public static Address ofKorea(String zipCode, String province, String city, String street, String detail) {
        return new Address(zipCode, street, city, province, "KR", detail);
    }

    private static String requireNonEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다");
        }
        return value.trim();
    }

    private static String validateZipCode(String zip) {
        if (zip == null || zip.isBlank()) {
            throw new IllegalArgumentException("우편번호는 필수입니다");
        }
        String trimmed = zip.trim();
        if (!trimmed.matches("^[A-Za-z0-9\\-]{3,10}$")) {
            throw new IllegalArgumentException("유효하지 않은 우편번호: " + zip);
        }
        return trimmed;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public String getCountry() {
        return country;
    }

    public String getDetail() {
        return detail;
    }

    /**
     * 전체 주소 문자열을 반환한다.
     */
    public String fullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(zipCode).append("] ");
        sb.append(country).append(" ").append(province).append(" ").append(city).append(" ").append(street);
        if (detail != null && !detail.isBlank()) {
            sb.append(" ").append(detail);
        }
        return sb.toString();
    }

    /**
     * 국내 주소 형식(시/도 → 시/군/구 → 도로명 + 상세).
     */
    public String koreanFormat() {
        return "%s %s %s %s (%s)".formatted(province, city, street,
                detail == null ? "" : detail, zipCode);
    }

    /**
     * 같은 도시 안에 있는지 검사한다(주소 일부 비교).
     */
    public boolean isSameCity(Address other) {
        if (other == null) return false;
        return Objects.equals(this.city, other.city) && Objects.equals(this.province, other.province);
    }

    /**
     * 국내 주소인지 검사한다.
     */
    public boolean isDomestic() {
        return DEFAULT_COUNTRY.equalsIgnoreCase(this.country) ||
                "Korea".equalsIgnoreCase(this.country) ||
                "South Korea".equalsIgnoreCase(this.country);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address address)) return false;
        return Objects.equals(zipCode, address.zipCode) &&
                Objects.equals(street, address.street) &&
                Objects.equals(city, address.city) &&
                Objects.equals(province, address.province) &&
                Objects.equals(country, address.country) &&
                Objects.equals(detail, address.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipCode, street, city, province, country, detail);
    }

    @Override
    public String toString() {
        return fullAddress();
    }
}
