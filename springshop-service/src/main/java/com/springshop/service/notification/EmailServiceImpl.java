package com.springshop.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * {@link EmailService} 구현체.
 *
 * <p>Spring의 {@link JavaMailSender}로 SMTP 이메일을 발송한다.
 * HTML 이메일 템플릿은 Java 15+ Text Block으로 작성되어 가독성이 높다.
 * 대량 발송은 Virtual Thread 기반 Executor로 비동기 처리한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String FROM = "noreply@springshop.com";
    private static final String APP_NAME = "SpringShop";

    private final JavaMailSender mailSender;

    @Override
    @Async
    public void sendEmail(String to, String subject, String text) {
        log.debug("텍스트 이메일 발송 - to={}, subject={}", to, subject);
        try {
            var message = new SimpleMailMessage();
            message.setFrom(FROM);
            message.setTo(to);
            message.setSubject("[%s] %s".formatted(APP_NAME, subject));
            message.setText(text);
            mailSender.send(message);
            log.info("이메일 발송 완료 - to={}", to);
        } catch (Exception ex) {
            log.error("이메일 발송 실패 - to={}, error={}", to, ex.getMessage(), ex);
        }
    }

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.debug("HTML 이메일 발송 - to={}, subject={}", to, subject);
        try {
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(FROM);
            helper.setTo(to);
            helper.setSubject("[%s] %s".formatted(APP_NAME, subject));
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("HTML 이메일 발송 완료 - to={}", to);
        } catch (Exception ex) {
            log.error("HTML 이메일 발송 실패 - to={}, error={}", to, ex.getMessage(), ex);
        }
    }

    @Override
    public int sendBulkEmail(List<String> recipients, String subject, String htmlBody) {
        log.info("대량 이메일 발송 시작 - count={}", recipients.size());
        var futures = new ArrayList<CompletableFuture<Boolean>>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String recipient : recipients) {
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        sendHtmlEmail(recipient, subject, htmlBody);
                        return true;
                    } catch (Exception ex) {
                        log.warn("대량 발송 실패 - to={}", recipient);
                        return false;
                    }
                }, executor);
                futures.add(future);
            }
        }

        int successCount = (int) futures.stream()
                .map(CompletableFuture::join)
                .filter(Boolean::booleanValue)
                .count();

        log.info("대량 이메일 발송 완료 - success={}/{}", successCount, recipients.size());
        return successCount;
    }

    @Override
    @Async
    public void sendOrderConfirmation(String to, String orderNo, String userName,
                                      List<Map<String, String>> items, String totalAmount) {
        log.info("주문 확인 이메일 - to={}, orderNo={}", to, orderNo);

        String itemsHtml = buildItemsHtml(items);
        String htmlBody = """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"><title>주문 확인</title></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #333;">안녕하세요, %s님!</h2>
                  <p>주문 <strong>#%s</strong>이(가) 성공적으로 접수되었습니다.</p>
                  <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                    <thead>
                      <tr style="background-color: #f5f5f5;">
                        <th style="padding: 10px; border: 1px solid #ddd;">상품</th>
                        <th style="padding: 10px; border: 1px solid #ddd;">수량</th>
                        <th style="padding: 10px; border: 1px solid #ddd;">금액</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                  <p style="font-size: 18px; font-weight: bold;">합계: %s원</p>
                  <p style="color: #666; font-size: 12px;">문의사항은 고객센터로 연락해 주세요.</p>
                </body>
                </html>
                """.formatted(userName, orderNo, itemsHtml, totalAmount);

        sendHtmlEmail(to, "주문 확인 - #" + orderNo, htmlBody);
    }

    @Override
    @Async
    public void sendPasswordReset(String to, String userName, String resetLink) {
        log.info("비밀번호 재설정 이메일 - to={}", to);
        String htmlBody = """
                <!DOCTYPE html>
                <html lang="ko">
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2>비밀번호 재설정</h2>
                  <p>%s님, 비밀번호 재설정 요청이 접수되었습니다.</p>
                  <p>아래 버튼을 클릭하여 비밀번호를 재설정하세요. 링크는 1시간 후 만료됩니다.</p>
                  <a href="%s" style="display: inline-block; padding: 12px 24px;
                     background-color: #4CAF50; color: white; text-decoration: none;
                     border-radius: 4px; margin: 20px 0;">비밀번호 재설정</a>
                  <p style="color: #999; font-size: 12px;">이 요청을 하지 않으셨다면 이 이메일을 무시하세요.</p>
                </body>
                </html>
                """.formatted(userName, resetLink);
        sendHtmlEmail(to, "비밀번호 재설정", htmlBody);
    }

    @Override
    @Async
    public void sendWelcome(String to, String userName) {
        log.info("환영 이메일 - to={}", to);
        String htmlBody = """
                <!DOCTYPE html>
                <html lang="ko">
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h1 style="color: #4CAF50;">SpringShop에 오신 것을 환영합니다! 🎉</h1>
                  <p>%s님, 회원 가입을 축하드립니다.</p>
                  <p>SpringShop에서 다양한 상품을 만나보세요.</p>
                  <ul>
                    <li>신규 가입 쿠폰 10%% 할인</li>
                    <li>첫 주문 무료 배송</li>
                    <li>다양한 프로모션 혜택</li>
                  </ul>
                </body>
                </html>
                """.formatted(userName);
        sendHtmlEmail(to, "SpringShop 가입을 환영합니다", htmlBody);
    }

    @Override
    @Async
    public void sendOrderShipping(String to, String orderNo, String trackingNumber, String carrier) {
        log.info("배송 시작 이메일 - to={}, orderNo={}, tracking={}", to, orderNo, trackingNumber);
        String htmlBody = """
                <!DOCTYPE html>
                <html lang="ko">
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2>🚚 배송이 시작되었습니다!</h2>
                  <p>주문 <strong>#%s</strong>이(가) 발송되었습니다.</p>
                  <table style="border-collapse: collapse; margin: 20px 0;">
                    <tr><td style="padding: 8px; font-weight: bold;">택배사</td><td>%s</td></tr>
                    <tr><td style="padding: 8px; font-weight: bold;">운송장 번호</td><td>%s</td></tr>
                  </table>
                </body>
                </html>
                """.formatted(orderNo, carrier, trackingNumber);
        sendHtmlEmail(to, "배송 시작 알림 - #" + orderNo, htmlBody);
    }

    @Override
    @Async
    public void sendCouponIssued(String to, String couponCode, String discount, String expiry) {
        log.info("쿠폰 발급 이메일 - to={}, couponCode={}", to, couponCode);
        String htmlBody = """
                <!DOCTYPE html>
                <html lang="ko">
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2>🎁 쿠폰이 발급되었습니다!</h2>
                  <div style="background-color: #f9f9f9; border: 2px dashed #4CAF50;
                              padding: 20px; text-align: center; margin: 20px 0;">
                    <p style="font-size: 24px; font-weight: bold; color: #4CAF50;">%s</p>
                    <p style="font-size: 18px;">%s</p>
                    <p style="color: #999;">유효기간: %s</p>
                  </div>
                </body>
                </html>
                """.formatted(couponCode, discount, expiry);
        sendHtmlEmail(to, "쿠폰 발급 안내 - " + discount, htmlBody);
    }

    private String buildItemsHtml(List<Map<String, String>> items) {
        var sb = new StringBuilder();
        for (var item : items) {
            sb.append("""
                    <tr>
                      <td style="padding: 8px; border: 1px solid #ddd;">%s</td>
                      <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">%s</td>
                      <td style="padding: 8px; border: 1px solid #ddd; text-align: right;">%s원</td>
                    </tr>
                    """.formatted(
                    item.getOrDefault("name", ""),
                    item.getOrDefault("quantity", "1"),
                    item.getOrDefault("price", "0")
            ));
        }
        return sb.toString();
    }
}
