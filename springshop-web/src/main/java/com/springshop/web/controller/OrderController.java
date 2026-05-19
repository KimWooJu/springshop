package com.springshop.web.controller;

import com.springshop.web.dto.request.CreateOrderRequest;
import com.springshop.web.dto.request.UpdateOrderStatusRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.OrderResponse;
import com.springshop.web.dto.response.OrderSummaryResponse;
import com.springshop.web.dto.response.OrderTrackingResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 주문 API 컨트롤러.
 *
 * <p>주문 생성, 내 주문 목록, 주문 상세, 취소, 상태 변경(관리자), 배송 추적,
 * 반품 요청, 통계 등 주문 도메인의 모든 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "주문 API", description = "주문 생성/조회/취소/배송 추적/반품")
public class OrderController {

    private static final Logger LOG = LoggerFactory.getLogger(OrderController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(50000);

    @Operation(summary = "주문 생성", description = "장바구니 결제 단계에서 주문을 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "주문 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "재고 부족")
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        LOG.info("주문 생성 - itemCount={}, paymentMethod={}, totalQty={}",
                request.items().size(), request.paymentMethod(), request.totalQuantity());
        Long newId = ID_SEQ.incrementAndGet();
        String orderNumber = buildOrderNumber(newId);
        OrderResponse data = buildSampleOrder(newId, orderNumber, "PENDING");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "주문이 생성되었습니다"));
    }

    @Operation(summary = "내 주문 목록", description = "로그인한 사용자의 주문 목록을 페이징 조회한다.")
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        LOG.debug("내 주문 목록 - page={}, size={}, status={}", page, size, status);
        List<OrderSummaryResponse> list = buildSampleSummaries(size);
        return ResponseEntity.ok(ApiResponse.Success.of(list,
                "총 " + list.size() + "건"));
    }

    @Operation(summary = "최근 주문 (TOP 5)", description = "마이페이지 최근 주문 위젯용.")
    @GetMapping("/my/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> recentOrders(
            @RequestParam(defaultValue = "5") int limit) {
        LOG.debug("최근 주문 - limit={}", limit);
        List<OrderSummaryResponse> list = buildSampleSummaries(Math.min(limit, 5));
        return ResponseEntity.ok(ApiResponse.Success.of(list));
    }

    @Operation(summary = "주문 상세 조회", description = "주문 ID로 상세 정보를 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "주문 ID", required = true) @PathVariable Long id) {
        LOG.debug("주문 상세 조회 - id={}", id);
        OrderResponse data = buildSampleOrder(id, buildOrderNumber(id), "PAID");
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "주문 취소", description = "본인의 주문을 취소한다 (PENDING/PAID/PREPARING 상태만).")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        LOG.warn("주문 취소 요청 - id={}, reason={}", id, reason);
        OrderResponse data = buildSampleOrder(id, buildOrderNumber(id), "CANCELLED");
        return ResponseEntity.ok(ApiResponse.Success.of(data, "주문이 취소되었습니다"));
    }

    @Operation(summary = "주문 상태 변경 (관리자)", description = "관리자가 주문 상태를 변경한다.")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        LOG.info("주문 상태 변경 - id={}, status={}", id, request.newStatus());
        OrderResponse data = buildSampleOrder(id, buildOrderNumber(id), request.newStatus());
        return ResponseEntity.ok(ApiResponse.Success.of(data, "상태 변경 완료"));
    }

    @Operation(summary = "배송 추적", description = "주문의 배송 진행 상황을 조회한다.")
    @GetMapping("/{id}/tracking")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> tracking(
            @PathVariable Long id) {
        LOG.debug("배송 추적 - orderId={}", id);
        List<OrderTrackingResponse.TrackingStep> steps = List.of(
                OrderTrackingResponse.TrackingStep.completed(
                        LocalDateTime.now().minusDays(3), "상품 준비", "PREPARING", "상품 준비 완료"),
                OrderTrackingResponse.TrackingStep.completed(
                        LocalDateTime.now().minusDays(2), "서울 강남 집하장", "SHIPPED", "발송 처리"),
                OrderTrackingResponse.TrackingStep.completed(
                        LocalDateTime.now().minusDays(1), "수도권 허브", "IN_TRANSIT", "배송 중"),
                OrderTrackingResponse.TrackingStep.pending("OUT_FOR_DELIVERY", "출발 대기")
        );
        OrderTrackingResponse data = new OrderTrackingResponse(
                id, buildOrderNumber(id), "SHIPPED", OrderSummaryResponse.toStatusDisplay("SHIPPED"),
                "CJ_LOGISTICS", "CJ대한통운", "1234-5678-9012",
                "https://tracker.cj-logistics.com/?tk=1234-5678-9012",
                LocalDateTime.now().minusDays(2), null,
                LocalDate.now().plusDays(1),
                "홍길동", "서울특별시 강남구",
                steps, OrderTrackingResponse.computeProgress("SHIPPED"));
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "반품 신청", description = "배송 완료된 주문에 대해 반품을 신청한다 (7일 이내).")
    @PostMapping("/{id}/return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestReturn(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(required = false) String detail) {
        LOG.info("반품 요청 - orderId={}, reason={}", id, reason);
        Map<String, Object> result = Map.of(
                "orderId", id,
                "returnId", ID_SEQ.incrementAndGet(),
                "status", "REQUESTED",
                "reason", reason,
                "detail", detail == null ? "" : detail,
                "requestedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(result, "반품 요청이 접수되었습니다"));
    }

    @Operation(summary = "교환 신청", description = "배송 완료된 주문에 대해 교환을 신청한다.")
    @PostMapping("/{id}/exchange")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestExchange(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(required = false) Long newVariantId) {
        LOG.info("교환 요청 - orderId={}, reason={}", id, reason);
        Map<String, Object> result = Map.of(
                "orderId", id,
                "exchangeId", ID_SEQ.incrementAndGet(),
                "status", "REQUESTED",
                "reason", reason,
                "newVariantId", newVariantId == null ? 0L : newVariantId,
                "requestedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(result, "교환 요청이 접수되었습니다"));
    }

    @Operation(summary = "주문 통계 (관리자)", description = "기간별 주문 건수/금액 통계.")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(
            @RequestParam(defaultValue = "TODAY") String period) {
        LOG.debug("주문 통계 - period={}", period);
        Map<String, Object> stats = Map.of(
                "period", period,
                "totalOrders", 1234L,
                "totalRevenue", new BigDecimal("12345678"),
                "averageOrderValue", new BigDecimal("32100"),
                "cancellationRate", 0.043,
                "topPaymentMethod", "CREDIT_CARD",
                "calculatedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(stats));
    }

    @Operation(summary = "주문 검색 (관리자)", description = "다양한 조건으로 주문을 검색한다.")
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderSummaryResponse>>> searchOrders(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LOG.debug("주문 검색 - orderNumber={}, status={}, from={}, to={}",
                orderNumber, status, from, to);
        List<OrderSummaryResponse> list = buildSampleSummaries(size);
        return ResponseEntity.ok(ApiResponse.Success.of(list, "검색 완료"));
    }

    @Operation(summary = "주문서 PDF 다운로드 URL", description = "주문 PDF 다운로드 URL을 발급한다.")
    @GetMapping("/{id}/invoice")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> invoiceUrl(
            @PathVariable Long id) {
        Map<String, String> data = Map.of(
                "orderId", id.toString(),
                "downloadUrl", "https://cdn.example.com/invoices/" + buildOrderNumber(id) + ".pdf",
                "expiresAt", LocalDateTime.now().plusMinutes(10).toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    // -------------------------------------
    // 헬퍼
    // -------------------------------------

    private String buildOrderNumber(Long id) {
        return "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + id;
    }

    private OrderResponse buildSampleOrder(Long id, String orderNumber, String status) {
        List<OrderResponse.OrderItemRef> items = List.of(
                new OrderResponse.OrderItemRef(
                        1L, 10001L, "샘플 상품", "https://cdn.example.com/p.jpg",
                        2001L, "색상", "블랙", 2,
                        new BigDecimal("19900"), new BigDecimal("39800"))
        );
        OrderResponse.ShippingInfo shipping = new OrderResponse.ShippingInfo(
                "홍길동", "010-1234-5678", "06236",
                "서울특별시 강남구 테헤란로 123", "456호",
                "CJ대한통운", "1234-5678-9012",
                "문 앞에 놓아주세요", LocalDate.now().plusDays(2));
        OrderResponse.PaymentInfo payment = new OrderResponse.PaymentInfo(
                ID_SEQ.incrementAndGet(), "CREDIT_CARD", status, new BigDecimal("42800"),
                "txn-" + java.util.UUID.randomUUID(), "TOSS_PAYMENTS");
        OrderResponse.AmountBreakdown amounts = new OrderResponse.AmountBreakdown(
                new BigDecimal("39800"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("3000"), BigDecimal.ZERO, new BigDecimal("42800"),
                0, 428);
        OrderResponse.CouponApplied coupon = null;
        return new OrderResponse(
                id, orderNumber, 1024L, status, items, shipping, payment, amounts, coupon,
                "주문 메모",
                LocalDateTime.now().minusHours(2),
                "PAID".equals(status) ? LocalDateTime.now().minusHours(2) : null,
                null, null);
    }

    private List<OrderSummaryResponse> buildSampleSummaries(int size) {
        List<OrderSummaryResponse> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Long id = (long) (50000 + i);
            list.add(OrderSummaryResponse.of(
                    id, buildOrderNumber(id), "PAID",
                    "샘플 상품 " + i, "https://cdn.example.com/p" + i + ".jpg",
                    2, new BigDecimal("42800"),
                    LocalDateTime.now().minusDays(i)));
        }
        return list;
    }
}
