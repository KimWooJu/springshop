package com.springshop.web.controller;

import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.NotificationResponse;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 알림 API 컨트롤러.
 *
 * <p>인앱 알림 조회/읽음 처리, 알림 삭제, 미읽음 카운트, 관리자 알림 발송/브로드캐스트
 * 등 알림 도메인 전반의 엔드포인트를 제공한다.
 *
 * <p>알림 타입: ORDER/PAYMENT/SHIPPING/REVIEW/COUPON/MARKETING/SYSTEM/REPLY/STOCK.
 * 채널: IN_APP/PUSH/EMAIL/SMS.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "알림 API", description = "사용자 알림 관리")
public class NotificationController {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(120000);
    private static final Long DEFAULT_USER_ID = 1024L;

    @Operation(summary = "내 알림 목록", description = "로그인 사용자의 알림 목록을 페이징 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        LOG.info("내 알림 목록 - page={}, type={}, unreadOnly={}", page, type, unreadOnly);
        List<NotificationResponse> items = buildSampleNotifications(size, type, unreadOnly);
        return ResponseEntity.ok(ApiResponse.Paginated.of(items, 234L, page, size));
    }

    @Operation(summary = "알림 단건 조회", description = "알림 상세를 조회한다. 자동으로 읽음 처리되지 않는다.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @Parameter(description = "알림 ID", required = true) @PathVariable Long id) {
        LOG.debug("알림 단건 조회 - id={}", id);
        NotificationResponse data = NotificationResponse.of(
                id, DEFAULT_USER_ID, "ORDER",
                "주문 결제 완료", "주문번호 ORD-20260520-00012 의 결제가 완료되었습니다.",
                "/orders/12");
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "알림 읽음 처리", description = "단일 알림을 읽음 상태로 변경한다.")
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id) {
        LOG.info("알림 읽음 처리 - id={}", id);
        LocalDateTime now = LocalDateTime.now();
        NotificationResponse data = new NotificationResponse(
                id, DEFAULT_USER_ID, "ORDER", "TRANSACTIONAL",
                "주문 결제 완료", "결제가 완료되었습니다.",
                null, "/orders/12", true, false,
                "IN_APP", 12L, "ORDER", Map.of(),
                now, now.minusHours(1), null);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "읽음 처리됨"));
    }

    @Operation(summary = "전체 알림 읽음 처리", description = "내 모든 미읽음 알림을 일괄 읽음 처리한다.")
    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead(
            @RequestParam(required = false) String type) {
        LOG.info("전체 알림 읽음 처리 - type={}", type);
        Map<String, Object> result = Map.of(
                "userId", DEFAULT_USER_ID,
                "type", type == null ? "ALL" : type,
                "markedReadCount", 23,
                "processedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result, "전체 읽음 처리 완료"));
    }

    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제한다 (soft delete).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long id) {
        LOG.warn("알림 삭제 - id={}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.Success.empty());
    }

    @Operation(summary = "미읽음 알림 수", description = "사용자의 읽지 않은 알림 개수를 조회한다 (배지용).")
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount() {
        LOG.debug("미읽음 알림 수 조회");
        Map<String, Object> result = Map.of(
                "userId", DEFAULT_USER_ID,
                "totalUnread", 7,
                "byType", Map.of(
                        "ORDER", 2,
                        "PAYMENT", 1,
                        "SHIPPING", 3,
                        "COUPON", 1,
                        "MARKETING", 0
                ),
                "lastCheckedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result));
    }

    @Operation(summary = "관리자 알림 발송", description = "관리자가 특정 사용자에게 알림을 발송한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "발송 성공")
    })
    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody Map<String, Object> request) {
        LOG.info("관리자 알림 발송 - request={}", request.keySet());

        Long userId = Long.parseLong(request.get("userId").toString());
        String type = (String) request.getOrDefault("type", "SYSTEM");
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        String channel = (String) request.getOrDefault("channel", "IN_APP");

        // Java 25 패턴 매칭: 채널 별 우선순위 결정
        boolean important = switch (channel) {
            case "PUSH", "SMS" -> true;
            case "EMAIL" -> false;
            case "IN_APP" -> false;
            case null -> false;
            default -> false;
        };

        Long newId = ID_SEQ.incrementAndGet();
        NotificationResponse data = new NotificationResponse(
                newId, userId, type, "TRANSACTIONAL", title, content,
                (String) request.get("thumbnailUrl"),
                (String) request.get("deepLink"),
                false, important, channel,
                request.containsKey("resourceId")
                        ? Long.parseLong(request.get("resourceId").toString()) : null,
                (String) request.get("resourceType"),
                Map.of(), null, LocalDateTime.now(),
                LocalDateTime.now().plusDays(30));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "알림이 발송되었습니다"));
    }

    @Operation(summary = "관리자 알림 브로드캐스트",
            description = "관리자가 전체 사용자 또는 특정 세그먼트에게 알림을 일괄 발송한다.")
    @PostMapping("/admin/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcast(
            @Valid @RequestBody Map<String, Object> request) {
        LOG.info("알림 브로드캐스트 - request={}", request.keySet());

        String segment = (String) request.getOrDefault("segment", "ALL");
        String channel = (String) request.getOrDefault("channel", "IN_APP");

        // Java 25 패턴 매칭으로 세그먼트별 대상 추정
        long estimatedTargets = switch (segment) {
            case "ALL" -> 100_000L;
            case "ACTIVE" -> 45_000L;
            case "VIP" -> 1_200L;
            case "NEW" -> 5_300L;
            case "DORMANT" -> 12_000L;
            case null -> 0L;
            default -> 100L;
        };

        Map<String, Object> result = Map.of(
                "broadcastId", "bcast-" + ID_SEQ.incrementAndGet(),
                "segment", segment,
                "channel", channel,
                "estimatedTargets", estimatedTargets,
                "title", request.getOrDefault("title", ""),
                "scheduledAt", request.getOrDefault("scheduledAt",
                        LocalDateTime.now().toString()),
                "status", "QUEUED"
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.Success.of(result, "브로드캐스트가 대기열에 등록되었습니다"));
    }

    @Operation(summary = "알림 설정 조회", description = "사용자의 알림 채널/타입별 활성 여부를 조회한다.")
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        LOG.debug("알림 설정 조회");
        Map<String, Object> settings = Map.of(
                "userId", DEFAULT_USER_ID,
                "channelSettings", Map.of(
                        "IN_APP", true,
                        "PUSH", true,
                        "EMAIL", true,
                        "SMS", false
                ),
                "typeSettings", Map.of(
                        "ORDER", true,
                        "PAYMENT", true,
                        "SHIPPING", true,
                        "COUPON", true,
                        "MARKETING", false,
                        "SYSTEM", true
                ),
                "quietHours", Map.of(
                        "enabled", true,
                        "startTime", "22:00",
                        "endTime", "08:00"
                )
        );
        return ResponseEntity.ok(ApiResponse.Success.of(settings));
    }

    @Operation(summary = "알림 설정 변경", description = "사용자의 알림 설정을 부분 변경한다.")
    @PutMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(
            @Valid @RequestBody Map<String, Object> request) {
        LOG.info("알림 설정 변경 - request={}", request.keySet());
        Map<String, Object> result = Map.of(
                "userId", DEFAULT_USER_ID,
                "updatedFields", request.keySet(),
                "updatedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result, "설정이 변경되었습니다"));
    }

    // ---------------------------------------
    // 헬퍼
    // ---------------------------------------

    private List<NotificationResponse> buildSampleNotifications(int size, String filterType,
                                                                boolean unreadOnly) {
        List<NotificationResponse> items = new ArrayList<>(size);
        String[] types = {"ORDER", "PAYMENT", "SHIPPING", "COUPON", "MARKETING", "SYSTEM"};
        for (int i = 0; i < size; i++) {
            String type = filterType != null ? filterType : types[i % types.length];
            boolean read = !unreadOnly && (i % 3 == 0);
            LocalDateTime created = LocalDateTime.now().minusHours(i);
            items.add(new NotificationResponse(
                    (long) (120000 + i), DEFAULT_USER_ID, type, "TRANSACTIONAL",
                    type + " 알림 #" + i, "알림 내용 " + i,
                    "https://cdn.example.com/notif-icon-" + (i % 5) + ".png",
                    "/notifications/" + (120000 + i),
                    read, i % 7 == 0, "IN_APP",
                    (long) (50000 + i), "ORDER", Map.of(),
                    read ? created.plusMinutes(5) : null,
                    created, created.plusDays(30)));
        }
        return items;
    }
}
