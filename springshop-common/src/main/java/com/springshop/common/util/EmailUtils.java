package com.springshop.common.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 이메일 검증/마스킹/도메인 유틸리티.
 */
public final class EmailUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "10minutemail.com",
            "tempmail.com",
            "guerrillamail.com",
            "throwawaymail.com",
            "mailinator.com",
            "yopmail.com",
            "trashmail.com",
            "fakeinbox.com",
            "dispostable.com",
            "getnada.com",
            "maildrop.cc",
            "tempmailaddress.com",
            "mintemail.com",
            "anonymbox.com",
            "spamgourmet.com"
    );

    private static final Set<String> POPULAR_DOMAINS = Set.of(
            "gmail.com", "naver.com", "daum.net", "kakao.com",
            "hanmail.net", "outlook.com", "yahoo.com", "icloud.com"
    );

    private EmailUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) return false;
        if (email.length() > 254) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 정규화: 소문자 변환, 공백 제거.
     */
    public static String normalize(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 이메일 마스킹. kim.dev@gmail.com -> ki****@gmail.com
     */
    public static String mask(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        String maskedLocal;
        if (local.length() <= 2) {
            maskedLocal = local.charAt(0) + "*";
        } else {
            int visible = Math.min(2, local.length() / 2);
            maskedLocal = local.substring(0, visible) + "*".repeat(local.length() - visible);
        }
        return maskedLocal + "@" + domain;
    }

    public static String getDomain(String email) {
        if (email == null || !email.contains("@")) return null;
        return email.substring(email.indexOf('@') + 1);
    }

    public static String getLocalPart(String email) {
        if (email == null || !email.contains("@")) return email;
        return email.substring(0, email.indexOf('@'));
    }

    /**
     * 임시 메일 서비스 감지.
     */
    public static boolean isDisposable(String email) {
        String domain = getDomain(email);
        if (domain == null) return false;
        return DISPOSABLE_DOMAINS.contains(domain.toLowerCase(Locale.ROOT));
    }

    public static boolean isPopularDomain(String email) {
        String domain = getDomain(email);
        if (domain == null) return false;
        return POPULAR_DOMAINS.contains(domain.toLowerCase(Locale.ROOT));
    }

    public static boolean isCorporateEmail(String email) {
        if (!isValid(email)) return false;
        return !isPopularDomain(email) && !isDisposable(email);
    }

    /**
     * 동일 이메일 판단 (정규화 후 비교).
     */
    public static boolean isSameEmail(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na == null || nb == null) return na == nb;
        return na.equals(nb);
    }

    /**
     * Gmail의 dot-trick과 +alias를 제거한 canonical 형식 변환.
     */
    public static String canonicalize(String email) {
        if (email == null) return null;
        String normalized = normalize(email);
        String local = getLocalPart(normalized);
        String domain = getDomain(normalized);
        if (domain == null) return normalized;

        // Gmail 처리
        if ("gmail.com".equals(domain) || "googlemail.com".equals(domain)) {
            // + 이후 제거
            int plusIdx = local.indexOf('+');
            if (plusIdx > 0) local = local.substring(0, plusIdx);
            // . 제거
            local = local.replace(".", "");
            return local + "@gmail.com";
        }
        return normalized;
    }

    public static String suggestCorrection(String email) {
        if (email == null || !email.contains("@")) return null;
        String[] parts = email.split("@", 2);
        String domain = parts[1].toLowerCase(Locale.ROOT);
        return switch (domain) {
            case "gmial.com", "gmai.com", "gmial.co", "gmaill.com" -> parts[0] + "@gmail.com";
            case "navr.com", "navre.com" -> parts[0] + "@naver.com";
            case "kakaom.com" -> parts[0] + "@kakao.com";
            default -> null;
        };
    }
}
