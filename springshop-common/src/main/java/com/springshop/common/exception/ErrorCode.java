package com.springshop.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 시스템 전역 에러 코드 정의.
 * 도메인별 섹션으로 구분되며 각 코드는 HTTP 상태와 기본 메시지를 보유한다.
 *
 * 코드 체계: [도메인]_[순번 3자리]
 *   - USER: 사용자 관련
 *   - PRODUCT: 상품 관련
 *   - ORDER: 주문 관련
 *   - PAYMENT: 결제 관련
 *   - INVENTORY: 재고 관련
 *   - REVIEW: 리뷰 관련
 *   - COUPON: 쿠폰 관련
 *   - AUTH: 인증/인가 관련
 *   - SYSTEM: 시스템 공통
 */
public enum ErrorCode {

    // ========== USER (USER_001 ~ USER_010) ==========
    USER_NOT_FOUND("USER_001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다: %s"),
    USER_ALREADY_EXISTS("USER_002", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다: %s"),
    USER_EMAIL_DUPLICATED("USER_003", HttpStatus.CONFLICT, "이미 가입된 이메일입니다: %s"),
    USER_PHONE_DUPLICATED("USER_004", HttpStatus.CONFLICT, "이미 등록된 전화번호입니다: %s"),
    USER_DELETED("USER_005", HttpStatus.GONE, "탈퇴한 사용자입니다"),
    USER_LOCKED("USER_006", HttpStatus.LOCKED, "잠긴 계정입니다. 잠금 해제까지 %s분 남았습니다"),
    USER_INACTIVE("USER_007", HttpStatus.FORBIDDEN, "비활성 계정입니다"),
    USER_PROFILE_INVALID("USER_008", HttpStatus.BAD_REQUEST, "프로필 정보가 올바르지 않습니다"),
    USER_PASSWORD_MISMATCH("USER_009", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
    USER_OLD_PASSWORD_INVALID("USER_010", HttpStatus.BAD_REQUEST, "기존 비밀번호가 올바르지 않습니다"),

    // ========== PRODUCT (PRODUCT_001 ~ PRODUCT_010) ==========
    PRODUCT_NOT_FOUND("PRODUCT_001", HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다: %s"),
    PRODUCT_OUT_OF_STOCK("PRODUCT_002", HttpStatus.CONFLICT, "재고가 부족합니다: %s"),
    PRODUCT_NOT_AVAILABLE("PRODUCT_003", HttpStatus.GONE, "판매가 종료된 상품입니다: %s"),
    PRODUCT_CATEGORY_NOT_FOUND("PRODUCT_004", HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다: %s"),
    PRODUCT_BRAND_NOT_FOUND("PRODUCT_005", HttpStatus.NOT_FOUND, "브랜드를 찾을 수 없습니다: %s"),
    PRODUCT_OPTION_INVALID("PRODUCT_006", HttpStatus.BAD_REQUEST, "유효하지 않은 상품 옵션입니다"),
    PRODUCT_PRICE_INVALID("PRODUCT_007", HttpStatus.BAD_REQUEST, "유효하지 않은 가격입니다"),
    PRODUCT_NAME_DUPLICATED("PRODUCT_008", HttpStatus.CONFLICT, "이미 존재하는 상품명입니다: %s"),
    PRODUCT_IMAGE_LIMIT_EXCEEDED("PRODUCT_009", HttpStatus.BAD_REQUEST, "상품 이미지는 최대 %s장까지 등록할 수 있습니다"),
    PRODUCT_DESCRIPTION_TOO_LONG("PRODUCT_010", HttpStatus.BAD_REQUEST, "상품 설명이 너무 깁니다 (최대 %s자)"),

    // ========== ORDER (ORDER_001 ~ ORDER_010) ==========
    ORDER_NOT_FOUND("ORDER_001", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다: %s"),
    ORDER_ALREADY_PAID("ORDER_002", HttpStatus.CONFLICT, "이미 결제된 주문입니다"),
    ORDER_ALREADY_CANCELLED("ORDER_003", HttpStatus.CONFLICT, "이미 취소된 주문입니다"),
    ORDER_CANNOT_CANCEL("ORDER_004", HttpStatus.CONFLICT, "취소할 수 없는 주문 상태입니다: %s"),
    ORDER_EMPTY("ORDER_005", HttpStatus.BAD_REQUEST, "주문 항목이 없습니다"),
    ORDER_AMOUNT_INVALID("ORDER_006", HttpStatus.BAD_REQUEST, "주문 금액이 올바르지 않습니다"),
    ORDER_MIN_AMOUNT_REQUIRED("ORDER_007", HttpStatus.BAD_REQUEST, "최소 주문 금액 %s원 미달"),
    ORDER_DELIVERY_ADDRESS_REQUIRED("ORDER_008", HttpStatus.BAD_REQUEST, "배송지 정보가 필요합니다"),
    ORDER_STATUS_TRANSITION_INVALID("ORDER_009", HttpStatus.CONFLICT, "유효하지 않은 주문 상태 변경: %s -> %s"),
    ORDER_ACCESS_DENIED("ORDER_010", HttpStatus.FORBIDDEN, "본인의 주문만 접근 가능합니다"),

    // ========== PAYMENT (PAYMENT_001 ~ PAYMENT_010) ==========
    PAYMENT_NOT_FOUND("PAYMENT_001", HttpStatus.NOT_FOUND, "결제 내역을 찾을 수 없습니다"),
    PAYMENT_FAILED("PAYMENT_002", HttpStatus.PAYMENT_REQUIRED, "결제에 실패했습니다: %s"),
    PAYMENT_TIMEOUT("PAYMENT_003", HttpStatus.REQUEST_TIMEOUT, "결제 요청이 시간 초과되었습니다"),
    PAYMENT_AMOUNT_MISMATCH("PAYMENT_004", HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다"),
    PAYMENT_METHOD_INVALID("PAYMENT_005", HttpStatus.BAD_REQUEST, "유효하지 않은 결제 수단입니다"),
    PAYMENT_ALREADY_REFUNDED("PAYMENT_006", HttpStatus.CONFLICT, "이미 환불된 결제입니다"),
    PAYMENT_REFUND_FAILED("PAYMENT_007", HttpStatus.PAYMENT_REQUIRED, "환불 처리에 실패했습니다: %s"),
    PAYMENT_PG_ERROR("PAYMENT_008", HttpStatus.BAD_GATEWAY, "PG사 오류: %s"),
    PAYMENT_CARD_DECLINED("PAYMENT_009", HttpStatus.PAYMENT_REQUIRED, "카드 결제가 거절되었습니다"),
    PAYMENT_INSUFFICIENT_BALANCE("PAYMENT_010", HttpStatus.PAYMENT_REQUIRED, "잔액이 부족합니다"),

    // ========== INVENTORY (INVENTORY_001 ~ INVENTORY_005) ==========
    INVENTORY_NOT_FOUND("INVENTORY_001", HttpStatus.NOT_FOUND, "재고 정보를 찾을 수 없습니다"),
    INVENTORY_INSUFFICIENT("INVENTORY_002", HttpStatus.CONFLICT, "재고 부족 (요청: %s, 가용: %s)"),
    INVENTORY_LOCK_TIMEOUT("INVENTORY_003", HttpStatus.REQUEST_TIMEOUT, "재고 락 획득 실패"),
    INVENTORY_NEGATIVE_QUANTITY("INVENTORY_004", HttpStatus.BAD_REQUEST, "재고 수량은 음수일 수 없습니다"),
    INVENTORY_RESERVATION_EXPIRED("INVENTORY_005", HttpStatus.GONE, "재고 예약이 만료되었습니다"),

    // ========== REVIEW (REVIEW_001 ~ REVIEW_006) ==========
    REVIEW_NOT_FOUND("REVIEW_001", HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다"),
    REVIEW_ALREADY_EXISTS("REVIEW_002", HttpStatus.CONFLICT, "이미 작성한 리뷰가 있습니다"),
    REVIEW_RATING_INVALID("REVIEW_003", HttpStatus.BAD_REQUEST, "평점은 1-5 사이여야 합니다"),
    REVIEW_NOT_PURCHASED("REVIEW_004", HttpStatus.FORBIDDEN, "구매 후에만 리뷰 작성이 가능합니다"),
    REVIEW_CONTENT_REQUIRED("REVIEW_005", HttpStatus.BAD_REQUEST, "리뷰 내용은 필수입니다"),
    REVIEW_IMAGE_LIMIT_EXCEEDED("REVIEW_006", HttpStatus.BAD_REQUEST, "리뷰 이미지는 최대 %s장까지 등록 가능합니다"),

    // ========== COUPON (COUPON_001 ~ COUPON_007) ==========
    COUPON_NOT_FOUND("COUPON_001", HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다: %s"),
    COUPON_EXPIRED("COUPON_002", HttpStatus.GONE, "만료된 쿠폰입니다"),
    COUPON_ALREADY_USED("COUPON_003", HttpStatus.CONFLICT, "이미 사용된 쿠폰입니다"),
    COUPON_NOT_APPLICABLE("COUPON_004", HttpStatus.BAD_REQUEST, "적용 불가능한 쿠폰입니다"),
    COUPON_MIN_ORDER_NOT_MET("COUPON_005", HttpStatus.BAD_REQUEST, "최소 주문 금액 미달 (필요: %s원)"),
    COUPON_LIMIT_EXCEEDED("COUPON_006", HttpStatus.CONFLICT, "쿠폰 발급 한도 초과"),
    COUPON_CODE_INVALID("COUPON_007", HttpStatus.BAD_REQUEST, "유효하지 않은 쿠폰 코드입니다"),

    // ========== AUTH (AUTH_001 ~ AUTH_010) ==========
    AUTH_INVALID_CREDENTIALS("AUTH_001", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    AUTH_TOKEN_EXPIRED("AUTH_002", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
    AUTH_TOKEN_INVALID("AUTH_003", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    AUTH_TOKEN_REQUIRED("AUTH_004", HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다"),
    AUTH_REFRESH_TOKEN_INVALID("AUTH_005", HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다"),
    AUTH_PERMISSION_DENIED("AUTH_006", HttpStatus.FORBIDDEN, "권한이 없습니다"),
    AUTH_ROLE_REQUIRED("AUTH_007", HttpStatus.FORBIDDEN, "필요한 권한: %s"),
    AUTH_LOGIN_ATTEMPTS_EXCEEDED("AUTH_008", HttpStatus.TOO_MANY_REQUESTS, "로그인 시도 횟수 초과. %s분 후 재시도하세요"),
    AUTH_SESSION_EXPIRED("AUTH_009", HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다"),
    AUTH_TOKEN_BLACKLISTED("AUTH_010", HttpStatus.UNAUTHORIZED, "차단된 토큰입니다"),

    // ========== SYSTEM (SYSTEM_001 ~ SYSTEM_010) ==========
    SYSTEM_INTERNAL_ERROR("SYSTEM_001", HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다"),
    SYSTEM_VALIDATION_FAILED("SYSTEM_002", HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다"),
    SYSTEM_REQUEST_INVALID("SYSTEM_003", HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다"),
    SYSTEM_METHOD_NOT_ALLOWED("SYSTEM_004", HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다"),
    SYSTEM_NOT_FOUND("SYSTEM_005", HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
    SYSTEM_RATE_LIMIT_EXCEEDED("SYSTEM_006", HttpStatus.TOO_MANY_REQUESTS, "요청 횟수 초과. 잠시 후 재시도하세요"),
    SYSTEM_EXTERNAL_SERVICE_ERROR("SYSTEM_007", HttpStatus.BAD_GATEWAY, "외부 서비스 오류: %s"),
    SYSTEM_DATABASE_ERROR("SYSTEM_008", HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 오류"),
    SYSTEM_CACHE_ERROR("SYSTEM_009", HttpStatus.INTERNAL_SERVER_ERROR, "캐시 시스템 오류"),
    SYSTEM_FILE_UPLOAD_FAILED("SYSTEM_010", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 실패: %s");

    private final String code;
    private final HttpStatus httpStatus;
    private final String messageTemplate;

    ErrorCode(String code, HttpStatus httpStatus, String messageTemplate) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.messageTemplate = messageTemplate;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public String format(Object... args) {
        if (args == null || args.length == 0) {
            return messageTemplate;
        }
        try {
            return String.format(messageTemplate, args);
        } catch (Exception e) {
            return messageTemplate;
        }
    }

    public static ErrorCode fromCode(String code) {
        for (ErrorCode ec : values()) {
            if (ec.code.equals(code)) {
                return ec;
            }
        }
        return SYSTEM_INTERNAL_ERROR;
    }
}
