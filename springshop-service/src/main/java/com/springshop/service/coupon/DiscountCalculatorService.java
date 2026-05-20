package com.springshop.service.coupon;

import com.springshop.domain.coupon.Coupon;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 쿠폰 할인 금액 계산 서비스.
 */
@Service
public class DiscountCalculatorService {

    public BigDecimal calculate(Coupon coupon, BigDecimal orderAmount) {
        if (coupon == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return switch (coupon.getDiscountType()) {
            case PERCENTAGE -> {
                BigDecimal discountRate = coupon.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal discount = orderAmount.multiply(discountRate).setScale(0, RoundingMode.HALF_UP);
                if (coupon.getMaxDiscountAmount() != null) {
                    discount = discount.min(coupon.getMaxDiscountAmount());
                }
                yield discount;
            }
            case FIXED_AMOUNT -> coupon.getDiscountValue().min(orderAmount);
            default -> BigDecimal.ZERO;
        };
    }

    public BigDecimal applyDiscount(BigDecimal orderAmount, BigDecimal discountAmount) {
        if (orderAmount == null) return BigDecimal.ZERO;
        if (discountAmount == null) return orderAmount;
        return orderAmount.subtract(discountAmount).max(BigDecimal.ZERO);
    }
}
