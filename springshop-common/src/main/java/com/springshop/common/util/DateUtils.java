package com.springshop.common.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 날짜/시간 처리 유틸리티.
 */
public final class DateUtils {

    public static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter KOREAN_DATE = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
    public static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final List<DateTimeFormatter> FLEX_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    // 2024-2026년 한국 공휴일 (Java 25 text block)
    private static final Set<LocalDate> HOLIDAYS = parseHolidays("""
            2024-01-01
            2024-02-09
            2024-02-10
            2024-02-11
            2024-02-12
            2024-03-01
            2024-04-10
            2024-05-05
            2024-05-06
            2024-05-15
            2024-06-06
            2024-08-15
            2024-09-16
            2024-09-17
            2024-09-18
            2024-10-01
            2024-10-03
            2024-10-09
            2024-12-25
            2025-01-01
            2025-01-28
            2025-01-29
            2025-01-30
            2025-03-01
            2025-03-03
            2025-05-05
            2025-05-06
            2025-06-06
            2025-08-15
            2025-10-03
            2025-10-05
            2025-10-06
            2025-10-07
            2025-10-09
            2025-12-25
            2026-01-01
            2026-02-16
            2026-02-17
            2026-02-18
            2026-03-01
            2026-05-05
            2026-05-25
            2026-06-06
            2026-08-15
            2026-09-24
            2026-09-25
            2026-09-26
            2026-10-03
            2026-10-09
            2026-12-25
            """);

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static Set<LocalDate> parseHolidays(String raw) {
        Set<LocalDate> set = new HashSet<>();
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                set.add(LocalDate.parse(trimmed, ISO_DATE));
            }
        }
        return set;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(KOREA_ZONE);
    }

    public static LocalDate today() {
        return LocalDate.now(KOREA_ZONE);
    }

    public static LocalDate yesterday() {
        return today().minusDays(1);
    }

    public static LocalDate tomorrow() {
        return today().plusDays(1);
    }

    public static String format(LocalDate date, String pattern) {
        if (date == null) return null;
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String format(LocalDateTime dt, String pattern) {
        if (dt == null) return null;
        return dt.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatIso(LocalDate date) {
        return date == null ? null : date.format(ISO_DATE);
    }

    public static String formatIso(LocalDateTime dt) {
        return dt == null ? null : dt.format(ISO_DATETIME);
    }

    public static String formatKorean(LocalDate date) {
        return date == null ? null : date.format(KOREAN_DATE);
    }

    public static LocalDateTime parseFlexible(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String trimmed = dateStr.trim();
        for (DateTimeFormatter f : FLEX_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, f);
            } catch (DateTimeParseException ignored) {
                // 다음 포맷 시도
            }
        }
        // 날짜만 있는 경우
        try {
            LocalDate d = LocalDate.parse(trimmed, ISO_DATE);
            return d.atStartOfDay();
        } catch (DateTimeParseException ignored) { }

        try {
            LocalDate d = LocalDate.parse(trimmed, COMPACT_DATE);
            return d.atStartOfDay();
        } catch (DateTimeParseException ignored) { }

        throw new IllegalArgumentException("파싱 불가한 날짜 형식: " + dateStr);
    }

    public static boolean isWeekend(LocalDate date) {
        if (date == null) return false;
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    public static boolean isHoliday(LocalDate date) {
        if (date == null) return false;
        return HOLIDAYS.contains(date);
    }

    public static boolean isBusinessDay(LocalDate date) {
        return !isWeekend(date) && !isHoliday(date);
    }

    public static LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    public static LocalDateTime endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }

    public static LocalDate startOfMonth(LocalDate date) {
        return date == null ? null : date.withDayOfMonth(1);
    }

    public static LocalDate endOfMonth(LocalDate date) {
        return date == null ? null : date.withDayOfMonth(date.lengthOfMonth());
    }

    public static LocalDate startOfYear(LocalDate date) {
        return date == null ? null : date.withDayOfYear(1);
    }

    public static LocalDate endOfYear(LocalDate date) {
        return date == null ? null : date.withDayOfYear(date.lengthOfYear());
    }

    /**
     * 영업일 기준으로 days 만큼 더한 날짜.
     */
    public static LocalDate addBusinessDays(LocalDate start, int days) {
        if (start == null) return null;
        LocalDate result = start;
        int added = 0;
        int step = days >= 0 ? 1 : -1;
        int target = Math.abs(days);
        while (added < target) {
            result = result.plusDays(step);
            if (isBusinessDay(result)) {
                added++;
            }
        }
        return result;
    }

    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) return 0;
        return Period.between(birthDate, today()).getYears();
    }

    public static int daysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) return 0;
        return (int) ChronoUnit.DAYS.between(from, to);
    }

    public static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        return Duration.between(from, to).toMinutes();
    }

    public static long toEpochMilli(LocalDateTime dt) {
        if (dt == null) return 0L;
        return dt.atZone(KOREA_ZONE).toInstant().toEpochMilli();
    }

    public static LocalDateTime fromEpochMilli(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), KOREA_ZONE);
    }

    public static ZonedDateTime toKoreanZone(LocalDateTime dt) {
        return dt == null ? null : dt.atZone(KOREA_ZONE);
    }

    /**
     * 상대 시간 표시. "3분 전", "어제", "2026.05.19" 등.
     */
    public static String formatRelative(LocalDateTime dt) {
        if (dt == null) return "";
        LocalDateTime now = now();
        long seconds = Duration.between(dt, now).getSeconds();

        if (seconds < 0) return formatIso(dt);
        if (seconds < 60) return "방금 전";
        if (seconds < 3600) return (seconds / 60) + "분 전";
        if (seconds < 86400) return (seconds / 3600) + "시간 전";

        LocalDate today = today();
        LocalDate target = dt.toLocalDate();

        if (target.equals(today.minusDays(1))) return "어제";
        if (target.equals(today.minusDays(2))) return "그저께";

        long days = ChronoUnit.DAYS.between(target, today);
        if (days < 7) return days + "일 전";
        if (days < 30) return (days / 7) + "주 전";

        return target.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    public static boolean isInRange(LocalDateTime target, LocalDateTime from, LocalDateTime to) {
        if (target == null) return false;
        boolean afterStart = from == null || !target.isBefore(from);
        boolean beforeEnd = to == null || !target.isAfter(to);
        return afterStart && beforeEnd;
    }

    public static boolean isExpired(LocalDateTime expiry) {
        return expiry != null && expiry.isBefore(now());
    }

    public static long secondsUntil(LocalDateTime target) {
        if (target == null) return 0;
        return Duration.between(now(), target).getSeconds();
    }

    public static int countBusinessDays(LocalDate from, LocalDate to) {
        if (from == null || to == null) return 0;
        int count = 0;
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            if (isBusinessDay(cur)) count++;
            cur = cur.plusDays(1);
        }
        return count;
    }

    public static String[] supportedTimeFormats() {
        return new String[]{"HH:mm", "HH:mm:ss", "h:mm a"};
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) return "";
        long s = duration.getSeconds();
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) return "%d시간 %d분 %d초".formatted(h, m, sec);
        if (m > 0) return "%d분 %d초".formatted(m, sec);
        return "%d초".formatted(sec);
    }

    public static List<LocalDate> getHolidayList() {
        return Arrays.stream(HOLIDAYS.toArray(new LocalDate[0]))
                .sorted()
                .toList();
    }
}
