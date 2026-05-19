package com.springshop.common.util;

import com.springshop.common.constant.RegexConstants;

import java.util.regex.Pattern;

/**
 * 정규식 기반 검증 유틸리티.
 */
public final class ValidationUtils {

    private static final Pattern KOREAN_BUSINESS_NUMBER = Pattern.compile("^\\d{3}-?\\d{2}-?\\d{5}$");
    private static final Pattern KOREAN_ID_NUMBER = Pattern.compile("^\\d{6}-?[1-4]\\d{6}$");

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isValidEmail(String email) {
        return matches(email, RegexConstants.EMAIL_PATTERN);
    }

    public static boolean isValidPhone(String phone) {
        return matches(phone, RegexConstants.PHONE_PATTERN);
    }

    /**
     * 비밀번호 강도 검증.
     * 8자 이상, 영문/숫자/특수문자 포함.
     */
    public static boolean isValidPassword(String password) {
        return matches(password, RegexConstants.PASSWORD_PATTERN);
    }

    public static boolean isValidUrl(String url) {
        return matches(url, RegexConstants.URL_PATTERN);
    }

    public static boolean isValidCreditCard(String card) {
        if (card == null) return false;
        String digits = card.replaceAll("\\D", "");
        if (!matches(digits, "^\\d{13,19}$")) return false;
        return luhnCheck(digits);
    }

    /**
     * Luhn 알고리즘 (신용카드 체크섬).
     */
    private static boolean luhnCheck(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /**
     * 사업자등록번호 형식 검증 (체크섬 포함).
     */
    public static boolean isValidKoreanBusinessNumber(String number) {
        if (number == null) return false;
        String digits = number.replaceAll("\\D", "");
        if (digits.length() != 10) return false;

        int[] weights = {1, 3, 7, 1, 3, 7, 1, 3, 5};
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        sum += ((digits.charAt(8) - '0') * 5) / 10;
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (digits.charAt(9) - '0');
    }

    /**
     * 주민등록번호 형식만 검증 (실제 검증 제외, 개인정보 보호).
     */
    public static boolean isValidKoreanIdNumberFormat(String idNumber) {
        if (idNumber == null) return false;
        return KOREAN_ID_NUMBER.matcher(idNumber).matches();
    }

    public static boolean matches(String text, String pattern) {
        if (text == null || pattern == null) return false;
        return Pattern.matches(pattern, text);
    }

    public static boolean matches(String text, Pattern pattern) {
        if (text == null || pattern == null) return false;
        return pattern.matcher(text).matches();
    }

    public static boolean isLength(String text, int min, int max) {
        if (text == null) return false;
        int len = text.length();
        return len >= min && len <= max;
    }

    public static boolean isNumeric(String text) {
        if (text == null || text.isEmpty()) return false;
        for (char c : text.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    public static boolean isAlpha(String text) {
        if (text == null || text.isEmpty()) return false;
        for (char c : text.toCharArray()) {
            if (!Character.isLetter(c)) return false;
        }
        return true;
    }

    public static boolean isAlphaNumeric(String text) {
        if (text == null || text.isEmpty()) return false;
        for (char c : text.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) return false;
        }
        return true;
    }
}
