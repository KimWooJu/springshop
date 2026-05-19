package com.springshop.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 시작일·종료일을 묶은 기간 값 객체.
 *
 * <p>from <= to 를 항상 만족하며, 두 날짜는 모두 포함(inclusive)된다.
 * 기간 계산, 포함 여부, 다른 기간과의 겹침 판정 등의 메서드를 제공한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Embeddable
public final class DateRange {

    @Column(name = "range_from")
    private LocalDate from;

    @Column(name = "range_to")
    private LocalDate to;

    protected DateRange() {
        // JPA 기본 생성자
    }

    public DateRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "시작일은 null일 수 없습니다");
        Objects.requireNonNull(to, "종료일은 null일 수 없습니다");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("시작일이 종료일 이후: %s > %s".formatted(from, to));
        }
        this.from = from;
        this.to = to;
    }

    public static DateRange of(LocalDate from, LocalDate to) {
        return new DateRange(from, to);
    }

    public static DateRange ofDays(LocalDate from, long days) {
        Objects.requireNonNull(from, "시작일은 null일 수 없습니다");
        if (days < 0) {
            throw new IllegalArgumentException("일수는 음수일 수 없습니다: " + days);
        }
        return new DateRange(from, from.plusDays(days));
    }

    public static DateRange singleDay(LocalDate day) {
        return new DateRange(day, day);
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }

    /**
     * 두 날짜 모두를 포함한 기간이 유효한지(시작 <= 종료) 검사한다.
     */
    public boolean isValid() {
        return from != null && to != null && !from.isAfter(to);
    }

    /**
     * 주어진 일자가 기간에 포함되는지 검사한다(양 끝 포함).
     */
    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date, "검사 일자가 null입니다");
        return !date.isBefore(from) && !date.isAfter(to);
    }

    /**
     * 다른 기간과 겹치는지 검사한다.
     */
    public boolean overlaps(DateRange other) {
        Objects.requireNonNull(other, "비교 대상 기간이 null입니다");
        return !this.to.isBefore(other.from) && !other.to.isBefore(this.from);
    }

    /**
     * 기간의 길이(일수). 같은 날이면 1.
     */
    public long durationInDays() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }

    /**
     * 기간의 길이를 Period 객체로 반환한다.
     */
    public Period period() {
        return Period.between(from, to);
    }

    /**
     * 기간이 만료되었는지 검사한다(기준일이 종료일보다 늦으면 만료).
     */
    public boolean isExpired(LocalDate referenceDate) {
        Objects.requireNonNull(referenceDate, "기준일이 null입니다");
        return referenceDate.isAfter(to);
    }

    /**
     * 두 기간을 합쳐 최소 포함 범위를 반환한다.
     */
    public DateRange union(DateRange other) {
        Objects.requireNonNull(other, "합집합 대상이 null입니다");
        LocalDate newFrom = from.isBefore(other.from) ? from : other.from;
        LocalDate newTo = to.isAfter(other.to) ? to : other.to;
        return new DateRange(newFrom, newTo);
    }

    /**
     * 두 기간이 겹치는 부분을 반환한다(겹치지 않으면 null).
     */
    public DateRange intersection(DateRange other) {
        if (!overlaps(other)) return null;
        LocalDate newFrom = from.isAfter(other.from) ? from : other.from;
        LocalDate newTo = to.isBefore(other.to) ? to : other.to;
        return new DateRange(newFrom, newTo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateRange that)) return false;
        return Objects.equals(from, that.from) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return "[%s ~ %s]".formatted(from, to);
    }
}
