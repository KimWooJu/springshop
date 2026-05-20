package com.springshop.web.mapper;

import com.springshop.domain.review.Review;
import com.springshop.web.dto.response.ReviewResponse;
import com.springshop.web.dto.response.ReviewSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 리뷰 엔티티 ↔ 응답 DTO 매퍼.
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    /**
     * Review 엔티티를 상세 응답 DTO로 변환한다.
     *
     * @param review 변환할 리뷰 엔티티
     * @return ReviewResponse DTO
     */
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "imageUrls", ignore = true)
    @Mapping(target = "editable", ignore = true)
    ReviewResponse toResponse(Review review);

    /**
     * Review 엔티티를 요약 응답 DTO로 변환한다.
     *
     * @param review 변환할 리뷰 엔티티
     * @return ReviewSummaryResponse DTO
     */
    ReviewSummaryResponse toSummaryResponse(Review review);

    /**
     * Review 엔티티 목록을 상세 응답 DTO 목록으로 변환한다.
     */
    List<ReviewResponse> toResponseList(List<Review> reviews);

    /**
     * 리뷰 상태를 표시용 한글 레이블로 변환한다.
     */
    default String mapReviewStatusLabel(String status) {
        if (status == null) return "대기";
        return switch (status.toUpperCase()) {
            case "PENDING"  -> "검토 대기";
            case "APPROVED" -> "게시 완료";
            case "REJECTED" -> "반려";
            case "REPORTED" -> "신고 처리 중";
            case "DELETED"  -> "삭제됨";
            default         -> status;
        };
    }
}
