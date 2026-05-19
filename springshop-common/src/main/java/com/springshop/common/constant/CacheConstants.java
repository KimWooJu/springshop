package com.springshop.common.constant;

/**
 * 캐시 이름 및 TTL 상수.
 *
 * <p>Spring Cache 어노테이션({@code @Cacheable}, {@code @CacheEvict})에서 참조하는
 * 캐시 이름과 Caffeine/Redis 캐시 매니저 설정에 사용되는 TTL/최대 항목 수 값을 정의한다.</p>
 */
public final class CacheConstants {

    private CacheConstants() {
        throw new AssertionError("CacheConstants는 인스턴스화할 수 없습니다");
    }

    // ===== 캐시 이름 =====
    /** 상품 단건 캐시. */
    public static final String CACHE_PRODUCT = "products";
    /** 상품 요약 리스트 캐시. */
    public static final String CACHE_PRODUCT_SUMMARY = "product-summaries";
    /** 카테고리 캐시. */
    public static final String CACHE_CATEGORY = "categories";
    /** 카테고리 트리 캐시. */
    public static final String CACHE_CATEGORY_TREE = "category-tree";
    /** 브랜드 캐시. */
    public static final String CACHE_BRAND = "brands";
    /** 사용자 캐시. */
    public static final String CACHE_USER = "users";
    /** 사용자 프로필 캐시. */
    public static final String CACHE_USER_PROFILE = "user-profiles";
    /** 주문 캐시. */
    public static final String CACHE_ORDER = "orders";
    /** 재고 캐시. */
    public static final String CACHE_INVENTORY = "inventory";
    /** 리뷰 통계 캐시. */
    public static final String CACHE_REVIEW_STATS = "review-stats";
    /** 쿠폰 캐시. */
    public static final String CACHE_COUPON = "coupons";
    /** 알림 카운트 캐시. */
    public static final String CACHE_NOTIFICATION_COUNT = "notification-counts";
    /** 검색 결과 캐시. */
    public static final String CACHE_SEARCH_RESULT = "search-results";
    /** 인기 태그 캐시. */
    public static final String CACHE_POPULAR_TAGS = "popular-tags";
    /** 인기 상품 캐시. */
    public static final String CACHE_TOP_PRODUCTS = "top-products";
    /** 대시보드 통계 캐시. */
    public static final String CACHE_DASHBOARD_STATS = "dashboard-stats";
    /** 추천 상품 캐시. */
    public static final String CACHE_RECOMMENDATIONS = "recommendations";
    /** FAQ 캐시. */
    public static final String CACHE_FAQ = "faq";
    /** 설정값 캐시. */
    public static final String CACHE_SETTINGS = "settings";

    // ===== TTL (초 단위) =====
    /** 짧은 TTL (1분). */
    public static final long TTL_SHORT = 60L;
    /** 중간 TTL (5분). */
    public static final long TTL_MEDIUM = 300L;
    /** 긴 TTL (30분). */
    public static final long TTL_LONG = 1800L;
    /** 매우 긴 TTL (1시간). */
    public static final long TTL_VERY_LONG = 3600L;
    /** 하루 TTL (24시간). */
    public static final long TTL_DAY = 86_400L;
    /** 일주일 TTL. */
    public static final long TTL_WEEK = 604_800L;

    // ===== 최대 항목 수 =====
    /** 상품 캐시 최대 항목 수. */
    public static final long MAX_ENTRIES_PRODUCT = 1_000L;
    /** 카테고리 캐시 최대 항목 수. */
    public static final long MAX_ENTRIES_CATEGORY = 200L;
    /** 사용자 캐시 최대 항목 수. */
    public static final long MAX_ENTRIES_USER = 500L;
    /** 검색 결과 캐시 최대 항목 수. */
    public static final long MAX_ENTRIES_SEARCH = 200L;
    /** 주문 캐시 최대 항목 수. */
    public static final long MAX_ENTRIES_ORDER = 500L;
    /** 브랜드 캐시 최대 항목 수. */
    public static final long MAX_ENTRIES_BRAND = 100L;

    // ===== 초기 용량 =====
    public static final int INITIAL_CAPACITY_SMALL = 16;
    public static final int INITIAL_CAPACITY_MEDIUM = 64;
    public static final int INITIAL_CAPACITY_LARGE = 256;

    // ===== 키 생성 =====
    /** 캐시 키 구분자. */
    public static final String KEY_SEPARATOR = ":";
    /** 캐시 키 와일드카드. */
    public static final String KEY_WILDCARD = "*";
}
