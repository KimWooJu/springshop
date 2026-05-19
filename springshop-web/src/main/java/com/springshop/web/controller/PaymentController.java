package com.springshop.web.controller;

import com.springshop.web.dto.request.ProcessPaymentRequest;
import com.springshop.web.dto.request.RefundRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.PaymentResponse;
import com.springshop.web.dto.response.RefundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 결제 API 컨트롤러.
 *
 * <p>결제 처리, 결제 상세 조회, 사용자 결제 이력, 환불, 결제 상태 조회,
 * 결제 취소, 관리자용 결제 목록 등 결제 도메인 전체 엔드포인트를 제공한다.
 *
 * <p>PG(Payment Gateway) 인증은 클라이언트가 먼저 수행하여 pgToken을 발급받은 후
 * 서버에 전달하는 방식을 사용한다.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "결제 API", description = "결제 처리 및 환불")
public class PaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(90000);

    @Operation(summary = "결제 처리", description = "주문에 대한 결제를 PG와 연동하여 처리한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "결제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "결제 정보 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "PG 거절")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        LOG.info("결제 처리 요청 - orderId={}, method={}, amount={}, installment={}",
                request.orderId(), request.paymentMethodType(), request.amount(), request.installmentMonths());

        // Java 25 switch pattern: 결제 수단별 분기
        String pgProvider = switch (request.paymentMethodType()) {
            case "CARD" -> "TOSS_PAYMENTS";
            case "KAKAOPAY" -> "KAKAO_PAY";
            case "NAVERPAY" -> "NAVER_PAY";
            case "PAYCO" -> "NHN_PAYCO";
            case "BANK_TRANSFER", "VBANK" -> "KG_INICIS";
            case "POINT_ONLY" -> "INTERNAL_POINT";
            case null -> "UNKNOWN";
            default -> "GENERIC_PG";
        };

        Long newId = ID_SEQ.incrementAndGet();
        PaymentResponse data = buildSamplePayment(newId, request.orderId(),
                request.paymentMethodType(), request.amount(), "PAID", pgProvider);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "결제가 완료되었습니다"));
    }

    @Operation(summary = "결제 상세 조회", description = "결제 ID로 결제 상세 정보를 조회한다.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @Parameter(description = "결제 ID", required = true) @PathVariable Long id) {
        LOG.debug("결제 상세 조회 - id={}", id);
        PaymentResponse data = buildSamplePayment(id, 50001L, "CREDIT_CARD",
                new BigDecimal("42800"), "PAID", "TOSS_PAYMENTS");
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "내 결제 이력", description = "로그인한 사용자의 결제 이력을 페이징 조회한다.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method) {
        LOG.debug("결제 이력 조회 - page={}, size={}, status={}", page, size, status);
        List<PaymentResponse> payments = buildSamplePaymentList(size);
        return ResponseEntity.ok(ApiResponse.Paginated.of(payments, payments.size() * 10L, page, size));
    }

    @Operation(summary = "환불 요청", description = "결제에 대한 환불을 요청한다. 전체/부분/아이템 환불을 지원한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "환불 요청 접수"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "환불 불가 상태")
    })
    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RefundResponse>> requestRefund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        LOG.info("환불 요청 - paymentId={}, orderId={}, type={}, amount={}",
                id, request.orderId(), request.refundType(), request.amount());

        BigDecimal refundAmount = switch (request.refundType()) {
            case "FULL" -> new BigDecimal("42800");
            case "PARTIAL" -> request.amount();
            case "ITEM" -> request.amount() == null ? new BigDecimal("19900") : request.amount();
            default -> BigDecimal.ZERO;
        };

        Long refundId = ID_SEQ.incrementAndGet();
        RefundResponse data = RefundResponse.requested(
                refundId, id, request.orderId(),
                refundAmount, request.reason(), request.isPartialRefund());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "환불이 요청되었습니다"));
    }

    @Operation(summary = "결제 상태 조회", description = "결제 진행 상태를 폴링하기 위한 경량 조회.")
    @GetMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStatus(
            @PathVariable Long id) {
        LOG.debug("결제 상태 조회 - id={}", id);
        Map<String, Object> status = Map.of(
                "paymentId", id,
                "status", "PAID",
                "statusDisplay", "결제완료",
                "lastUpdatedAt", LocalDateTime.now().toString(),
                "amount", new BigDecimal("42800"),
                "currency", "KRW"
        );
        return ResponseEntity.ok(ApiResponse.Success.of(status));
    }

    @Operation(summary = "결제 취소", description = "결제 직후 짧은 시간 내에 결제를 취소한다 (PG 취소 호출).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "취소 불가")
    })
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        LOG.warn("결제 취소 요청 - id={}, reason={}", id, reason);
        PaymentResponse data = buildSamplePayment(id, 50001L, "CREDIT_CARD",
                new BigDecimal("42800"), "CANCELLED", "TOSS_PAYMENTS");
        return ResponseEntity.ok(ApiResponse.Success.of(data, "결제가 취소되었습니다"));
    }

    @Operation(summary = "관리자 결제 목록", description = "관리자가 전체 결제 목록을 페이징 조회한다.")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> adminPaymentList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pgProvider,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        LOG.info("관리자 결제 목록 - page={}, size={}, status={}, pg={}", page, size, status, pgProvider);
        List<PaymentResponse> payments = buildSamplePaymentList(size);
        return ResponseEntity.ok(ApiResponse.Paginated.of(payments, 1234L, page, size));
    }

    @Operation(summary = "환불 상세 조회", description = "환불 ID로 환불 처리 상세를 조회한다.")
    @GetMapping("/refunds/{refundId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(
            @PathVariable Long refundId) {
        LOG.debug("환불 상세 조회 - refundId={}", refundId);
        RefundResponse data = new RefundResponse(
                refundId, 90001L, 50001L, new BigDecimal("19900"), "KRW",
                "단순 변심", "검수 후 처리",
                "ORIGINAL", "PROCESSING", "CREDIT_CARD",
                "rfd-" + UUID.randomUUID(), null, null, null,
                false, LocalDateTime.now().minusDays(1), null,
                LocalDateTime.now().plusDays(2), null);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "환불 승인 (관리자)", description = "관리자가 요청된 환불을 승인한다.")
    @PostMapping("/refunds/{refundId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> approveRefund(
            @PathVariable Long refundId,
            @RequestParam(required = false) String adminNote) {
        LOG.info("환불 승인 - refundId={}, note={}", refundId, adminNote);
        RefundResponse data = new RefundResponse(
                refundId, 90001L, 50001L, new BigDecimal("19900"), "KRW",
                "단순 변심", adminNote,
                "ORIGINAL", "APPROVED", "CREDIT_CARD",
                "rfd-" + UUID.randomUUID(), null, null, null,
                false, LocalDateTime.now().minusDays(1), LocalDateTime.now(),
                LocalDateTime.now().plusDays(1), null);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "환불이 승인되었습니다"));
    }

    @Operation(summary = "환불 거절 (관리자)", description = "관리자가 환불 요청을 거절한다.")
    @PostMapping("/refunds/{refundId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> rejectRefund(
            @PathVariable Long refundId,
            @RequestParam String rejectReason) {
        LOG.warn("환불 거절 - refundId={}, reason={}", refundId, rejectReason);
        RefundResponse data = new RefundResponse(
                refundId, 90001L, 50001L, BigDecimal.ZERO, "KRW",
                "단순 변심", null,
                "ORIGINAL", "REJECTED", "CREDIT_CARD",
                null, null, null, null,
                false, LocalDateTime.now().minusDays(1), LocalDateTime.now(),
                null, rejectReason);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "환불이 거절되었습니다"));
    }

    // ---------------------------------------
    // 헬퍼
    // ---------------------------------------

    private PaymentResponse buildSamplePayment(Long id, Long orderId, String method,
                                               BigDecimal amount, String status, String pg) {
        LocalDateTime now = LocalDateTime.now();
        return new PaymentResponse(
                id, orderId, method, amount, "KRW", status,
                pg, "txn-" + UUID.randomUUID(), "AUTH-" + (id % 1000000),
                "삼성카드", "1234-****-****-5678", 0,
                null, null, null,
                amount, List.of(),
                now.minusMinutes(2), "PAID".equals(status) ? now.minusMinutes(1) : null,
                null);
    }

    private List<PaymentResponse> buildSamplePaymentList(int size) {
        List<PaymentResponse> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buildSamplePayment(
                    (long) (90000 + i), (long) (50000 + i), "CREDIT_CARD",
                    new BigDecimal("42800"), "PAID", "TOSS_PAYMENTS"));
        }
        return list;
    }
}
