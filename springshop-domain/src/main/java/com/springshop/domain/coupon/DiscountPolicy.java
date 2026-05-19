package com.springshop.domain.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 할인 정책 sealed interface.
 *
 * <p>지원되는 할인 유형:
 * <ul>
 *   <li>{@link FixedAmount}: 정액 할인 (예: 5,000원 할인)</li>
 *   <li>{@link Percentage}: 정률 할인 (예: 10% 할인, 최대 한도 가능)</li>
 *   <li>{@link FreeShipping}: 무료 배송 (조건부 가능)</li>
 *   <li>{@link BuyXGetY}: N개 구매 시 M개 무료</li>
 * </ul>
 *
 * @author SpringShop Domain Team
 */
public sealed interface DiscountPolicy
        permits DiscountPolicy.FixedAmount,
                DiscountPolicy.Percentage,
                DiscountPolicy.FreeShipping,
                DiscountPolicy.BuyXGetY {

    /**
     * 정액 할인.
     */
    record FixedAmount(BigDecimal amount, Currency currency) implements DiscountPolicy {
        public FixedAmount {
            Objects.requireNonNull(amount, "할인 금액은 null일 수 없습니다");
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("고정 할인금액은 양수여야 합니다: " + amount);
            }
            if (currency == null) {
                currency = Currency.getInstance("KRW");
            }
        }

        /**
         * 주문 금액에 대한 할인액. 주문 금액을 초과할 수 없다.
         */
        public BigDecimal calculate(BigDecimal orderAmount) {
            Objects.requireNonNull(orderAmount, "orderAmount 필수");
            return amount.min(orderAmount);
        }
    }

    /**
     * 정률 할인.
     */
    record Percentage(int rate, BigDecimal maxDiscountAmount) implements DiscountPolicy {
        public Percentage {
            if (rate <= 0 || rate > 100) {
                throw new IllegalArgumentException("할인율은 1~100 사이여야 합니다: " + rate);
            }
            if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("최대 할인 금액은 양수여야 합니다: " + maxDiscountAmount);
            }
        }

        public BigDecimal calculate(BigDecimal orderAmount) {
            Objects.requireNonNull(orderAmount, "orderAmount 필수");
            BigDecimal discount = orderAmount.multiply(BigDecimal.valueOf(rate))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            return maxDiscountAmount != null ? discount.min(maxDiscountAmount) : discount;
        }
    }

    /**
     * 무료 배송. 최소 주문 금액 조건이 있을 수 있다.
     */
    record FreeShipping(BigDecimal minOrderAmount) implements DiscountPolicy {
        public FreeShipping {
            if (minOrderAmount == null) {
                minOrderAmount = BigDecimal.ZERO;
            }
            if (minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("최소 주문 금액은 0 이상이어야 합니다: " + minOrderAmount);
            }
        }

        /**
         * 무료 배송 조건 충족 여부.
         */
        public boolean isApplicable(BigDecimal orderAmount) {
            Objects.requireNonNull(orderAmount, "orderAmount 필수");
            return orderAmount.compareTo(minOrderAmount) >= 0;
        }
    }

    /**
     * N+M 행사.
     */
    record BuyXGetY(int buyCount, int freeCount, BigDecimal itemPrice) implements DiscountPolicy {
        public BuyXGetY {
            if (buyCount <= 0) throw new IllegalArgumentException("buyCount는 양수여야 합니다: " + buyCount);
            if (freeCount <= 0) throw new IllegalArgumentException("freeCount는 양수여야 합니다: " + freeCount);
            if (itemPrice == null) itemPrice = BigDecimal.ZERO;
            if (itemPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("itemPrice는 0 이상이어야 합니다: " + itemPrice);
            }
        }

        /**
         * 총 구매 개수에 대해 적용 가능한 무료 항목 수.
         */
        public int calculateFreeItems(int totalItems) {
            int setSize = buyCount + freeCount;
            if (totalItems < setSize) return 0;
            int sets = totalItems / setSize;
            return sets * freeCount;
        }

        /**
         * 무료 항목에 대한 총 할인액.
         */
        public BigDecimal calculate(int totalItems, BigDecimal unitPrice) {
            int freeItems = calculateFreeItems(totalItems);
            BigDecimal effectivePrice = unitPrice != null ? unitPrice : itemPrice;
            return effectivePrice.multiply(BigDecimal.valueOf(freeItems));
        }
    }

    /**
     * 정책을 사용자에게 표시 가능한 텍스트로 변환.
     */
    default String getDisplayDescription() {
        return switch (this) {
            case FixedAmount f -> "%,d원 할인".formatted(f.amount().longValue());
            case Percentage p -> {
                StringBuilder sb = new StringBuilder(p.rate() + "% 할인");
                if (p.maxDiscountAmount() != null) {
                    sb.append(" (최대 ").append(String.format("%,d", p.maxDiscountAmount().longValue())).append("원)");
                }
                yield sb.toString();
            }
            case FreeShipping fs -> "무료배송 (%,d원 이상)".formatted(fs.minOrderAmount().longValue());
            case BuyXGetY b -> "%d개 구매 시 %d개 무료".formatted(b.buyCount(), b.freeCount());
        };
    }

    /**
     * 정책 코드.
     */
    default String typeCode() {
        return switch (this) {
            case FixedAmount f -> "FIXED_AMOUNT";
            case Percentage p -> "PERCENTAGE";
            case FreeShipping fs -> "FREE_SHIPPING";
            case BuyXGetY b -> "BUY_X_GET_Y";
        };
    }
}
