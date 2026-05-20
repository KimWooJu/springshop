package com.springshop.web.mapper;

import com.springshop.domain.payment.Payment;
import com.springshop.web.dto.response.PaymentResponse;
import com.springshop.web.dto.response.RefundResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 결제 엔티티 ↔ 응답 DTO 매퍼.
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper {

    /**
     * Payment 엔티티를 응답 DTO로 변환한다.
     *
     * @param payment 변환할 결제 엔티티
     * @return PaymentResponse DTO
     */
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "method", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "refundable", ignore = true)
    PaymentResponse toResponse(Payment payment);

    /**
     * Payment 엔티티 목록을 응답 DTO 목록으로 변환한다.
     */
    List<PaymentResponse> toResponseList(List<Payment> payments);

    /**
     * 결제 상태를 표시용 한글 레이블로 변환한다.
     */
    default String mapPaymentStatusLabel(String status) {
        if (status == null) return "알 수 없음";
        return switch (status) {
            case "PENDING"          -> "결제 대기";
            case "COMPLETED"        -> "결제 완료";
            case "FAILED"           -> "결제 실패";
            case "CANCELLED"        -> "결제 취소";
            case "REFUND_REQUESTED" -> "환불 요청";
            case "REFUNDED"         -> "환불 완료";
            default                 -> status;
        };
    }
}
