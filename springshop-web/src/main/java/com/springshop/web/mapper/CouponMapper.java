package com.springshop.web.mapper;

import com.springshop.domain.coupon.Coupon;
import com.springshop.web.dto.response.CouponResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 쿠폰 엔티티 ↔ 응답 DTO 매퍼.
 */
@Mapper(componentModel = "spring")
public interface CouponMapper {

    /**
     * Coupon 엔티티를 응답 DTO로 변환한다.
     *
     * @param coupon 변환할 쿠폰 엔티티
     * @return CouponResponse DTO
     */
    @Mapping(target = "status", source = "couponStatus")
    @Mapping(target = "discountDisplay", ignore = true)
    @Mapping(target = "isExpired", ignore = true)
    @Mapping(target = "isUsed", ignore = true)
    @Mapping(target = "remainingDays", ignore = true)
    CouponResponse toResponse(Coupon coupon);

    /**
     * Coupon 엔티티 목록을 응답 DTO 목록으로 변환한다.
     */
    List<CouponResponse> toResponseList(List<Coupon> coupons);

    /**
     * 쿠폰 할인 정보를 표시용 문자열로 변환한다.
     * 예: PERCENT + 10 → "10% 할인", FIXED + 5000 → "5,000원 할인"
     *
     * @param discountType  할인 유형
     * @param discountValue 할인 값
     * @return 표시용 할인 문자열
     */
    default String buildDiscountDisplay(String discountType, java.math.BigDecimal discountValue) {
        if (discountType == null || discountValue == null) return "";
        return switch (discountType.toUpperCase()) {
            case "PERCENT" -> discountValue.stripTrailingZeros().toPlainString() + "% 할인";
            case "FIXED"   -> String.format("%,d원 할인", discountValue.longValue());
            default        -> discountValue + " 할인";
        };
    }
}
