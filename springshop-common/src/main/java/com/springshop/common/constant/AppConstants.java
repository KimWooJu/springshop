package com.springshop.common.constant;

/**
 * 애플리케이션 전역 상수.
 *
 * <p>비즈니스 규칙, 페이지네이션, 파일 업로드, 캐시 TTL 등 도메인 전반에 걸쳐
 * 사용되는 매직 넘버를 제거하기 위한 상수 모음.</p>
 *
 * <p>모든 상수는 {@code public static final}로 선언되며, 인스턴스화를 막기 위해
 * private 생성자를 가진다.</p>
 */
public final class AppConstants {

    private AppConstants() {
        throw new AssertionError("AppConstants는 인스턴스화할 수 없습니다");
    }

    // ===== 페이지네이션 =====
    /** 기본 페이지 크기. */
    public static final int DEFAULT_PAGE_SIZE = 20;
    /** 최대 페이지 크기 (DoS 방지). */
    public static final int MAX_PAGE_SIZE = 100;
    /** 기본 페이지 번호 (0-based). */
    public static final int DEFAULT_PAGE = 0;
    /** 최소 페이지 크기. */
    public static final int MIN_PAGE_SIZE = 1;

    // ===== 파일 업로드 =====
    /** 일반 파일 최대 크기 (10MB). */
    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    /** 이미지 파일 최대 크기 (5MB). */
    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    /** 문서 파일 최대 크기 (10MB). */
    public static final long MAX_DOCUMENT_SIZE_BYTES = 10L * 1024L * 1024L;
    /** 허용 이미지 확장자. */
    public static final String[] ALLOWED_IMAGE_TYPES = {"jpg", "jpeg", "png", "gif", "webp"};
    /** 허용 문서 확장자. */
    public static final String[] ALLOWED_DOCUMENT_TYPES = {"pdf", "doc", "docx", "xls", "xlsx"};
    /** 상품 당 최대 이미지 수. */
    public static final int MAX_IMAGES_PER_PRODUCT = 10;
    /** 리뷰 당 최대 이미지 수. */
    public static final int MAX_IMAGES_PER_REVIEW = 5;
    /** 사용자 프로필 이미지 최대 크기 (2MB). */
    public static final long MAX_PROFILE_IMAGE_SIZE_BYTES = 2L * 1024L * 1024L;

    // ===== 캐시 TTL (초 단위) =====
    /** 상품 캐시 TTL (5분). */
    public static final long PRODUCT_CACHE_TTL_SECONDS = 300L;
    /** 카테고리 캐시 TTL (1시간). */
    public static final long CATEGORY_CACHE_TTL_SECONDS = 3600L;
    /** 사용자 캐시 TTL (3분). */
    public static final long USER_CACHE_TTL_SECONDS = 180L;
    /** 브랜드 캐시 TTL (30분). */
    public static final long BRAND_CACHE_TTL_SECONDS = 1800L;
    /** 검색 결과 캐시 TTL (1분). */
    public static final long SEARCH_CACHE_TTL_SECONDS = 60L;
    /** 통계 캐시 TTL (10분). */
    public static final long STATS_CACHE_TTL_SECONDS = 600L;
    /** 쿠폰 캐시 TTL (2분). */
    public static final long COUPON_CACHE_TTL_SECONDS = 120L;
    /** 인기 상품 캐시 TTL (15분). */
    public static final long POPULAR_PRODUCT_CACHE_TTL_SECONDS = 900L;

    // ===== 비즈니스 규칙 =====
    /** 장바구니 최대 상품 수. */
    public static final int MAX_CART_ITEMS = 30;
    /** 사용자 당 최대 배송지 수. */
    public static final int MAX_ADDRESS_PER_USER = 10;
    /** 최대 로그인 시도 횟수. */
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    /** 리뷰 수정 가능 기간 (일). */
    public static final int REVIEW_EDIT_DAYS = 30;
    /** 주문 취소 가능 시간 (시간). */
    public static final int ORDER_CANCEL_HOURS = 24;
    /** 무료 배송 임계 금액 (5만원). */
    public static final long FREE_SHIPPING_THRESHOLD = 50_000L;
    /** 기본 배송비 (3천원). */
    public static final long SHIPPING_FEE = 3_000L;
    /** 쿠폰 코드 길이. */
    public static final int COUPON_CODE_LENGTH = 12;
    /** 주문 당 적용 가능 쿠폰 수. */
    public static final int MAX_COUPON_PER_ORDER = 1;
    /** 구매 후 리뷰 작성 가능 기간 (일). */
    public static final int REVIEW_WRITE_DAYS_LIMIT = 90;
    /** 비밀번호 최소 길이. */
    public static final int PASSWORD_MIN_LENGTH = 8;
    /** 비밀번호 최대 길이. */
    public static final int PASSWORD_MAX_LENGTH = 100;
    /** 사용자명 최소 길이. */
    public static final int USERNAME_MIN_LENGTH = 2;
    /** 사용자명 최대 길이. */
    public static final int USERNAME_MAX_LENGTH = 20;
    /** 리뷰 본문 최소 길이. */
    public static final int REVIEW_MIN_LENGTH = 10;
    /** 리뷰 본문 최대 길이. */
    public static final int REVIEW_MAX_LENGTH = 2000;
    /** 상품명 최대 길이. */
    public static final int PRODUCT_NAME_MAX_LENGTH = 200;
    /** 상품 설명 최대 길이. */
    public static final int PRODUCT_DESCRIPTION_MAX_LENGTH = 5000;
    /** 반품 가능 기간 (일). */
    public static final int RETURN_PERIOD_DAYS = 7;
    /** 자동 구매 확정 일수. */
    public static final int AUTO_PURCHASE_CONFIRM_DAYS = 14;

    // ===== API 버전 =====
    /** 현재 API 버전. */
    public static final String API_VERSION = "v1";
    /** API 기본 경로. */
    public static final String API_BASE_PATH = "/api/" + API_VERSION;
    /** 관리자 API 기본 경로. */
    public static final String ADMIN_API_BASE_PATH = API_BASE_PATH + "/admin";

    // ===== 날짜/시간 포맷 =====
    /** 날짜 포맷. */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    /** 날짜시간 포맷. */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /** ISO-8601 날짜시간 포맷. */
    public static final String DATETIME_ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    /** 시간 포맷. */
    public static final String TIME_FORMAT = "HH:mm:ss";

    // ===== 특수 사용자 =====
    /** 시스템 사용자명 (감사 로그용). */
    public static final String SYSTEM_USER = "SYSTEM";
    /** 익명 사용자명. */
    public static final String ANONYMOUS_USER = "ANONYMOUS";
    /** 배치 사용자명. */
    public static final String BATCH_USER = "BATCH";

    // ===== 통화 / 지역 =====
    /** 기본 통화 (KRW). */
    public static final String DEFAULT_CURRENCY = "KRW";
    /** 기본 국가. */
    public static final String DEFAULT_COUNTRY = "KR";
    /** 기본 언어. */
    public static final String DEFAULT_LANGUAGE = "ko";
    /** 기본 시간대. */
    public static final String DEFAULT_TIMEZONE = "Asia/Seoul";

    // ===== HTTP 헤더 =====
    /** Correlation ID 헤더. */
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    /** 클라이언트 버전 헤더. */
    public static final String HEADER_CLIENT_VERSION = "X-Client-Version";
    /** Request ID 헤더. */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
}
