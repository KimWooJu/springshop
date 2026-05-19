package com.springshop.common.util;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 문자열 처리 유틸리티.
 */
public final class StringUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String NUMERIC = "0123456789";
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    public static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    public static String defaultIfBlank(String s, String defaultValue) {
        return isNullOrBlank(s) ? defaultValue : s;
    }

    public static String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (!isNullOrEmpty(v)) return v;
        }
        return null;
    }

    /**
     * 이메일을 마스킹한다. 예: kim.dev@gmail.com -> ki****@g****.com
     */
    public static String maskEmail(String email) {
        if (isNullOrBlank(email) || !email.contains("@")) return email;
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        String maskedLocal = local.length() <= 2
                ? local.charAt(0) + "*"
                : local.substring(0, 2) + "*".repeat(Math.max(2, local.length() - 2));

        int dot = domain.lastIndexOf('.');
        String maskedDomain;
        if (dot <= 1) {
            maskedDomain = "*".repeat(domain.length());
        } else {
            String name = domain.substring(0, dot);
            String tld = domain.substring(dot);
            maskedDomain = name.charAt(0) + "*".repeat(Math.max(3, name.length() - 1)) + tld;
        }
        return maskedLocal + "@" + maskedDomain;
    }

    /**
     * 전화번호를 마스킹한다. 예: 010-1234-5678 -> 010-****-5678
     */
    public static String maskPhone(String phone) {
        if (isNullOrBlank(phone)) return phone;
        String digits = NON_DIGITS.matcher(phone).replaceAll("");
        if (digits.length() < 10) return phone;
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-****-" + digits.substring(6);
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(7);
    }

    /**
     * 카드번호를 마스킹한다. 예: 1234-5678-9012-3456 -> ****-****-****-3456
     */
    public static String maskCardNumber(String card) {
        if (isNullOrBlank(card)) return card;
        String digits = NON_DIGITS.matcher(card).replaceAll("");
        if (digits.length() < 4) return card;
        String last4 = digits.substring(digits.length() - 4);
        return "****-****-****-" + last4;
    }

    /**
     * 한글 포함 텍스트를 URL 슬러그로 변환한다.
     */
    public static String toSlug(String text) {
        if (isNullOrBlank(text)) return "";
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        normalized = WHITESPACE.matcher(normalized).replaceAll("-");
        // 한글, 영문, 숫자, 하이픈만 남김
        StringBuilder sb = new StringBuilder();
        for (char c : normalized.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || (c >= 0xAC00 && c <= 0xD7A3)) {
                sb.append(c);
            }
        }
        // 연속 하이픈 정리
        String result = sb.toString().replaceAll("-+", "-");
        return result.replaceAll("^-|-$", "");
    }

    public static String generateRandomCode(int length) {
        return generateRandomCode(length, ALPHA_NUMERIC);
    }

    public static String generateRandomCode(int length, String charset) {
        if (length <= 0 || charset == null || charset.isEmpty()) {
            throw new IllegalArgumentException("Invalid arguments");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(RANDOM.nextInt(charset.length())));
        }
        return sb.toString();
    }

    public static String generateNumericCode(int length) {
        return generateRandomCode(length, NUMERIC);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        if (maxLength <= 3) return text.substring(0, maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String camelToSnake(String camel) {
        if (isNullOrBlank(camel)) return camel;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    public static String snakeToCamel(String snake) {
        if (isNullOrBlank(snake)) return snake;
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static String repeat(String s, int times) {
        return s == null ? null : s.repeat(Math.max(0, times));
    }

    public static String padLeft(String s, int length, char padChar) {
        if (s == null) s = "";
        if (s.length() >= length) return s;
        return String.valueOf(padChar).repeat(length - s.length()) + s;
    }

    public static String padRight(String s, int length, char padChar) {
        if (s == null) s = "";
        if (s.length() >= length) return s;
        return s + String.valueOf(padChar).repeat(length - s.length());
    }

    public static String extractNumbers(String text) {
        if (text == null) return "";
        return NON_DIGITS.matcher(text).replaceAll("");
    }

    public static String sanitizeHtml(String html) {
        if (html == null) return null;
        return HTML_TAG.matcher(html).replaceAll("");
    }

    public static String normalizeWhitespace(String s) {
        if (s == null) return null;
        return WHITESPACE.matcher(s.trim()).replaceAll(" ");
    }

    public static boolean equalsIgnoreCaseAndSpace(String a, String b) {
        if (a == null || b == null) return a == b;
        return a.replace(" ", "").equalsIgnoreCase(b.replace(" ", ""));
    }

    public static String reverse(String s) {
        if (s == null) return null;
        return new StringBuilder(s).reverse().toString();
    }

    public static int countOccurrences(String text, String target) {
        if (isNullOrEmpty(text) || isNullOrEmpty(target)) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }
}
