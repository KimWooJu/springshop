package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 상품 리뷰 작성 요청 DTO.
 *
 * <p>주문 완료된 상품에 대해서만 작성 가능하다. 별점(1-5)과 본문은 필수,
 * 이미지/영상 URL과 추천 여부는 선택이다.
 *
 * <p>compact constructor 에서 본문 trim, 이미지 개수 제한, 중복 제거를 수행한다.
 *
 * <pre>
 *   POST /api/v1/reviews
 *   {
 *     "productId": 1024,
 *     "orderId": 9001,
 *     "rating": 5,
 *     "title": "정말 만족합니다",
 *     "content": "배송도 빠르고 품질도 좋습니다. 재구매 의사 있어요!",
 *     "imageUrls": ["https://cdn.example.com/review/abc.jpg"],
 *     "recommended": true
 *   }
 * </pre>
 */
@Schema(description = "리뷰 작성 요청")
public record CreateReviewRequest(

        @Schema(description = "상품 ID", example = "1024")
        @NotNull(message = "상품 ID는 필수입니다.")
        @Positive
        Long productId,

        @Schema(description = "주문 ID (구매 확인용)", example = "9001")
        @NotNull(message = "주문 ID는 필수입니다.")
        @Positive
        Long orderId,

        @Schema(description = "별점 (1-5)", example = "5")
        @NotNull(message = "별점은 필수입니다.")
        @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5 이하여야 합니다.")
        Integer rating,

        @Schema(description = "리뷰 제목", example = "정말 만족합니다")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @Schema(description = "리뷰 본문", example = "품질이 매우 좋아요!")
        @NotBlank(message = "본문은 필수입니다.")
        @Size(min = 10, max = 2000, message = "본문은 10~2000자여야 합니다.")
        String content,

        @Schema(description = "리뷰 이미지 URL 목록 (최대 5장)")
        List<String> imageUrls,

        @Schema(description = "리뷰 영상 URL (선택, 1개)")
        String videoUrl,

        @Schema(description = "다른 사용자에게 추천하는지", defaultValue = "true")
        Boolean recommended,

        @Schema(description = "익명 작성 여부", defaultValue = "false")
        Boolean anonymous
) {

    /** 이미지 최대 개수. */
    public static final int MAX_IMAGES = 5;

    public CreateReviewRequest {
        if (title != null) {
            title = title.trim();
            if (title.isBlank()) title = null;
        }
        if (content != null) {
            content = content.trim();
        }
        if (imageUrls == null) {
            imageUrls = List.of();
        } else if (imageUrls.size() > MAX_IMAGES) {
            throw new IllegalArgumentException("이미지는 최대 " + MAX_IMAGES + "장까지 가능합니다.");
        } else {
            imageUrls = imageUrls.stream()
                    .filter(u -> u != null && !u.isBlank())
                    .distinct()
                    .toList();
        }
        if (recommended == null) recommended = Boolean.TRUE;
        if (anonymous == null) anonymous = Boolean.FALSE;
    }

    /** 미디어가 첨부됐는지 여부. */
    public boolean hasMedia() {
        return !imageUrls.isEmpty() || (videoUrl != null && !videoUrl.isBlank());
    }

    /** 긴 본문 여부 (300자 초과 시 우수 리뷰 후보). */
    public boolean isDetailedReview() {
        return content != null && content.length() > 300;
    }
}
