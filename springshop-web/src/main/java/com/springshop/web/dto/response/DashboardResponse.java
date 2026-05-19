package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 관리자 대시보드 응답.
 *
 * <p>관리자 메인 화면 위젯에 사용되는 모든 통계를 한 번에 전달한다.
 * 오늘/이번달 매출, 신규 회원, 미처리 주문, 재고 부족 상품 등을 포함한다.
 */
@Schema(description = "관리자 대시보드 응답")
public record DashboardResponse(
        @Schema(description = "오늘 매출 합계") BigDecimal todayRevenue,
        @Schema(description = "어제 매출 합계") BigDecimal yesterdayRevenue,
        @Schema(description = "이번 달 매출") BigDecimal monthRevenue,
        @Schema(description = "지난 달 매출") BigDecimal lastMonthRevenue,
        @Schema(description = "월 매출 성장률(%)") double monthGrowthRate,
        @Schema(description = "오늘 주문 수") long todayOrderCount,
        @Schema(description = "어제 주문 수") long yesterdayOrderCount,
        @Schema(description = "이번 달 주문 수") long monthOrderCount,
        @Schema(description = "미처리 주문 수 (결제완료/준비중)") long pendingOrderCount,
        @Schema(description = "오늘 신규 회원") long todayNewUsers,
        @Schema(description = "전체 회원 수") long totalUsers,
        @Schema(description = "활성 회원 수 (최근 30일 로그인)") long activeUsers,
        @Schema(description = "오늘 결제 실패") long todayFailedPayments,
        @Schema(description = "오늘 환불 건수") long todayRefundCount,
        @Schema(description = "오늘 환불 금액") BigDecimal todayRefundAmount,
        @Schema(description = "재고 부족 상품 수") long lowStockProductCount,
        @Schema(description = "품절 상품 수") long outOfStockProductCount,
        @Schema(description = "검수 대기 리뷰 수") long pendingReviewCount,
        @Schema(description = "신고된 리뷰 수") long reportedReviewCount,
        @Schema(description = "Top 5 베스트셀러") List<BestSeller> topSellers,
        @Schema(description = "Top 5 인기 카테고리") List<TopCategory> topCategories,
        @Schema(description = "최근 7일 일별 매출") List<DailyMetric> dailyRevenue,
        @Schema(description = "결제 수단별 비율") Map<String, Double> paymentMethodShare,
        @Schema(description = "시스템 상태", example = "HEALTHY",
                allowableValues = {"HEALTHY", "WARNING", "DEGRADED", "DOWN"})
        String systemHealth,
        @Schema(description = "생성 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime generatedAt
) {

    public DashboardResponse {
        topSellers = topSellers == null ? List.of() : List.copyOf(topSellers);
        topCategories = topCategories == null ? List.of() : List.copyOf(topCategories);
        dailyRevenue = dailyRevenue == null ? List.of() : List.copyOf(dailyRevenue);
        paymentMethodShare = paymentMethodShare == null ? Map.of() : Map.copyOf(paymentMethodShare);
    }

    @Schema(description = "베스트셀러")
    public record BestSeller(
            Long productId, String name, String thumbnailUrl,
            long soldCount, BigDecimal revenue, int rank
    ) {}

    @Schema(description = "인기 카테고리")
    public record TopCategory(
            Long categoryId, String name, long orderCount, BigDecimal revenue, int rank
    ) {}

    @Schema(description = "일별 메트릭")
    public record DailyMetric(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            java.time.LocalDate date,
            long orderCount,
            BigDecimal revenue,
            long newUserCount
    ) {}
}
