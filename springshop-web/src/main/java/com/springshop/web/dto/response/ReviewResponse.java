package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 리뷰 응답.
 *
 * <p>상품 상세 페이지 리뷰 섹션, 마이페이지 내 리뷰 등에 사용된다.
 * 신고/도움됨 카운트 등 상호작용 메트릭, 관리자 검수 상태도 포함한다.
 */
@Schema(description = "상품 리뷰 응답")
public record ReviewResponse(
        @Schema(description = "리뷰 ID") Long id,
        @Schema(description = "상품 ID") Long productId,
        @Schema(description = "주문 ID (구매 인증)") Long orderId,
        @Schema(description = "작성자 ID") Long userId,
        @Schema(description = "작성자 닉네임 (마스킹)", example = "ho***길") String userName,
        @Schema(description = "작성자 프로필 이미지") String userProfileImage,
        @Schema(description = "평점 (1~5)", example = "5") int rating,
        @Schema(description = "리뷰 제목") String title,
        @Schema(description = "리뷰 본문") String content,
        @Schema(description = "이미지 URL 목록") List<String> imageUrls,
        @Schema(description = "구매 옵션 표시", example = "색상: 블랙 / 사이즈: L") String optionDisplay,
        @Schema(description = "구매 인증 여부", example = "true") boolean isVerifiedPurchase,
        @Schema(description = "관리자 검수 상태",
                allowableValues = {"PENDING", "APPROVED", "REJECTED", "HIDDEN"})
        String approvalStatus,
        @Schema(description = "도움됨 수") int helpfulCount,
        @Schema(description = "도움안됨 수") int notHelpfulCount,
        @Schema(description = "신고 수") int reportCount,
        @Schema(description = "관리자 답변") String adminReply,
        @Schema(description = "내가 작성한 리뷰 여부") boolean editable,
        @Schema(description = "작성 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @Schema(description = "수정 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {

    public ReviewResponse {
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }

    /** 별점 시각화 문자열. */
    public String ratingStars() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    /** 단순 헬퍼. */
    public static ReviewResponse of(Long id, Long productId, Long userId, String userName,
                                    int rating, String title, String content,
                                    boolean isVerifiedPurchase) {
        LocalDateTime now = LocalDateTime.now();
        return new ReviewResponse(
                id, productId, null, userId, userName, null,
                rating, title, content, List.of(), null,
                isVerifiedPurchase, "APPROVED", 0, 0, 0, null, false, now, now
        );
    }

    /** 관리자 답변이 달린 인스턴스 반환. */
    public ReviewResponse withAdminReply(String reply) {
        return new ReviewResponse(
                id, productId, orderId, userId, userName, userProfileImage,
                rating, title, content, imageUrls, optionDisplay,
                isVerifiedPurchase, approvalStatus, helpfulCount, notHelpfulCount,
                reportCount, reply, editable, createdAt, updatedAt
        );
    }
}
