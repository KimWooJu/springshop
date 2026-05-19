package com.springshop.common.util;

import java.util.regex.Pattern;

/**
 * 전화번호 정규화/포맷팅/검증 유틸리티.
 */
public final class PhoneUtils {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");
    private static final Pattern KOREAN_MOBILE = Pattern.compile("^01[016789]\\d{7,8}$");
    private static final Pattern KOREAN_LANDLINE = Pattern.compile("^0(?:2|[3-9]\\d)\\d{7,8}$");
    private static final Pattern KOREAN_TOLL_FREE = Pattern.compile("^(080|1[5-9]\\d{2})\\d{4,7}$");

    public static final String DEFAULT_COUNTRY_CODE = "82";

    private PhoneUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 입력에서 숫자만 추출하여 정규화한다.
     */
    public static String normalize(String phone) {
        if (phone == null) return null;
        return NON_DIGITS.matcher(phone).replaceAll("");
    }

    /**
     * 010-1234-5678 형식으로 변환한다.
     */
    public static String format(String phone) {
        String digits = normalize(phone);
        if (digits == null || digits.length() < 9) return phone;

        if (digits.startsWith("02")) {
            // 서울 지역
            if (digits.length() == 9) {
                return "02-%s-%s".formatted(digits.substring(2, 5), digits.substring(5));
            } else if (digits.length() == 10) {
                return "02-%s-%s".formatted(digits.substring(2, 6), digits.substring(6));
            }
        } else if (digits.length() == 10) {
            return "%s-%s-%s".formatted(digits.substring(0, 3), digits.substring(3, 6), digits.substring(6));
        } else if (digits.length() == 11) {
            return "%s-%s-%s".formatted(digits.substring(0, 3), digits.substring(3, 7), digits.substring(7));
        }
        return phone;
    }

    public static boolean isValid(String phone) {
        return isMobile(phone) || isLandline(phone) || isTollFree(phone);
    }

    public static boolean isMobile(String phone) {
        String digits = normalize(phone);
        if (digits == null) return false;
        return KOREAN_MOBILE.matcher(digits).matches();
    }

    public static boolean isLandline(String phone) {
        String digits = normalize(phone);
        if (digits == null) return false;
        return KOREAN_LANDLINE.matcher(digits).matches();
    }

    public static boolean isTollFree(String phone) {
        String digits = normalize(phone);
        if (digits == null) return false;
        return KOREAN_TOLL_FREE.matcher(digits).matches();
    }

    /**
     * E.164 국제 형식으로 변환. 예: +821012345678
     */
    public static String toE164(String phone) {
        return toE164(phone, DEFAULT_COUNTRY_CODE);
    }

    public static String toE164(String phone, String countryCode) {
        String digits = normalize(phone);
        if (digits == null || digits.isEmpty()) return null;
        // 한국 번호: 첫 0 제거
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return "+" + countryCode + digits;
    }

    /**
     * E.164 형식을 일반 형식으로 복원.
     */
    public static String fromE164(String e164) {
        if (e164 == null || !e164.startsWith("+")) return e164;
        String digits = normalize(e164);
        if (digits.startsWith(DEFAULT_COUNTRY_CODE)) {
            return "0" + digits.substring(DEFAULT_COUNTRY_CODE.length());
        }
        return digits;
    }

    /**
     * 전화번호를 마스킹한다.
     */
    public static String mask(String phone) {
        String digits = normalize(phone);
        if (digits == null || digits.length() < 10) return phone;
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-****-" + digits.substring(6);
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(7);
    }

    public static String getCarrier(String phone) {
        String digits = normalize(phone);
        if (digits == null || digits.length() < 3) return "UNKNOWN";
        if (!digits.startsWith("01")) return "LANDLINE";
        return switch (digits.substring(2, 3)) {
            case "0" -> "SKT";
            case "1" -> "KT";
            case "6", "7" -> "LGU+";
            case "9" -> "MVNO";
            default -> "UNKNOWN";
        };
    }

    public static boolean equalsNormalized(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na == null || nb == null) return na == nb;
        return na.equals(nb);
    }
}
