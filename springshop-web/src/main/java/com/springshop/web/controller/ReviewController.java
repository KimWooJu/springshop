package com.springshop.web.controller;

import com.springshop.web.dto.request.CreateReviewRequest;
import com.springshop.web.dto.request.UpdateReviewRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.ReviewResponse;
import com.springshop.web.dto.response.ReviewSummaryResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 리뷰 API 컨트롤러.
 *
 * <p>상품 리뷰 작성/수정/삭제/조회, 도움됨/신고 처리, 관리자 검수(승인/거절),
 * 리뷰 통계(평점 분포) 등 리뷰 도메인의 모든 엔드포인트를 제공한다.
 *
 * <p>구매 인증 리뷰만 정식 노출되며, 미인증 리뷰는 검수 대기 상태가 된다.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "리뷰 API", description = "상품 리뷰 CRUD 및 검수")
public class ReviewController {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(40000);
    private static final Long DEFAULT_USER_ID = 1024L;

    @Operation(summary = "상품별 리뷰 목록", description = "특정 상품의 리뷰를 페이징 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getProductReviews(
            @Parameter(description = "상품 ID", required = true) @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "RECENT") String sortBy,
            @RequestParam(required = false) Integer ratingFilter,
            @RequestParam(defaultValue = "false") boolean onlyWithImages) {
        LOG.info("상품별 리뷰 목록 - productId={}, sortBy={}, filter={}, onlyImages={}",
                productId, sortBy, ratingFilter, onlyWithImages);
        List<ReviewResponse> items = buildSampleReviews(size, productId);
        return ResponseEntity.ok(ApiResponse.Paginated.of(items, items.size() * 50L, page, size));
    }

    @Operation(summary = "리뷰 작성", description = "주문한 상품에 대해 리뷰를 작성한다 (구매 인증 필요).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 작성됨")
    })
    @PostMapping("/products/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody CreateReviewRequest request) {
        LOG.info("리뷰 작성 - productId={}, rating={}, hasMedia={}",
                productId, request.rating(), request.hasMedia());
        if (!productId.equals(request.productId())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.Failure.of("PRODUCT_MISMATCH", "URL 상품 ID와 요청 상품 ID가 다릅니다."));
        }
        Long newId = ID_SEQ.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        ReviewResponse data = new ReviewResponse(
                newId, productId, request.orderId(), DEFAULT_USER_ID, "홍***길",
                null, request.rating(), request.title(), request.content(),
                request.imageUrls(), "색상: 블랙",
                true, request.isDetailedReview() ? "APPROVED" : "PENDING",
                0, 0, 0, null, true, now, now);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "리뷰가 작성되었습니다"));
    }

    @Operation(summary = "리뷰 단건 조회", description = "리뷰 ID로 단건을 조회한다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(
            @PathVariable Long id) {
        LOG.debug("리뷰 단건 조회 - id={}", id);
        ReviewResponse data = ReviewResponse.of(
                id, 10001L, DEFAULT_USER_ID, "홍***길",
                5, "정말 좋아요", "품질이 매우 만족스럽습니다. 재구매 의사 있어요.", true);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "리뷰 수정", description = "본인 작성 리뷰를 수정한다.")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        LOG.info("리뷰 수정 - id={}, hasChange={}", id, request.hasAnyChange());
        if (!request.hasAnyChange()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.Failure.of("NO_CHANGE", "변경된 필드가 없습니다."));
        }
        LocalDateTime now = LocalDateTime.now();
        ReviewResponse data = new ReviewResponse(
                id, 10001L, 50001L, DEFAULT_USER_ID, "홍***길", null,
                request.rating() == null ? 5 : request.rating(),
                request.title() == null ? "기본 제목" : request.title(),
                request.content() == null ? "본문" : request.content(),
                request.imageUrls() == null ? List.of() : request.imageUrls(),
                null, true, "APPROVED", 0, 0, 0, null, true,
                now.minusDays(1), now);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "리뷰가 수정되었습니다"));
    }

    @Operation(summary = "리뷰 삭제", description = "본인 작성 리뷰를 삭제한다 (soft delete).")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long id) {
        LOG.warn("리뷰 삭제 - id={}", id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.Success.empty());
    }

    @Operation(summary = "리뷰 도움됨", description = "리뷰에 도움됨/도움안됨을 표시한다.")
    @PostMapping("/{id}/helpful")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markHelpful(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean helpful) {
        LOG.info("리뷰 도움됨 표시 - id={}, helpful={}", id, helpful);
        Map<String, Object> result = Map.of(
                "reviewId", id,
                "helpful", helpful,
                "newHelpfulCount", helpful ? 45 : 44,
                "newNotHelpfulCount", helpful ? 2 : 3,
                "votedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(result, helpful ? "도움됨" : "도움안됨"));
    }

    @Operation(summary = "리뷰 신고", description = "부적절한 리뷰를 신고한다.")
    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportReview(
            @PathVariable Long id,
            @RequestParam String reason,
            @RequestParam(required = false) String detail) {
        LOG.warn("리뷰 신고 - id={}, reason={}", id, reason);

        // Java 25 패턴 매칭으로 신고 사유 검증
        String validatedReason = switch (reason) {
            case "SPAM", "ADVERTISEMENT" -> "SPAM";
            case "INAPPROPRIATE", "OFFENSIVE" -> "INAPPROPRIATE";
            case "FAKE", "MISLEADING" -> "FAKE";
            case "PERSONAL_INFO" -> "PERSONAL_INFO";
            case null -> "OTHER";
            default -> "OTHER";
        };

        Map<String, Object> result = Map.of(
                "reviewId", id,
                "reportId", ID_SEQ.incrementAndGet(),
                "reason", validatedReason,
                "detail", detail == null ? "" : detail,
                "status", "PENDING_REVIEW",
                "reportedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(result, "신고가 접수되었습니다"));
    }

    @Operation(summary = "검수 대기 리뷰 (관리자)", description = "관리자가 검수 대기 중인 리뷰 목록을 조회한다.")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "false") boolean reportedOnly) {
        LOG.info("검수 대기 리뷰 - page={}, status={}, reported={}", page, status, reportedOnly);
        List<ReviewResponse> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(new ReviewResponse(
                    (long) (40000 + i), (long) (10000 + i), 50000L + i,
                    1024L + i, "사용자" + i, null,
                    (i % 5) + 1, "검수 대기 리뷰", "검수 대기 중인 리뷰 내용입니다.",
                    List.of(), null, true, status, 0, 0,
                    reportedOnly ? 3 : 0, null, false,
                    LocalDateTime.now().minusHours(i), LocalDateTime.now().minusHours(i)));
        }
        return ResponseEntity.ok(ApiResponse.Paginated.of(items, 234L, page, size));
    }

    @Operation(summary = "리뷰 승인 (관리자)", description = "검수 대기 리뷰를 승인 처리한다.")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(
            @PathVariable Long id,
            @RequestParam(required = false) String approverNote) {
        LOG.info("리뷰 승인 - id={}, note={}", id, approverNote);
        ReviewResponse data = new ReviewResponse(
                id, 10001L, 50001L, 1024L, "홍***길", null,
                5, "승인된 리뷰", "리뷰 본문", List.of(), null,
                true, "APPROVED", 0, 0, 0, null, false,
                LocalDateTime.now().minusDays(1), LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.Success.of(data, "리뷰가 승인되었습니다"));
    }

    @Operation(summary = "리뷰 거절 (관리자)", description = "검수 대기 리뷰를 거절 처리한다.")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(
            @PathVariable Long id,
            @RequestParam String rejectReason) {
        LOG.warn("리뷰 거절 - id={}, reason={}", id, rejectReason);
        ReviewResponse data = new ReviewResponse(
                id, 10001L, 50001L, 1024L, "홍***길", null,
                1, "거절된 리뷰", "거절된 리뷰 본문", List.of(), null,
                true, "REJECTED", 0, 0, 0,
                "관리자: " + rejectReason, false,
                LocalDateTime.now().minusDays(1), LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.Success.of(data, "리뷰가 거절되었습니다"));
    }

    @Operation(summary = "리뷰 통계", description = "상품의 평점 분포 통계를 조회한다.")
    @GetMapping("/products/{productId}/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewSummary(
            @PathVariable Long productId) {
        LOG.debug("리뷰 통계 - productId={}", productId);
        Map<Integer, Long> distribution = new TreeMap<>();
        distribution.put(1, 12L);
        distribution.put(2, 25L);
        distribution.put(3, 89L);
        distribution.put(4, 347L);
        distribution.put(5, 810L);
        ReviewSummaryResponse data = ReviewSummaryResponse.from(productId, distribution);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "관리자 답변 등록", description = "관리자가 리뷰에 공식 답변을 작성한다.")
    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> replyToReview(
            @PathVariable Long id,
            @RequestParam String reply) {
        LOG.info("관리자 답변 등록 - id={}, replyLen={}", id, reply.length());
        ReviewResponse original = ReviewResponse.of(
                id, 10001L, 1024L, "홍***길", 5,
                "정말 좋아요", "리뷰 본문", true);
        ReviewResponse data = original.withAdminReply(reply);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "답변이 등록되었습니다"));
    }

    // ---------------------------------------
    // 헬퍼
    // ---------------------------------------

    private List<ReviewResponse> buildSampleReviews(int size, Long productId) {
        List<ReviewResponse> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(new ReviewResponse(
                    (long) (40000 + i), productId, 50000L + i,
                    1024L + i, mask("사용자" + i), null,
                    Math.max(1, 5 - (i % 5)),
                    "리뷰 제목 " + i, "리뷰 본문이 길게 작성되었습니다. 만족합니다.",
                    i % 3 == 0 ? List.of("https://cdn.example.com/r" + i + ".jpg") : List.of(),
                    "색상: 블랙 / 사이즈: L",
                    true, "APPROVED", 10 + i, i / 2, 0,
                    i % 7 == 0 ? "감사합니다!" : null, false,
                    LocalDateTime.now().minusDays(i),
                    LocalDateTime.now().minusDays(i)));
        }
        return items;
    }

    private String mask(String name) {
        if (name == null || name.length() < 3) return name;
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }
}
