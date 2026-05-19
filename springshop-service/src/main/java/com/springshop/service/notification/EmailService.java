package com.springshop.service.notification;

import java.util.List;
import java.util.Map;

/**
 * 이메일 발송 서비스 인터페이스.
 *
 * <p>텍스트/HTML 이메일 발송, 주문 확인·비밀번호 재설정·환영 이메일 등
 * 템플릿 기반 이메일 발송 메서드를 정의한다.
 *
 * <p>구현체는 {@link org.springframework.mail.javamail.JavaMailSender}를 사용하며,
 * HTML 템플릿은 Java 15+ Text Block으로 작성한다.
 */
public interface EmailService {

    /**
     * 일반 텍스트 이메일을 발송한다.
     *
     * @param to      수신자 이메일
     * @param subject 이메일 제목
     * @param text    이메일 본문
     */
    void sendEmail(String to, String subject, String text);

    /**
     * HTML 이메일을 발송한다.
     *
     * @param to       수신자 이메일
     * @param subject  이메일 제목
     * @param htmlBody HTML 본문
     */
    void sendHtmlEmail(String to, String subject, String htmlBody);

    /**
     * 여러 수신자에게 동일한 이메일을 발송한다.
     *
     * @param recipients 수신자 이메일 목록
     * @param subject    제목
     * @param htmlBody   HTML 본문
     * @return           발송 성공 수
     */
    int sendBulkEmail(List<String> recipients, String subject, String htmlBody);

    /**
     * 주문 완료 확인 이메일을 발송한다.
     *
     * @param to        수신자 이메일
     * @param orderNo   주문 번호
     * @param userName  사용자 이름
     * @param items     주문 상품 정보 목록
     * @param totalAmount 결제 금액
     */
    void sendOrderConfirmation(String to, String orderNo, String userName,
                               List<Map<String, String>> items, String totalAmount);

    /**
     * 비밀번호 재설정 링크 이메일을 발송한다.
     *
     * @param to        수신자 이메일
     * @param userName  사용자 이름
     * @param resetLink 재설정 링크 (유효 시간: 1시간)
     */
    void sendPasswordReset(String to, String userName, String resetLink);

    /**
     * 회원 가입 환영 이메일을 발송한다.
     *
     * @param to       수신자 이메일
     * @param userName 사용자 이름
     */
    void sendWelcome(String to, String userName);

    /**
     * 배송 시작 알림 이메일을 발송한다.
     *
     * @param to             수신자 이메일
     * @param orderNo        주문 번호
     * @param trackingNumber 운송장 번호
     * @param carrier        택배사
     */
    void sendOrderShipping(String to, String orderNo, String trackingNumber, String carrier);

    /**
     * 쿠폰 발급 알림 이메일을 발송한다.
     *
     * @param to         수신자 이메일
     * @param couponCode 쿠폰 코드
     * @param discount   할인 내용 (예: "10% 할인")
     * @param expiry     만료일
     */
    void sendCouponIssued(String to, String couponCode, String discount, String expiry);
}
