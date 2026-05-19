package com.springshop.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 이메일 주소 값 객체.
 *
 * <p>RFC 5322 의 단순화된 정규식으로 형식을 검증한다. 모든 인스턴스는 소문자로
 * 정규화되어 저장된다(예: "USER@example.com" → "user@example.com").</p>
 *
 * <p>마스킹(masking) 메서드를 제공하여 로그 및 응답 직렬화 시 개인정보 보호를
 * 보장한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Embeddable
public final class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public static final int MAX_LENGTH = 254;

    @Column(name = "email_value", length = 254, nullable = false)
    private String value;

    protected Email() {
        // JPA 기본 생성자
    }

    public Email(String value) {
        Objects.requireNonNull(value, "이메일은 null일 수 없습니다");
        String normalized = value.trim().toLowerCase();
        validate(normalized);
        this.value = normalized;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    public static boolean isValid(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty() || normalized.length() > MAX_LENGTH) return false;
        return EMAIL_PATTERN.matcher(normalized).matches();
    }

    private void validate(String normalized) {
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("이메일은 비어 있을 수 없습니다");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("이메일 길이 초과: " + normalized.length());
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식: " + normalized);
        }
    }

    public String getValue() {
        return value;
    }

    /**
     * 이메일 로컬 파트("@" 앞쪽)를 반환한다.
     */
    public String localPart() {
        int at = value.indexOf('@');
        return at >= 0 ? value.substring(0, at) : value;
    }

    /**
     * 이메일 도메인 파트("@" 뒤쪽)를 반환한다.
     */
    public String domain() {
        int at = value.indexOf('@');
        return at >= 0 ? value.substring(at + 1) : "";
    }

    /**
     * 이메일을 마스킹한다. (예: "alice@example.com" → "a***@example.com")
     */
    public String masked() {
        String local = localPart();
        String dom = domain();
        if (local.length() <= 1) {
            return "*@" + dom;
        }
        return local.charAt(0) + "***@" + dom;
    }

    /**
     * 회사 도메인 여부를 검사한다(예: 사내 직원 식별).
     */
    public boolean isCorporateDomain(String corporateDomain) {
        Objects.requireNonNull(corporateDomain, "회사 도메인이 null입니다");
        return domain().equalsIgnoreCase(corporateDomain);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return Objects.equals(value, email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
