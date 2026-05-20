package com.springshop.web.controller;

import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.InventoryResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 재고 API 컨트롤러.
 *
 * <p>재고 조회, 수동 조정, 안전 재고 알림, 재고 부족 상품 목록, ERP 동기화,
 * 재고 변동 이력 조회 등 재고 도메인의 모든 엔드포인트를 제공한다.
 *
 * <p>대부분의 엔드포인트는 관리자 권한이 필요하며, 단순 조회만 일반 사용자에게 노출된다.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "재고 API", description = "재고 조회 및 관리")
public class InventoryController {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(60000);

    @Operation(summary = "재고 목록 (관리자)", description = "전체 재고를 페이징 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> listInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Long categoryId) {
        LOG.info("재고 목록 조회 - page={}, size={}, status={}, location={}", page, size, status, location);
        List<InventoryResponse> items = buildSampleInventory(size);
        return ResponseEntity.ok(ApiResponse.Paginated.of(items, items.size() * 20L, page, size));
    }

    @Operation(summary = "재고 단건 조회 (상품 ID)", description = "상품 ID로 재고를 조회한다. 옵션(variant)별로 분리된다.")
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByProduct(
            @Parameter(description = "상품 ID", required = true)
            @PathVariable Long productId,
            @RequestParam(required = false) Long variantId) {
        LOG.debug("상품별 재고 조회 - productId={}, variantId={}", productId, variantId);
        if (variantId != null) {
            InventoryResponse single = InventoryResponse.of(
                    ID_SEQ.incrementAndGet(), productId, variantId, 100, 5, 10);
            return ResponseEntity.ok(ApiResponse.Success.of(List.of(single)));
        }
        List<InventoryResponse> items = List.of(
                InventoryResponse.of(60001L, productId, 2001L, 50, 2, 5),
                InventoryResponse.of(60002L, productId, 2002L, 30, 1, 5),
                InventoryResponse.of(60003L, productId, 2003L, 0, 0, 5)
        );
        return ResponseEntity.ok(ApiResponse.Success.of(items));
    }

    @Operation(summary = "재고 수량 직접 설정 (관리자)", description = "특정 상품의 재고를 직접 설정한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "음수 재고")
    })
    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<InventoryResponse>> setInventory(
            @PathVariable Long productId,
            @RequestParam Long variantId,
            @RequestParam int quantity,
            @RequestParam(required = false) String reason) {
        LOG.info("재고 직접 설정 - productId={}, variantId={}, qty={}, reason={}",
                productId, variantId, quantity, reason);
        if (quantity < 0) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.Failure.of("INVALID_QUANTITY", "재고 수량은 0 이상이어야 합니다."));
        }
        InventoryResponse data = InventoryResponse.of(
                ID_SEQ.incrementAndGet(), productId, variantId, quantity, 0, 10);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "재고가 수정되었습니다"));
    }

    @Operation(summary = "재고 조정 (입출고)", description = "입출고/손실/조정 등의 사유로 재고를 가감한다.")
    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustInventory(
            @Valid @RequestBody Map<String, Object> request) {
        LOG.info("재고 조정 요청 - request={}", request);

        Long productId = Long.parseLong(request.get("productId").toString());
        Long variantId = request.containsKey("variantId")
                ? Long.parseLong(request.get("variantId").toString()) : null;
        int delta = Integer.parseInt(request.get("quantityDelta").toString());
        String type = (String) request.getOrDefault("type", "ADJUSTMENT");
        String reason = (String) request.getOrDefault("reason", "");

        // Java 25 패턴 매칭으로 조정 유형 검증
        String validatedType = switch (type) {
            case "INBOUND", "RECEIVED" -> "INBOUND";
            case "OUTBOUND", "SHIPPED" -> "OUTBOUND";
            case "LOSS", "DAMAGE" -> "LOSS";
            case "ADJUSTMENT", "CORRECTION" -> "ADJUSTMENT";
            case null -> "ADJUSTMENT";
            default -> type;
        };
        LOG.info("재고 조정 - type={}, delta={}, reason={}", validatedType, delta, reason);

        List<InventoryResponse.AdjustmentLog> logs = List.of(
                new InventoryResponse.AdjustmentLog(
                        ID_SEQ.incrementAndGet(), validatedType, delta, reason,
                        "admin", LocalDateTime.now())
        );
        int newTotal = Math.max(0, 100 + delta);
        InventoryResponse data = new InventoryResponse(
                ID_SEQ.incrementAndGet(), productId, variantId, "SKU-" + productId,
                newTotal, 0, newTotal, 10,
                InventoryResponse.computeStatus(newTotal, 0, 10),
                newTotal <= 10, "WH-SEOUL-A-12",
                LocalDateTime.now(), null, logs, LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.Success.of(data, "재고 조정 완료"));
    }

    @Operation(summary = "재고 부족 상품", description = "안전 재고 미만의 상품 목록을 조회한다.")
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> lowStockProducts(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "10") int safetyThreshold) {
        LOG.debug("재고 부족 상품 조회 - limit={}, safety={}", limit, safetyThreshold);
        List<InventoryResponse> items = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, 15); i++) {
            items.add(InventoryResponse.of(
                    (long) (60000 + i), (long) (10000 + i), 2000L + i,
                    Math.max(1, safetyThreshold - i), 0, safetyThreshold));
        }
        return ResponseEntity.ok(ApiResponse.Success.of(items,
                "재고 부족 " + items.size() + "개"));
    }

    @Operation(summary = "재고 부족 알림", description = "재고 부족 발생 시 발송된 알림 목록.")
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LOG.debug("재고 부족 알림 조회 - page={}", page);
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            alerts.add(Map.of(
                    "id", (long) (i + 1),
                    "productId", (long) (10000 + i),
                    "productName", "샘플 상품 " + i,
                    "currentStock", Math.max(0, 8 - i),
                    "safetyStock", 10,
                    "alertedAt", LocalDateTime.now().minusHours(i).toString(),
                    "resolved", i > 10
            ));
        }
        return ResponseEntity.ok(ApiResponse.Success.of(alerts));
    }

    @Operation(summary = "재고 동기화 (ERP)", description = "외부 ERP/WMS와 재고를 동기화한다.")
    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncInventory(
            @RequestParam(defaultValue = "FULL") String syncType,
            @RequestParam(required = false) String externalSystem) {
        LOG.info("재고 동기화 요청 - type={}, system={}", syncType, externalSystem);
        Map<String, Object> result = Map.of(
                "syncId", "sync-" + ID_SEQ.incrementAndGet(),
                "syncType", syncType,
                "externalSystem", externalSystem == null ? "DEFAULT_ERP" : externalSystem,
                "syncedProducts", 1234L,
                "updatedRecords", 187L,
                "errors", 0L,
                "startedAt", LocalDateTime.now().toString(),
                "completedAt", LocalDateTime.now().plusMinutes(2).toString(),
                "status", "COMPLETED"
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.Success.of(result, "재고 동기화가 시작되었습니다"));
    }

    @Operation(summary = "재고 변동 이력", description = "특정 상품의 재고 변동 이력을 조회한다.")
    @GetMapping("/history/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER', 'WAREHOUSE')")
    public ResponseEntity<ApiResponse<List<InventoryResponse.AdjustmentLog>>> getHistory(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "100") int limit) {
        LOG.debug("재고 변동 이력 - productId={}, days={}, limit={}", productId, days, limit);
        List<InventoryResponse.AdjustmentLog> logs = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, 20); i++) {
            logs.add(new InventoryResponse.AdjustmentLog(
                    (long) (i + 1),
                    i % 2 == 0 ? "INBOUND" : "OUTBOUND",
                    i % 2 == 0 ? 10 : -3,
                    i % 2 == 0 ? "입고" : "주문 출고",
                    i % 3 == 0 ? "admin" : "system",
                    LocalDateTime.now().minusDays(i % days)));
        }
        return ResponseEntity.ok(ApiResponse.Success.of(logs));
    }

    @Operation(summary = "재고 예약", description = "주문 진행 중 재고를 일시 예약한다.")
    @PostMapping("/reserve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reserveInventory(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam int quantity,
            @RequestParam(defaultValue = "15") int reserveMinutes) {
        LOG.info("재고 예약 - productId={}, variantId={}, qty={}, minutes={}",
                productId, variantId, quantity, reserveMinutes);
        Map<String, Object> result = Map.of(
                "reservationId", "rsv-" + ID_SEQ.incrementAndGet(),
                "productId", productId,
                "variantId", variantId == null ? 0L : variantId,
                "quantity", quantity,
                "reservedUntil", LocalDateTime.now().plusMinutes(reserveMinutes).toString()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(result, "재고가 예약되었습니다"));
    }

    // ---------------------------------------
    // 헬퍼
    // ---------------------------------------

    private List<InventoryResponse> buildSampleInventory(int size) {
        List<InventoryResponse> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(InventoryResponse.of(
                    (long) (60000 + i), (long) (10000 + i), 2000L + i,
                    100 + i * 10, i, 10));
        }
        return items;
    }
}
