package com.springshop.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 상품 리뷰 통계 응답.
 *
 * <p>상품 상세 페이지 상단의 평점 위젯에 사용된다.
 * 1~5점 별 카운트 분포, 평균 평점, 전체 리뷰 수를 포함한다.
 */
@Schema(description = "리뷰 통계 응답")
public record ReviewSummaryResponse(
        @Schema(description = "상품 ID") Long productId,
        @Schema(description = "평균 평점", example = "4.65") double averageRating,
        @Schema(description = "전체 리뷰 수", example = "1283") long totalCount,
        @Schema(description = "최근 30일 리뷰 수", example = "47") long recentCount,
        @Schema(description = "평점별 분포 (1~5)") Map<Integer, Long> ratingDistribution,
        @Schema(description = "평점별 비율(%) (1~5)") Map<Integer, Double> ratingPercentage,
        @Schema(description = "구매 인증 리뷰 수", example = "1240") long verifiedCount,
        @Schema(description = "이미지 포함 리뷰 수", example = "352") long withImageCount,
        @Schema(description = "추천 비율(4점 이상 비율)", example = "92.5") double recommendRate
) {

    public ReviewSummaryResponse {
        ratingDistribution = ratingDistribution == null ? Map.of() : new TreeMap<>(ratingDistribution);
        ratingPercentage = ratingPercentage == null ? Map.of() : new TreeMap<>(ratingPercentage);
    }

    /** 1~5점 카운트로부터 통계 응답을 만든다. */
    public static ReviewSummaryResponse from(Long productId, Map<Integer, Long> distribution) {
        long total = distribution.values().stream().mapToLong(Long::longValue).sum();
        double avg = total == 0 ? 0.0
                : distribution.entrySet().stream()
                        .mapToDouble(e -> e.getKey() * e.getValue())
                        .sum() / total;

        Map<Integer, Double> pct = new LinkedHashMap<>();
        long finalTotal = total;
        distribution.forEach((rating, count) -> {
            double percent = finalTotal == 0 ? 0.0
                    : Math.round((count * 1000.0 / finalTotal)) / 10.0;
            pct.put(rating, percent);
        });

        long recommend = distribution.entrySet().stream()
                .filter(e -> e.getKey() >= 4)
                .mapToLong(Map.Entry::getValue)
                .sum();
        double recommendRate = total == 0 ? 0.0
                : Math.round(recommend * 1000.0 / total) / 10.0;

        return new ReviewSummaryResponse(
                productId, Math.round(avg * 100.0) / 100.0,
                total, 0L, distribution, pct, 0L, 0L, recommendRate
        );
    }

    /** 빈 통계. */
    public static ReviewSummaryResponse empty(Long productId) {
        Map<Integer, Long> dist = new TreeMap<>();
        for (int i = 1; i <= 5; i++) dist.put(i, 0L);
        return new ReviewSummaryResponse(
                productId, 0.0, 0L, 0L, dist, Map.of(), 0L, 0L, 0.0
        );
    }

    /** 특정 별점의 비율을 조회한다. */
    public double percentageOf(int rating) {
        return ratingPercentage.getOrDefault(rating, 0.0);
    }
}
