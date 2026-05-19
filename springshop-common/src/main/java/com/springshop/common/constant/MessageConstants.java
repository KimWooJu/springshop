package com.springshop.common.constant;

/**
 * API 응답 메시지 상수.
 *
 * <p>비즈니스 도메인별 사용자 응답 메시지를 한곳에 모아 일관성과 다국어 전환을 용이하게 한다.</p>
 *
 * <p>실제 i18n이 필요한 경우 메시지 키를 추출하여 {@code MessageSource}로 이전한다.</p>
 */
public final class MessageConstants {

    private MessageConstants() {
        throw new AssertionError("MessageConstants는 인스턴스화할 수 없습니다");
    }

    // ===== 공통 =====
    public static final String MSG_SUCCESS = "요청이 성공적으로 처리되었습니다";
    public static final String MSG_CREATED = "성공적으로 등록되었습니다";
    public static final String MSG_UPDATED = "성공적으로 수정되었습니다";
    public static final String MSG_DELETED = "성공적으로 삭제되었습니다";
    public static final String MSG_NOT_FOUND = "요청한 리소스를 찾을 수 없습니다";
    public static final String MSG_DUPLICATE = "이미 존재하는 데이터입니다";
    public static final String MSG_NO_CONTENT = "조회된 데이터가 없습니다";

    // ===== 사용자 =====
    public static final String MSG_USER_REGISTERED = "회원가입이 완료되었습니다";
    public static final String MSG_USER_LOGIN_SUCCESS = "로그인이 성공하였습니다";
    public static final String MSG_USER_LOGOUT_SUCCESS = "로그아웃되었습니다";
    public static final String MSG_USER_PASSWORD_CHANGED = "비밀번호가 변경되었습니다";
    public static final String MSG_USER_WITHDRAWN = "회원 탈퇴가 완료되었습니다";
    public static final String MSG_USER_EMAIL_VERIFIED = "이메일 인증이 완료되었습니다";
    public static final String MSG_USER_NOT_FOUND = "존재하지 않는 사용자입니다";
    public static final String MSG_USER_LOCKED = "계정이 잠겼습니다. 잠시 후 다시 시도해주세요";
    public static final String MSG_USER_DISABLED = "비활성화된 계정입니다";
    public static final String MSG_USER_PROFILE_UPDATED = "프로필이 수정되었습니다";
    public static final String MSG_USER_EMAIL_SENT = "인증 메일을 발송하였습니다";
    public static final String MSG_USER_PASSWORD_RESET_SENT = "비밀번호 재설정 메일을 발송하였습니다";

    // ===== 상품 =====
    public static final String MSG_PRODUCT_CREATED = "상품이 등록되었습니다";
    public static final String MSG_PRODUCT_UPDATED = "상품이 수정되었습니다";
    public static final String MSG_PRODUCT_PUBLISHED = "상품이 판매 시작되었습니다";
    public static final String MSG_PRODUCT_DISCONTINUED = "상품이 판매 종료되었습니다";
    public static final String MSG_PRODUCT_OUT_OF_STOCK = "재고가 부족합니다";
    public static final String MSG_PRODUCT_NOT_FOUND = "존재하지 않는 상품입니다";
    public static final String MSG_PRODUCT_DELETED = "상품이 삭제되었습니다";

    // ===== 주문 =====
    public static final String MSG_ORDER_PLACED = "주문이 완료되었습니다";
    public static final String MSG_ORDER_CANCELLED = "주문이 취소되었습니다";
    public static final String MSG_ORDER_CONFIRMED = "주문이 확인되었습니다";
    public static final String MSG_ORDER_SHIPPED = "배송이 시작되었습니다";
    public static final String MSG_ORDER_DELIVERED = "배송이 완료되었습니다";
    public static final String MSG_ORDER_RETURN_REQUESTED = "반품 신청이 완료되었습니다";
    public static final String MSG_ORDER_NOT_FOUND = "존재하지 않는 주문입니다";
    public static final String MSG_ORDER_CANNOT_CANCEL = "취소할 수 없는 주문 상태입니다";
    public static final String MSG_ORDER_PURCHASE_CONFIRMED = "구매가 확정되었습니다";

    // ===== 결제 =====
    public static final String MSG_PAYMENT_SUCCESS = "결제가 완료되었습니다";
    public static final String MSG_PAYMENT_FAILED = "결제에 실패하였습니다";
    public static final String MSG_PAYMENT_CANCELLED = "결제가 취소되었습니다";
    public static final String MSG_REFUND_REQUESTED = "환불 신청이 완료되었습니다";
    public static final String MSG_REFUND_COMPLETED = "환불이 완료되었습니다";
    public static final String MSG_PAYMENT_PENDING = "결제가 진행 중입니다";

    // ===== 장바구니 =====
    public static final String MSG_CART_ITEM_ADDED = "장바구니에 상품이 담겼습니다";
    public static final String MSG_CART_ITEM_UPDATED = "장바구니 수량이 변경되었습니다";
    public static final String MSG_CART_ITEM_REMOVED = "장바구니에서 상품이 제거되었습니다";
    public static final String MSG_CART_CLEARED = "장바구니를 비웠습니다";
    public static final String MSG_COUPON_APPLIED = "쿠폰이 적용되었습니다";
    public static final String MSG_COUPON_REMOVED = "쿠폰이 제거되었습니다";
    public static final String MSG_COUPON_INVALID = "유효하지 않은 쿠폰입니다";
    public static final String MSG_COUPON_EXPIRED = "만료된 쿠폰입니다";

    // ===== 리뷰 =====
    public static final String MSG_REVIEW_SUBMITTED = "리뷰가 등록되었습니다. 검수 후 게시됩니다";
    public static final String MSG_REVIEW_APPROVED = "리뷰가 승인되었습니다";
    public static final String MSG_REVIEW_REJECTED = "리뷰가 반려되었습니다";
    public static final String MSG_REVIEW_UPDATED = "리뷰가 수정되었습니다";
    public static final String MSG_REVIEW_DELETED = "리뷰가 삭제되었습니다";
    public static final String MSG_REVIEW_ALREADY_WRITTEN = "이미 작성한 리뷰가 있습니다";

    // ===== 알림 =====
    public static final String MSG_NOTIFICATION_READ = "알림을 읽음 처리했습니다";
    public static final String MSG_ALL_NOTIFICATIONS_READ = "모든 알림을 읽음 처리했습니다";
    public static final String MSG_NOTIFICATION_DELETED = "알림이 삭제되었습니다";

    // ===== 오류 =====
    public static final String MSG_UNAUTHORIZED = "로그인이 필요합니다";
    public static final String MSG_FORBIDDEN = "접근 권한이 없습니다";
    public static final String MSG_VALIDATION_ERROR = "입력값을 확인해주세요";
    public static final String MSG_INTERNAL_ERROR = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요";
    public static final String MSG_SERVICE_UNAVAILABLE = "서비스가 일시적으로 사용 불가합니다";
    public static final String MSG_BAD_REQUEST = "잘못된 요청입니다";
    public static final String MSG_TOO_MANY_REQUESTS = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요";
    public static final String MSG_TOKEN_EXPIRED = "토큰이 만료되었습니다";
    public static final String MSG_TOKEN_INVALID = "유효하지 않은 토큰입니다";
    public static final String MSG_FILE_TOO_LARGE = "파일 크기가 너무 큽니다";
    public static final String MSG_FILE_TYPE_NOT_ALLOWED = "허용되지 않은 파일 형식입니다";
}
