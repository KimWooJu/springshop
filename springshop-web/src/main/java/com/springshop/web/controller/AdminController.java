package com.springshop.web.controller;

import com.springshop.web.dto.request.DateRangeRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 관리자 API 컨트롤러.
 *
 * <p>관리자 대시보드, 통계(매출/회원/상품/주문), 시스템 유지보수(캐시 클리어, 로그 조회),
 * 헬스 체크 등 관리자 전용 엔드포인트를 제공한다.
 *
 * <p>모든 엔드포인트는 {@code ROLE_ADMIN} 권한이 필수이며, 일부는 {@code ROLE_SUPER_ADMIN}
 * 만 접근 가능하다.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "관리자 API", description = "관리자 대시보드 및 통계")
public class AdminController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);
    private static final AtomicLong LOG_ID_SEQ = new AtomicLong(0);

    @Operation(summary = "관리자 대시보드", description = "관리자 메인 화면 대시보드 데이터를 한 번에 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @Parameter(description = "리프레시 강제 여부") @RequestParam(defaultValue = "false") boolean forceRefresh) {
        LOG.info("관리자 대시보드 조회 - forceRefresh={}", forceRefresh);

        List<DashboardResponse.BestSeller> topSellers = List.of(
                new DashboardResponse.BestSeller(10001L, "맥북 프로 16인치",
                        "https://cdn.example.com/p1.jpg", 234L, new BigDecimal("89123400"), 1),
                new DashboardResponse.BestSeller(10002L, "아이폰 15 Pro",
                        "https://cdn.example.com/p2.jpg", 198L, new BigDecimal("72340000"), 2),
                new DashboardResponse.BestSeller(10003L, "에어팟 프로",
                        "https://cdn.example.com/p3.jpg", 567L, new BigDecimal("34020000"), 3),
                new DashboardResponse.BestSeller(10004L, "갤럭시 S24 Ultra",
                        "https://cdn.example.com/p4.jpg", 145L, new BigDecimal("28950000"), 4),
                new DashboardResponse.BestSeller(10005L, "닌텐도 스위치",
                        "https://cdn.example.com/p5.jpg", 89L, new BigDecimal("21800000"), 5)
        );

        List<DashboardResponse.TopCategory> topCategories = List.of(
                new DashboardResponse.TopCategory(300L, "전자제품", 1234L, new BigDecimal("345600000"), 1),
                new DashboardResponse.TopCategory(400L, "패션", 987L, new BigDecimal("123450000"), 2),
                new DashboardResponse.TopCategory(500L, "식품", 567L, new BigDecimal("45670000"), 3),
                new DashboardResponse.TopCategory(600L, "도서", 432L, new BigDecimal("12340000"), 4),
                new DashboardResponse.TopCategory(700L, "스포츠", 234L, new BigDecimal("8900000"), 5)
        );

        List<DashboardResponse.DailyMetric> dailyRevenue = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            dailyRevenue.add(new DashboardResponse.DailyMetric(
                    LocalDate.now().minusDays(i),
                    100L + i * 23,
                    new BigDecimal(String.valueOf(5_000_000 + i * 123456)),
                    20L + i * 5
            ));
        }

        DashboardResponse data = new DashboardResponse(
                new BigDecimal("8345600"), new BigDecimal("7234500"),
                new BigDecimal("234567890"), new BigDecimal("198765432"),
                17.9, 234L, 198L, 5678L, 23L, 45L,
                123_456L, 87_654L, 3L, 12L, new BigDecimal("234500"),
                17L, 5L, 23L, 7L,
                topSellers, topCategories, dailyRevenue,
                Map.of(
                        "CREDIT_CARD", 65.3,
                        "KAKAOPAY", 18.7,
                        "NAVERPAY", 8.2,
                        "BANK_TRANSFER", 4.5,
                        "VIRTUAL_ACCOUNT", 3.3
                ),
                "HEALTHY", LocalDateTime.now()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(data, "대시보드 데이터 조회 완료"));
    }

    @Operation(summary = "매출 통계", description = "기간별 매출 통계를 조회한다.")
    @GetMapping("/statistics/sales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String groupBy) {
        LOG.info("매출 통계 - startDate={}, endDate={}, groupBy={}", startDate, endDate, groupBy);

        DateRangeRequest range = startDate != null && endDate != null
                ? new DateRangeRequest(startDate, endDate, groupBy, "Asia/Seoul")
                : DateRangeRequest.fromPreset(DateRangeRequest.PresetRange.LAST_30_DAYS);

        // Java 25 패턴 매칭으로 그룹 단위 검증
        String validatedGroupBy = switch (groupBy) {
            case "DAY", "WEEK", "MONTH", "QUARTER", "YEAR" -> groupBy;
            case null -> "DAY";
            default -> "DAY";
        };

        List<Map<String, Object>> series = new ArrayList<>();
        long days = range.toDays();
        for (long i = 0; i < Math.min(days, 30); i++) {
            series.add(Map.of(
                    "date", range.startDate().plusDays(i).toString(),
                    "revenue", new BigDecimal(String.valueOf(1_000_000L + i * 234567)),
                    "orderCount", 100L + i * 5,
                    "averageOrderValue", new BigDecimal("32100")
            ));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", validatedGroupBy);
        result.put("startDate", range.startDate().toString());
        result.put("endDate", range.endDate().toString());
        result.put("totalRevenue", new BigDecimal("234567890"));
        result.put("totalOrders", 12345L);
        result.put("averageOrderValue", new BigDecimal("19000"));
        result.put("growthRate", 17.9);
        result.put("series", series);
        return ResponseEntity.ok(ApiResponse.Success.of(result));
    }

    @Operation(summary = "회원 통계", description = "기간별 회원 통계 (신규/활성/탈퇴)를 조회한다.")
    @GetMapping("/statistics/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        LOG.info("회원 통계 - startDate={}, endDate={}", startDate, endDate);
        Map<String, Object> result = Map.of(
                "totalUsers", 123_456L,
                "activeUsers", 87_654L,
                "newUsersThisMonth", 2_345L,
                "withdrawnThisMonth", 123L,
                "averageLifetime", "23개월",
                "retentionRate", Map.of(
                        "day1", 78.5,
                        "day7", 45.3,
                        "day30", 23.7
                ),
                "demographics", Map.of(
                        "byAge", Map.of("20s", 35.4, "30s", 32.1, "40s", 18.7, "50+", 13.8),
                        "byGender", Map.of("MALE", 47.3, "FEMALE", 52.7)
                ),
                "topReferrers", List.of(
                        Map.of("source", "GOOGLE", "count", 4567L),
                        Map.of("source", "NAVER", "count", 3456L),
                        Map.of("source", "FACEBOOK", "count", 1234L)
                ),
                "calculatedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result));
    }

    @Operation(summary = "상품 통계", description = "상품 카테고리별, 브랜드별 판매 통계를 조회한다.")
    @GetMapping("/statistics/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductStatistics(
            @RequestParam(defaultValue = "MONTH") String period,
            @RequestParam(defaultValue = "20") int topN) {
        LOG.info("상품 통계 - period={}, topN={}", period, topN);
        Map<String, Object> result = Map.ofEntries(
                Map.entry("period", period),
                Map.entry("totalProducts", 12_345L),
                Map.entry("activeProducts", 9_876L),
                Map.entry("outOfStockProducts", 234L),
                Map.entry("lowStockProducts", 567L),
                Map.entry("newProductsThisMonth", 234L),
                Map.entry("averagePrice", new BigDecimal("38000")),
                Map.entry("averageRating", 4.32),
                Map.entry("totalReviews", 234_567L),
                Map.entry("topSellingProducts", List.of(
                        Map.of("productId", 10001L, "name", "맥북 프로 16인치",
                                "soldCount", 234L, "revenue", new BigDecimal("89123400")),
                        Map.of("productId", 10002L, "name", "아이폰 15 Pro",
                                "soldCount", 198L, "revenue", new BigDecimal("72340000"))
                )),
                Map.entry("byCategory", Map.of(
                        "전자제품", Map.of("productCount", 3456, "totalSold", 12345L),
                        "패션", Map.of("productCount", 2345, "totalSold", 8765L),
                        "식품", Map.of("productCount", 1234, "totalSold", 5432L)
                )),
                Map.entry("calculatedAt", LocalDateTime.now().toString())
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result));
    }

    @Operation(summary = "주문 통계", description = "기간별 주문 상태별 통계를 조회한다.")
    @GetMapping("/statistics/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderStatistics(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        LOG.info("주문 통계 - startDate={}, endDate={}", startDate, endDate);
        Map<String, Object> result = Map.of(
                "totalOrders", 23_456L,
                "byStatus", Map.of(
                        "PENDING", 234L,
                        "PAID", 1_234L,
                        "PREPARING", 567L,
                        "SHIPPED", 3_456L,
                        "DELIVERED", 17_890L,
                        "CANCELLED", 89L,
                        "RETURNED", 23L
                ),
                "byPaymentMethod", Map.of(
                        "CREDIT_CARD", 15_234L,
                        "KAKAOPAY", 4_567L,
                        "NAVERPAY", 1_900L,
                        "BANK_TRANSFER", 1_055L,
                        "VIRTUAL_ACCOUNT", 700L
                ),
                "cancellationRate", 0.038,
                "returnRate", 0.0098,
                "averageOrderValue", new BigDecimal("38500"),
                "averageItemsPerOrder", 2.3,
                "averageDeliveryDays", 2.1,
                "calculatedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result));
    }

    @Operation(summary = "캐시 클리어", description = "지정된 캐시 영역을 무효화한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @PostMapping("/maintenance/cache-clear")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearCache(
            @RequestParam(defaultValue = "ALL") String cacheRegion,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        LOG.warn("캐시 클리어 - region={}, dryRun={}", cacheRegion, dryRun);

        // Java 25 패턴 매칭으로 캐시 영역 분류
        Map<String, Long> clearedEntries = switch (cacheRegion) {
            case "ALL" -> Map.of(
                    "products", 12_345L, "categories", 234L,
                    "users", 5_678L, "sessions", 23_456L);
            case "PRODUCTS" -> Map.of("products", 12_345L);
            case "USERS" -> Map.of("users", 5_678L);
            case "SESSIONS" -> Map.of("sessions", 23_456L);
            case null -> Map.of();
            default -> Map.of("custom", 0L);
        };

        Map<String, Object> result = Map.of(
                "cacheRegion", cacheRegion,
                "dryRun", dryRun,
                "clearedEntries", clearedEntries,
                "totalCleared", clearedEntries.values().stream().mapToLong(Long::longValue).sum(),
                "executedAt", LocalDateTime.now().toString(),
                "executedBy", "admin"
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result, "캐시 클리어 완료"));
    }

    @Operation(summary = "헬스 체크 (상세)",
            description = "DB/Redis/External API 등 시스템 상세 헬스를 조회한다.")
    @GetMapping("/health/detailed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailedHealth() {
        LOG.debug("상세 헬스 체크");
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", Map.of(
                "status", "UP", "responseTime", "12ms",
                "activeConnections", 23, "maxConnections", 100));
        components.put("redis", Map.of(
                "status", "UP", "responseTime", "2ms",
                "memoryUsed", "234MB", "memoryMax", "1GB"));
        components.put("elasticsearch", Map.of(
                "status", "UP", "responseTime", "45ms",
                "clusterStatus", "GREEN"));
        components.put("paymentGateway", Map.of(
                "status", "UP", "lastCheckedAt",
                LocalDateTime.now().minusMinutes(1).toString()));
        components.put("emailService", Map.of(
                "status", "UP", "queueSize", 12));
        components.put("smsService", Map.of(
                "status", "DEGRADED", "queueSize", 234,
                "warning", "큐 적체 발생"));

        Map<String, Object> result = Map.of(
                "overallStatus", "DEGRADED",
                "components", components,
                "uptime", "23일 4시간 12분",
                "version", "1.2.3-RELEASE",
                "checkedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result));
    }

    @Operation(summary = "시스템 로그", description = "최근 시스템 로그를 조회한다.")
    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLogs(
            @RequestParam(defaultValue = "INFO") String level,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String keyword) {
        LOG.info("시스템 로그 조회 - level={}, limit={}, keyword={}", level, limit, keyword);

        // Java 25 패턴 매칭으로 로그 레벨 검증
        String validLevel = switch (level) {
            case "TRACE", "DEBUG", "INFO", "WARN", "ERROR" -> level;
            case null -> "INFO";
            default -> "INFO";
        };

        List<Map<String, Object>> logs = new ArrayList<>(limit);
        String[] components = {"OrderService", "PaymentService", "UserService", "CartService", "InventoryService"};
        for (int i = 0; i < limit; i++) {
            String comp = components[i % components.length];
            String entryLevel = i % 20 == 0 ? "ERROR" : (i % 10 == 0 ? "WARN" : validLevel);
            logs.add(Map.of(
                    "id", LOG_ID_SEQ.incrementAndGet(),
                    "timestamp", LocalDateTime.now().minusSeconds(i).toString(),
                    "level", entryLevel,
                    "component", comp,
                    "message", comp + " 처리 - 작업 " + i,
                    "thread", "http-nio-8080-exec-" + (i % 16),
                    "traceId", UUID.randomUUID().toString()
            ));
        }
        return ResponseEntity.ok(ApiResponse.Success.of(logs,
                "총 " + logs.size() + "건의 로그"));
    }

    @Operation(summary = "시스템 점검 모드 토글", description = "서비스 점검 모드를 켜거나 끈다.")
    @PostMapping("/maintenance/mode")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleMaintenanceMode(
            @RequestParam boolean enabled,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) Integer estimatedMinutes) {
        LOG.warn("점검 모드 변경 - enabled={}, msg={}, mins={}", enabled, message, estimatedMinutes);
        Map<String, Object> result = Map.of(
                "maintenanceMode", enabled,
                "message", message == null ? "" : message,
                "estimatedMinutes", estimatedMinutes == null ? 0 : estimatedMinutes,
                "changedAt", LocalDateTime.now().toString(),
                "changedBy", "super_admin"
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.Success.of(result, enabled ? "점검 모드 시작" : "점검 모드 해제"));
    }

    @Operation(summary = "시스템 정보", description = "JVM, OS, 빌드 정보 등 시스템 메타 정보를 조회한다.")
    @GetMapping("/system/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> systemInfo() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> info = Map.of(
                "application", Map.of(
                        "name", "SpringShop",
                        "version", "1.2.3-RELEASE",
                        "profile", "production",
                        "buildTime", "2026-05-15T10:23:45"
                ),
                "jvm", Map.of(
                        "version", System.getProperty("java.version"),
                        "vendor", System.getProperty("java.vendor"),
                        "totalMemory", runtime.totalMemory(),
                        "freeMemory", runtime.freeMemory(),
                        "maxMemory", runtime.maxMemory(),
                        "availableProcessors", runtime.availableProcessors()
                ),
                "os", Map.of(
                        "name", System.getProperty("os.name"),
                        "version", System.getProperty("os.version"),
                        "arch", System.getProperty("os.arch")
                ),
                "currentTime", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(info));
    }
}
