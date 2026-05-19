package com.springshop.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 전화번호 값 객체.
 *
 * <p>국제 형식(E.164)을 지원한다. 입력 시 하이픈/공백을 제거하고, "+" 접두어가
 * 없으면 기본 국가 코드(대한민국 +82)를 적용한다.</p>
 *
 * <p>표시 형식, 마스킹, 국가 코드 추출 메서드를 제공한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Embeddable
public final class PhoneNumber {

    /** E.164 표준: '+' + 국가코드(1~3자리) + 가입자번호(최대 12자리) */
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+\\d{8,15}$");

    public static final String DEFAULT_COUNTRY_CODE = "+82";

    @Column(name = "phone_number", length = 16, nullable = false)
    private String e164;

    protected PhoneNumber() {
        // JPA
    }

    public PhoneNumber(String raw) {
        Objects.requireNonNull(raw, "전화번호는 null일 수 없습니다");
        String normalized = normalize(raw);
        validate(normalized);
        this.e164 = normalized;
    }

    public static PhoneNumber of(String raw) {
        return new PhoneNumber(raw);
    }

    public static boolean isValid(String raw) {
        if (raw == null) return false;
        try {
            String normalized = normalize(raw);
            return E164_PATTERN.matcher(normalized).matches();
        } catch (Exception e) {
            return false;
        }
    }

    private static String normalize(String raw) {
        String cleaned = raw.replaceAll("[\\s-()]", "");
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2);
        } else if (cleaned.startsWith("0")) {
            // 국내 형식 "010-1234-5678" → "+8210-1234-5678"
            cleaned = DEFAULT_COUNTRY_CODE + cleaned.substring(1);
        } else if (!cleaned.startsWith("+")) {
            cleaned = DEFAULT_COUNTRY_CODE + cleaned;
        }
        return cleaned;
    }

    private void validate(String normalized) {
        if (!E164_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("유효하지 않은 전화번호 형식(E.164): " + normalized);
        }
    }

    public String getE164() {
        return e164;
    }

    /**
     * 국가 코드("+82" 등)를 반환한다.
     */
    public String countryCode() {
        // 단순 휴리스틱: 첫 1~3자리 숫자
        if (e164.startsWith("+82") || e164.startsWith("+81")) return e164.substring(0, 3);
        if (e164.startsWith("+1")) return "+1";
        if (e164.startsWith("+86")) return "+86";
        return e164.substring(0, Math.min(3, e164.length()));
    }

    /**
     * 국가 코드를 제외한 가입자 번호를 반환한다.
     */
    public String nationalNumber() {
        return e164.substring(countryCode().length());
    }

    /**
     * 표시용 포맷(예: +82 10-1234-5678).
     */
    public String formatted() {
        String national = nationalNumber();
        if (national.length() >= 8) {
            int mid = national.length() - 4;
            return "%s %s-%s".formatted(countryCode(), national.substring(0, mid), national.substring(mid));
        }
        return "%s %s".formatted(countryCode(), national);
    }

    /**
     * 가입자 번호의 중간 부분을 마스킹한다.
     */
    public String masked() {
        String national = nationalNumber();
        if (national.length() < 6) {
            return countryCode() + "****";
        }
        int prefixLen = Math.min(3, national.length());
        int suffixLen = Math.min(4, national.length() - prefixLen);
        int maskLen = national.length() - prefixLen - suffixLen;
        return countryCode() + " " + national.substring(0, prefixLen) + "*".repeat(maskLen) +
                national.substring(national.length() - suffixLen);
    }

    public boolean isMobile() {
        // 한국 기준 010~019, 미국 기준은 영역 분리 어려워 일단 +82만 처리
        if (e164.startsWith("+82") && nationalNumber().startsWith("1")) return true;
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber that)) return false;
        return Objects.equals(e164, that.e164);
    }

    @Override
    public int hashCode() {
        return Objects.hash(e164);
    }

    @Override
    public String toString() {
        return e164;
    }
}
