package com.springshop.web.mapper;

import com.springshop.domain.cart.Cart;
import com.springshop.web.dto.response.CartResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 장바구니 엔티티 ↔ 응답 DTO 매퍼.
 */
@Mapper(componentModel = "spring")
public interface CartMapper {

    /**
     * Cart 엔티티를 응답 DTO로 변환한다.
     *
     * @param cart 변환할 장바구니 엔티티
     * @return CartResponse DTO
     */
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "totalItemCount", ignore = true)
    @Mapping(target = "appliedCouponCode", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    CartResponse toResponse(Cart cart);

    /**
     * 장바구니 상태를 표시용 레이블로 변환한다.
     */
    default String mapCartStatus(String status) {
        if (status == null) return "ACTIVE";
        return switch (status.toUpperCase()) {
            case "ACTIVE"    -> "ACTIVE";
            case "ORDERED"   -> "ORDERED";
            case "ABANDONED" -> "ABANDONED";
            default          -> status;
        };
    }
}
