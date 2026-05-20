package com.springshop.web.mapper;

import com.springshop.domain.order.Order;
import com.springshop.web.dto.response.OrderResponse;
import com.springshop.web.dto.response.OrderSummaryResponse;
import com.springshop.web.dto.response.OrderTrackingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 주문 엔티티 ↔ 응답 DTO 매퍼.
 *
 * <p>주문 상세, 요약, 배송 추적 등 다양한 형태의 응답 DTO로 변환한다.
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    /**
     * Order 엔티티를 상세 응답 DTO로 변환한다.
     *
     * @param order 변환할 주문 엔티티
     * @return OrderResponse DTO
     */
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "shipping", ignore = true)
    @Mapping(target = "payment", ignore = true)
    OrderResponse toResponse(Order order);

    /**
     * Order 엔티티를 요약 응답 DTO로 변환한다.
     *
     * @param order 변환할 주문 엔티티
     * @return OrderSummaryResponse DTO
     */
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "itemCount", ignore = true)
    OrderSummaryResponse toSummaryResponse(Order order);

    /**
     * Order 엔티티를 배송 추적 응답 DTO로 변환한다.
     *
     * @param order 변환할 주문 엔티티
     * @return OrderTrackingResponse DTO
     */
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "estimatedDelivery", ignore = true)
    @Mapping(target = "carrierCode", ignore = true)
    OrderTrackingResponse toTrackingResponse(Order order);

    /**
     * Order 엔티티 목록을 요약 응답 DTO 목록으로 변환한다.
     */
    List<OrderSummaryResponse> toSummaryResponseList(List<Order> orders);

    /**
     * 주문 상태 코드를 표시용 한글 레이블로 변환한다.
     *
     * @param status 주문 상태 코드
     * @return 표시용 레이블
     */
    default String mapStatusLabel(String status) {
        if (status == null) return "알 수 없음";
        return switch (status) {
            case "PENDING"            -> "결제 대기";
            case "PAYMENT_COMPLETED"  -> "결제 완료";
            case "PREPARING"          -> "상품 준비 중";
            case "SHIPPED"            -> "배송 중";
            case "DELIVERED"          -> "배송 완료";
            case "COMPLETED"          -> "구매 확정";
            case "CANCELLED"          -> "주문 취소";
            case "REFUND_REQUESTED"   -> "환불 요청";
            case "REFUNDED"           -> "환불 완료";
            default                   -> status;
        };
    }
}
