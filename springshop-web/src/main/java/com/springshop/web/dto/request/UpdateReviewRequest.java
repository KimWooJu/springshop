package com.springshop.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 리뷰 수정 요청 DTO.
 *
 * <p>작성자 본인만 수정 가능하며, 별점/제목/본문/이미지를 부분 갱신한다.
 * 모든 필드는 nullable — null 인 필드는 기존 값을 유지한다.
 *
 * <p>compact constructor 에서 trim 및 이미지 개수 제한을 수행한다.
 */
@Schema(description = "리뷰 수정 요청")
public record UpdateReviewRequest(

        @Schema(description = "별점 (1-5, null=유지)", example = "4")
        @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5 이하여야 합니다.")
        Integer rating,

        @Schema(description = "리뷰 제목 (null=유지)")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @Schema(description = "리뷰 본문 (null=유지)")
        @Size(min = 10, max = 2000, message = "본문은 10~2000자여야 합니다.")
        String content,

        @Schema(description = "리뷰 이미지 URL 목록 (null=유지, 빈 리스트=전체 삭제)")
        List<String> imageUrls,

        @Schema(description = "추천 여부 (null=유지)")
        Boolean recommended,

        @Schema(description = "수정 사유 (감사 로그용)")
        @Size(max = 200)
        String editReason
) {

    public UpdateReviewRequest {
        if (title != null) {
            title = title.trim();
        }
        if (content != null) {
            content = content.trim();
        }
        if (imageUrls != null) {
            if (imageUrls.size() > CreateReviewRequest.MAX_IMAGES) {
                throw new IllegalArgumentException("이미지는 최대 5장까지 가능합니다.");
            }
            imageUrls = imageUrls.stream().filter(u -> u != null && !u.isBlank()).distinct().toList();
        }
        if (editReason != null) {
            editReason = editReason.trim();
            if (editReason.isBlank()) editReason = null;
        }
    }

    /** 어떤 필드라도 변경되었는지. */
    public boolean hasAnyChange() {
        return rating != null || title != null || content != null
                || imageUrls != null || recommended != null;
    }

    /** 본문 변경 여부. */
    public boolean isContentChange() {
        return content != null;
    }
}
