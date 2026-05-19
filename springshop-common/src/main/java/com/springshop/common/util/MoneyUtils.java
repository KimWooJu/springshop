package com.springshop.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 금액(BigDecimal) 처리 유틸리티.
 */
public final class MoneyUtils {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP);
    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("0.10");

    private static final DecimalFormat KRW_FORMAT = new DecimalFormat("#,##0");
    private static final NumberFormat USD_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private MoneyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String format(BigDecimal amount) {
        if (amount == null) return "₩0";
        return "₩" + KRW_FORMAT.format(amount);
    }

    public static String formatPlain(BigDecimal amount) {
        if (amount == null) return "0";
        return KRW_FORMAT.format(amount);
    }

    public static String formatUSD(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return USD_FORMAT.format(amount);
    }

    public static BigDecimal roundDown(BigDecimal amount, int scale) {
        if (amount == null) return ZERO;
        return amount.setScale(scale, RoundingMode.DOWN);
    }

    public static BigDecimal roundUp(BigDecimal amount, int scale) {
        if (amount == null) return ZERO;
        return amount.setScale(scale, RoundingMode.UP);
    }

    public static BigDecimal roundHalf(BigDecimal amount, int scale) {
        if (amount == null) return ZERO;
        return amount.setScale(scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal roundToWon(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateVat(BigDecimal amount) {
        return calculateVat(amount, DEFAULT_VAT_RATE);
    }

    public static BigDecimal calculateVat(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null) return ZERO;
        return roundToWon(amount.multiply(rate));
    }

    public static BigDecimal addVat(BigDecimal amount) {
        if (amount == null) return ZERO;
        return roundToWon(amount.add(calculateVat(amount)));
    }

    public static BigDecimal removeVat(BigDecimal amountIncludingVat) {
        if (amountIncludingVat == null) return ZERO;
        BigDecimal divisor = BigDecimal.ONE.add(DEFAULT_VAT_RATE);
        return amountIncludingVat.divide(divisor, 0, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateDiscount(BigDecimal price, BigDecimal discountRate) {
        if (price == null || discountRate == null) return ZERO;
        return roundToWon(price.multiply(discountRate));
    }

    public static BigDecimal applyDiscount(BigDecimal price, BigDecimal discountRate) {
        if (price == null) return ZERO;
        if (discountRate == null) return price;
        return price.subtract(calculateDiscount(price, discountRate));
    }

    public static BigDecimal applyFlatDiscount(BigDecimal price, BigDecimal discountAmount) {
        if (price == null) return ZERO;
        if (discountAmount == null) return price;
        BigDecimal result = price.subtract(discountAmount);
        return result.signum() < 0 ? ZERO : result;
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.signum() > 0;
    }

    public static boolean isNegative(BigDecimal amount) {
        return amount != null && amount.signum() < 0;
    }

    public static boolean isZero(BigDecimal amount) {
        return amount != null && amount.signum() == 0;
    }

    public static boolean isPositiveOrZero(BigDecimal amount) {
        return amount != null && amount.signum() >= 0;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static BigDecimal sum(BigDecimal... values) {
        BigDecimal total = ZERO;
        if (values == null) return total;
        for (BigDecimal v : values) {
            if (v != null) total = total.add(v);
        }
        return total;
    }

    public static BigDecimal percentage(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() == 0) return ZERO;
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }

    public static String formatPercent(BigDecimal rate) {
        if (rate == null) return "0%";
        BigDecimal pct = rate.multiply(HUNDRED).setScale(1, RoundingMode.HALF_UP);
        return pct.toPlainString() + "%";
    }

    public static BigDecimal nullSafe(BigDecimal amount) {
        return amount == null ? ZERO : amount;
    }
}
