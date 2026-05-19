package com.springshop.service.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 상품 리뷰 서비스 인터페이스.
 *
 * <p>리뷰 작성·수정·삭제, 관리자 승인/반려, 신고 처리,
 * 리뷰 통계 집계 등 리뷰 라이프사이클 전반을 정의한다.
 */
public interface ReviewService {

    /**
     * 리뷰를 생성한다. 구매 이력이 없는 경우 예외를 던진다.
     *
     * @param productId  대상 상품 ID
     * @param userId     리뷰 작성자 ID
     * @param rating     평점 (1~5)
     * @param title      리뷰 제목
     * @param content    리뷰 본문
     * @param imageUrls  첨부 이미지 URL 목록
     * @return           생성된 리뷰 ID
     */
    Long createReview(Long productId, Long userId, int rating,
                      String title, String content, List<String> imageUrls);

    /**
     * 리뷰를 수정한다. 작성자만 수정 가능하다.
     *
     * @param reviewId 수정할 리뷰 ID
     * @param userId   수정 요청자 ID
     * @param rating   변경할 평점
     * @param title    변경할 제목
     * @param content  변경할 내용
     */
    void updateReview(Long reviewId, Long userId, int rating, String title, String content);

    /**
     * 리뷰를 삭제한다. 작성자 또는 관리자만 삭제 가능하다.
     *
     * @param reviewId 삭제할 리뷰 ID
     * @param userId   삭제 요청자 ID
     */
    void deleteReview(Long reviewId, Long userId);

    /**
     * 리뷰 단건을 조회한다.
     *
     * @param reviewId 조회할 리뷰 ID
     * @return ReviewDto 리뷰 데이터
     */
    ReviewDto getReview(Long reviewId);

    /**
     * 특정 상품의 승인된 리뷰 목록을 페이징 조회한다.
     *
     * @param productId 대상 상품 ID
     * @param pageable  페이징 정보
     * @return 리뷰 페이지
     */
    Page<ReviewDto> getProductReviews(Long productId, Pageable pageable);

    /**
     * 특정 사용자가 작성한 리뷰 목록을 조회한다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 리뷰 페이지
     */
    Page<ReviewDto> getUserReviews(Long userId, Pageable pageable);

    /**
     * 관리자가 리뷰를 승인 처리한다.
     *
     * @param reviewId  승인할 리뷰 ID
     * @param adminNote 승인 메모
     */
    void approveReview(Long reviewId, String adminNote);

    /**
     * 관리자가 리뷰를 반려 처리한다.
     *
     * @param reviewId 반려할 리뷰 ID
     * @param reason   반려 사유
     */
    void rejectReview(Long reviewId, String reason);

    /**
     * 리뷰를 신고한다.
     *
     * @param reviewId   신고할 리뷰 ID
     * @param reporterId 신고자 ID
     * @param reason     신고 사유
     */
    void reportReview(Long reviewId, Long reporterId, String reason);

    /**
     * 리뷰에 도움이 됐다고 표시한다.
     *
     * @param reviewId 리뷰 ID
     * @param userId   표시한 사용자 ID
     */
    void markHelpful(Long reviewId, Long userId);

    /**
     * 상품의 리뷰 통계를 집계한다 (평균 평점, 평점별 분포).
     *
     * @param productId 대상 상품 ID
     * @return 통계 맵 (avgRating, count, ratingDistribution)
     */
    Map<String, Object> getReviewStatistics(Long productId);

    /**
     * 관리자 검토 대기 중인 리뷰 목록을 조회한다.
     *
     * @param pageable 페이징 정보
     * @return 대기 리뷰 페이지
     */
    Page<ReviewDto> getPendingReviews(Pageable pageable);

    /**
     * 리뷰 데이터 전송 객체.
     */
    record ReviewDto(
            Long id,
            Long productId,
            Long userId,
            String userNickname,
            int rating,
            String title,
            String content,
            String status,
            List<String> imageUrls,
            int helpfulCount,
            int reportCount,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}
}
