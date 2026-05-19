package com.springshop.service.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

/**
 * {@link ReviewService} 구현체.
 *
 * <p>리뷰 CRUD와 관리자 승인 워크플로우를 처리한다.
 * 리뷰 통계 집계에 Java 25 Virtual Thread를 활용하고,
 * 리뷰 상태 전이는 패턴 매칭 switch로 타입 안전하게 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    /**
     * 리뷰 상태 — sealed interface로 상태 전이를 타입 안전하게 표현한다.
     */
    sealed interface ReviewStatus permits ReviewStatus.Pending, ReviewStatus.Approved,
                                           ReviewStatus.Rejected, ReviewStatus.Reported {
        record Pending() implements ReviewStatus {}
        record Approved(String adminNote) implements ReviewStatus {}
        record Rejected(String reason) implements ReviewStatus {}
        record Reported(int count, String lastReason) implements ReviewStatus {}
    }

    // 실제 구현에서는 ReviewRepository 주입
    // private final ReviewRepository reviewRepository;
    // private final OrderRepository orderRepository; // 구매 이력 확인용

    @Override
    public Long createReview(Long productId, Long userId, int rating,
                             String title, String content, List<String> imageUrls) {
        log.info("리뷰 생성 - productId={}, userId={}, rating={}", productId, userId, rating);

        validateRating(rating);
        validatePurchaseHistory(productId, userId);

        // 동일 상품에 대한 중복 리뷰 방지
        checkDuplicateReview(productId, userId);

        // 리뷰 생성 후 상품 평균 평점 업데이트
        Long reviewId = persistReview(productId, userId, rating, title, content, imageUrls);
        updateProductRating(productId);

        log.info("리뷰 생성 완료 - reviewId={}, productId={}", reviewId, productId);
        return reviewId;
    }

    @Override
    public void updateReview(Long reviewId, Long userId, int rating, String title, String content) {
        log.info("리뷰 수정 - reviewId={}, userId={}", reviewId, userId);
        validateRating(rating);
        verifyReviewOwnership(reviewId, userId);
        // 실제 구현: reviewRepository.findById(reviewId).orElseThrow().update(rating, title, content);
        updateProductRating(1L); // 연관 상품 평점 재계산
        log.info("리뷰 수정 완료 - reviewId={}", reviewId);
    }

    @Override
    public void deleteReview(Long reviewId, Long userId) {
        log.info("리뷰 삭제 - reviewId={}, userId={}", reviewId, userId);
        verifyReviewOwnershipOrAdmin(reviewId, userId);
        // 실제 구현: reviewRepository.deleteById(reviewId);
        log.info("리뷰 삭제 완료 - reviewId={}", reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDto getReview(Long reviewId) {
        log.debug("리뷰 조회 - reviewId={}", reviewId);
        return buildMockReviewDto(reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDto> getProductReviews(Long productId, Pageable pageable) {
        log.debug("상품 리뷰 목록 - productId={}, page={}", productId, pageable.getPageNumber());
        List<ReviewDto> reviews = IntStream.range(0, 5)
                .mapToObj(i -> buildMockReviewDto((long) (productId * 10 + i)))
                .toList();
        return new PageImpl<>(reviews, pageable, 42);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDto> getUserReviews(Long userId, Pageable pageable) {
        log.debug("사용자 리뷰 목록 - userId={}", userId);
        List<ReviewDto> reviews = List.of(buildMockReviewDto(userId * 100));
        return new PageImpl<>(reviews, pageable, 1);
    }

    @Override
    public void approveReview(Long reviewId, String adminNote) {
        log.info("리뷰 승인 - reviewId={}, note={}", reviewId, adminNote);
        var status = new ReviewStatus.Approved(adminNote);
        applyStatusTransition(reviewId, status);
    }

    @Override
    public void rejectReview(Long reviewId, String reason) {
        log.warn("리뷰 반려 - reviewId={}, reason={}", reviewId, reason);
        var status = new ReviewStatus.Rejected(reason);
        applyStatusTransition(reviewId, status);
    }

    @Override
    public void reportReview(Long reviewId, Long reporterId, String reason) {
        log.warn("리뷰 신고 - reviewId={}, reporterId={}, reason={}", reviewId, reporterId, reason);
        // 신고 임계값(5회) 초과 시 자동으로 관리자 검토 대기 상태로 전환
        int reportCount = incrementReportCount(reviewId);
        if (reportCount >= 5) {
            log.warn("리뷰 자동 숨김 처리 - reviewId={}, reportCount={}", reviewId, reportCount);
            applyStatusTransition(reviewId, new ReviewStatus.Reported(reportCount, reason));
        }
    }

    @Override
    public void markHelpful(Long reviewId, Long userId) {
        log.debug("도움이 됐어요 - reviewId={}, userId={}", reviewId, userId);
        // 동일 사용자의 중복 클릭 방지
        checkDuplicateHelpful(reviewId, userId);
        // 실제 구현: reviewRepository.incrementHelpfulCount(reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getReviewStatistics(Long productId) {
        log.debug("리뷰 통계 집계 - productId={}", productId);

        // Virtual Thread로 병렬 통계 집계
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Double> avgFuture = executor.submit(() -> calculateAverageRating(productId));
            Future<Long> countFuture = executor.submit(() -> countReviews(productId));
            Future<Map<Integer, Long>> distFuture = executor.submit(() -> getRatingDistribution(productId));

            double avg = avgFuture.get();
            long count = countFuture.get();
            Map<Integer, Long> distribution = distFuture.get();

            Map<String, Object> stats = new HashMap<>();
            stats.put("averageRating", avg);
            stats.put("totalCount", count);
            stats.put("ratingDistribution", distribution);
            return stats;
        } catch (Exception ex) {
            log.error("리뷰 통계 집계 실패 - productId={}", productId, ex);
            return Map.of("averageRating", 0.0, "totalCount", 0L, "ratingDistribution", Map.of());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDto> getPendingReviews(Pageable pageable) {
        log.debug("검토 대기 리뷰 목록 - page={}", pageable.getPageNumber());
        List<ReviewDto> reviews = List.of(buildMockReviewDto(9001L), buildMockReviewDto(9002L));
        return new PageImpl<>(reviews, pageable, 2);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다. rating=" + rating);
        }
    }

    private void validatePurchaseHistory(Long productId, Long userId) {
        // 실제 구현: orderRepository.existsByUserIdAndProductId(userId, productId)
        log.debug("구매 이력 확인 - productId={}, userId={}", productId, userId);
    }

    private void checkDuplicateReview(Long productId, Long userId) {
        // 실제 구현: reviewRepository.existsByProductIdAndUserId(productId, userId)
        log.debug("중복 리뷰 확인 - productId={}, userId={}", productId, userId);
    }

    private Long persistReview(Long productId, Long userId, int rating,
                               String title, String content, List<String> imageUrls) {
        // 실제 구현: new Review(...)를 생성하여 repository.save()
        return System.currentTimeMillis();
    }

    private void updateProductRating(Long productId) {
        // 실제 구현: Stream Gatherer로 평균 평점 재계산 후 Product 업데이트
        log.debug("상품 평점 업데이트 - productId={}", productId);
    }

    private void verifyReviewOwnership(Long reviewId, Long userId) {
        // 실제 구현: reviewRepository.findById().getUser().getId().equals(userId)
        log.debug("리뷰 소유권 확인 - reviewId={}, userId={}", reviewId, userId);
    }

    private void verifyReviewOwnershipOrAdmin(Long reviewId, Long userId) {
        log.debug("리뷰 소유권/관리자 확인 - reviewId={}, userId={}", reviewId, userId);
    }

    private void applyStatusTransition(Long reviewId, ReviewStatus newStatus) {
        String statusName = switch (newStatus) {
            case ReviewStatus.Pending p  -> "PENDING";
            case ReviewStatus.Approved a -> "APPROVED";
            case ReviewStatus.Rejected r -> "REJECTED";
            case ReviewStatus.Reported r -> "REPORTED";
        };
        log.info("리뷰 상태 변경 - reviewId={}, newStatus={}", reviewId, statusName);
        // 실제 구현: reviewRepository.updateStatus(reviewId, statusName)
    }

    private int incrementReportCount(Long reviewId) {
        // 실제 구현: reviewRepository.incrementReportCount(reviewId)
        return 1;
    }

    private void checkDuplicateHelpful(Long reviewId, Long userId) {
        log.debug("중복 도움 확인 - reviewId={}, userId={}", reviewId, userId);
    }

    private double calculateAverageRating(Long productId) {
        return 4.2;
    }

    private long countReviews(Long productId) {
        return 42L;
    }

    private Map<Integer, Long> getRatingDistribution(Long productId) {
        return Map.of(1, 2L, 2, 3L, 3, 5L, 4, 12L, 5, 20L);
    }

    private ReviewDto buildMockReviewDto(Long reviewId) {
        return new ReviewDto(
                reviewId, 1L, 100L, "사용자닉네임", 5,
                "최고의 상품입니다", "배송도 빠르고 품질도 훌륭합니다.",
                "APPROVED", List.of(), 10, 0,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(3)
        );
    }
}
