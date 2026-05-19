package com.springshop.common.constant;

/**
 * 정규식 상수.
 *
 * <p>입력값 검증({@code @Pattern}) 및 비즈니스 로직에서 사용하는 정규식 모음.</p>
 *
 * <p>한국 환경에 특화된 패턴(휴대전화, 사업자번호, 우편번호)을 포함한다.</p>
 */
public final class RegexConstants {

    private RegexConstants() {
        throw new AssertionError("RegexConstants는 인스턴스화할 수 없습니다");
    }

    // ===== 연락처 =====
    /** RFC 5322 이메일 패턴 (간소화). */
    public static final String EMAIL = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";
    /** 한국 휴대전화 (01x-xxxx-xxxx). */
    public static final String PHONE_KR = "^(01[016789])(\\d{3,4})(\\d{4})$";
    /** 한국 휴대전화 (하이픈 포함). */
    public static final String PHONE_KR_WITH_HYPHEN = "^(01[016789])-?(\\d{3,4})-?(\\d{4})$";
    /** 국제 전화 (E.164). */
    public static final String PHONE_INTERNATIONAL = "^\\+?[1-9]\\d{1,14}$";

    // ===== 인증/계정 =====
    /** 비밀번호: 대소문자, 숫자, 특수문자 포함 8-100자. */
    public static final String PASSWORD =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)"
        + "(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,100}$";
    /** 사용자명: 한글/영문/숫자/하이픈/언더스코어 2-20자. */
    public static final String USERNAME = "^[a-zA-Z0-9가-힣_\\-]{2,20}$";

    // ===== URL/네트워크 =====
    /** HTTP/FTP URL. */
    public static final String URL = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
    /** IPv4 주소. */
    public static final String IP_ADDRESS =
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    // ===== 결제 =====
    /** 신용카드 번호 (4-4-4-4). */
    public static final String CREDIT_CARD = "^\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}$";
    /** 양수 금액 (소수점 2자리). */
    public static final String AMOUNT_POSITIVE = "^\\d+(\\.\\d{1,2})?$";

    // ===== 쿠폰/상품 =====
    /** 쿠폰 코드: 영문 대문자 + 숫자 6-20자. */
    public static final String COUPON_CODE = "^[A-Z0-9]{6,20}$";
    /** 상품 코드: AA000000 형식. */
    public static final String PRODUCT_CODE = "^[A-Z]{2}\\d{6}$";
    /** SKU 코드. */
    public static final String SKU = "^[A-Z0-9\\-]{6,30}$";

    // ===== URL Slug =====
    /** URL Slug: 소문자/숫자/하이픈. */
    public static final String SLUG = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    // ===== 정제 =====
    /** HTML 태그. */
    public static final String HTML_TAG = "<[^>]*>";
    /** 다중 공백. */
    public static final String WHITESPACE_MULTIPLE = "\\s+";
    /** 한글만. */
    public static final String KOREAN_ONLY = "^[가-힣]+$";

    // ===== 한국 비즈니스 =====
    /** 사업자 등록 번호 (000-00-00000). */
    public static final String KOREAN_BUSINESS_NUMBER = "^\\d{3}-\\d{2}-\\d{5}$";
    /** 우편번호 (5자리). */
    public static final String ZIPCODE_KR = "^\\d{5}$";
    /** 주민등록번호 마스킹 (앞 6자리만). */
    public static final String SSN_KR_MASKED = "^\\d{6}-[1-4]\\*{6}$";

    // ===== 식별자 =====
    /** UUID v4. */
    public static final String UUID =
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    /** 운송장 번호. */
    public static final String TRACKING_NUMBER = "^[A-Z0-9]{10,30}$";
    /** 주문 번호 (YYYYMMDD + 8자리). */
    public static final String ORDER_NUMBER = "^\\d{8}[A-Z0-9]{8}$";

    // ===== 날짜 =====
    /** YYYY-MM-DD. */
    public static final String DATE_YYYYMMDD = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$";
    /** HH:MM:SS. */
    public static final String TIME_HHMMSS = "^([01]?\\d|2[0-3]):[0-5]\\d:[0-5]\\d$";

    // ===== 안전성 =====
    /** SQL Injection 의심 문자. */
    public static final String SQL_INJECTION_SUSPECT =
        "(?i).*(\\b(select|insert|update|delete|drop|union|exec|--|;)\\b).*";
    /** XSS 의심 패턴. */
    public static final String XSS_SUSPECT = "(?i).*<script.*?>.*?</script>.*";
}
