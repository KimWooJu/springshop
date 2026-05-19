package com.springshop.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 금액을 표현하는 불변 값 객체.
 *
 * <p>BigDecimal 기반 금액과 ISO 4217 통화 코드(예: KRW, USD, JPY)를 함께 관리한다.
 * 사칙연산은 동일 통화 사이에서만 허용되며, 음수 금액은 일부 연산(예: 차감)에서만
 * 허용된다.</p>
 *
 * <p>JPA @Embeddable로 지정되어 엔티티 내부에 평탄화되어 저장된다.</p>
 *
 * @author SpringShop Domain Team
 */
@Embeddable
public final class Money {

    public static final String DEFAULT_CURRENCY_CODE = "KRW";
    public static final int DEFAULT_SCALE = 2;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currencyCode;

    protected Money() {
        // JPA 기본 생성자
    }

    public Money(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "금액은 null일 수 없습니다");
        Objects.requireNonNull(currencyCode, "통화 코드는 null일 수 없습니다");
        validateCurrency(currencyCode);
        this.amount = amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
        this.currencyCode = currencyCode;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, currencyCode);
    }

    public static Money ofKrw(long amount) {
        return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY_CODE);
    }

    public static Money ofKrw(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY_CODE);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, DEFAULT_CURRENCY_CODE);
    }

    public static Money zero(String currencyCode) {
        return new Money(BigDecimal.ZERO, currencyCode);
    }

    private static void validateCurrency(String code) {
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 통화 코드: " + code, e);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("차감 후 음수 금액은 허용되지 않습니다: " + result);
        }
        return new Money(result, this.currencyCode);
    }

    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("음수 곱셈은 허용되지 않습니다: " + factor);
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currencyCode);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "곱셈 인자가 null입니다");
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("음수 곱셈은 허용되지 않습니다: " + factor);
        }
        return new Money(this.amount.multiply(factor), this.currencyCode);
    }

    public Money divide(int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("0으로 나눌 수 없습니다");
        }
        if (divisor < 0) {
            throw new IllegalArgumentException("음수 나눗셈은 허용되지 않습니다: " + divisor);
        }
        BigDecimal result = this.amount.divide(BigDecimal.valueOf(divisor), DEFAULT_SCALE, RoundingMode.HALF_UP);
        return new Money(result, this.currencyCode);
    }

    public Money percent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("백분율은 0~100 사이여야 합니다: " + percent);
        }
        BigDecimal rate = BigDecimal.valueOf(percent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return new Money(this.amount.multiply(rate), this.currencyCode);
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "비교 대상 Money가 null입니다");
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException(
                    "통화 불일치: %s vs %s".formatted(this.currencyCode, other.currencyCode));
        }
    }

    public String format() {
        return "%s %s".formatted(currencyCode, amount.toPlainString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money other)) return false;
        return Objects.equals(amount, other.amount) && Objects.equals(currencyCode, other.currencyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currencyCode);
    }

    @Override
    public String toString() {
        return format();
    }
}
