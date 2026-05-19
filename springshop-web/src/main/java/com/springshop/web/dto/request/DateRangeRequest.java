package com.springshop.web.dto.request;

import com.springshop.web.validator.DateRangeConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 기간 범위 요청 DTO.
 *
 * <p>통계/조회 API에서 공통으로 사용되는 시작-종료 기간을 표현한다.
 * 클래스 레벨 {@link DateRangeConstraint} 으로 startDate &lt; endDate 검증을 수행한다.
 *
 * <pre>
 *   GET /api/v1/admin/dashboard?startDate=2026-01-01&endDate=2026-01-31
 * </pre>
 */
@Schema(description = "기간 범위 요청")
@DateRangeConstraint
public record DateRangeRequest(

        @Schema(description = "시작 일자", example = "2026-01-01")
        @NotNull(message = "시작 일자는 필수입니다.")
        LocalDate startDate,

        @Schema(description = "종료 일자 (포함)", example = "2026-01-31")
        @NotNull(message = "종료 일자는 필수입니다.")
        LocalDate endDate,

        @Schema(description = "기간 단위", allowableValues = {"DAY", "WEEK", "MONTH", "QUARTER", "YEAR"})
        String unit,

        @Schema(description = "시간대 (IANA TZ)", example = "Asia/Seoul")
        String timezone
) {

    public DateRangeRequest {
        if (unit == null || unit.isBlank()) unit = "DAY";
        if (timezone == null || timezone.isBlank()) timezone = "Asia/Seoul";
    }

    /** start, end 동일 시 단일 일자 조회. */
    public boolean isSingleDay() {
        return startDate != null && startDate.equals(endDate);
    }

    /** 기간 일수 (양 끝 포함). */
    public long toDays() {
        if (startDate == null || endDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** 시작 자정 LocalDateTime. */
    public LocalDateTime startAtMidnight() {
        return startDate == null ? null : startDate.atStartOfDay();
    }

    /** 종료 일자의 23:59:59.999 LocalDateTime. */
    public LocalDateTime endOfDay() {
        return endDate == null ? null : endDate.atTime(23, 59, 59, 999_000_000);
    }

    /** 1년 이내 범위인지. */
    public boolean isWithinOneYear() {
        return toDays() <= 366;
    }

    public boolean isValid() {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    /** 사전 정의된 기간 enum. */
    public enum PresetRange {
        TODAY, YESTERDAY, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, LAST_MONTH, THIS_YEAR
    }

    /** 프리셋으로부터 DateRangeRequest 생성. */
    public static DateRangeRequest fromPreset(PresetRange preset) {
        LocalDate today = LocalDate.now();
        return switch (preset) {
            case TODAY -> new DateRangeRequest(today, today, "DAY", "Asia/Seoul");
            case YESTERDAY -> new DateRangeRequest(today.minusDays(1), today.minusDays(1), "DAY", "Asia/Seoul");
            case LAST_7_DAYS -> new DateRangeRequest(today.minusDays(6), today, "DAY", "Asia/Seoul");
            case LAST_30_DAYS -> new DateRangeRequest(today.minusDays(29), today, "DAY", "Asia/Seoul");
            case THIS_MONTH -> new DateRangeRequest(today.withDayOfMonth(1), today, "DAY", "Asia/Seoul");
            case LAST_MONTH -> {
                LocalDate firstOfLast = today.minusMonths(1).withDayOfMonth(1);
                LocalDate lastOfLast = today.withDayOfMonth(1).minusDays(1);
                yield new DateRangeRequest(firstOfLast, lastOfLast, "DAY", "Asia/Seoul");
            }
            case THIS_YEAR -> new DateRangeRequest(today.withDayOfYear(1), today, "MONTH", "Asia/Seoul");
        };
    }
}
