package com.springshop.service.order;

import com.springshop.domain.order.Order;
import com.springshop.domain.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주문 핵심 서비스.
 *
 * <p>주문 생성/취소/반품/조회/상태 전이를 담당한다.
 * 결제/재고/배송과의 협업은 {@link OrderProcessingService}에 위임한다.
 */
public interface OrderService {

    /** 주문 생성 — 재고 확인, 가격 계산, 쿠폰 적용, 주문 엔티티 영속화 */
    Order placeOrder(PlaceOrderCommand command);

    /** 주문 취소 — 환불, 재고 해제, 알림 */
    Order cancel(Long orderId, String reason, String requester);

    /** 주문 반품 신청 */
    Order requestReturn(Long orderId, String reason, List<Long> orderItemIds);

    /** 반품 승인 */
    Order approveReturn(Long orderId, String approver);

    /** 반품 거절 */
    Order rejectReturn(Long orderId, String reason, String approver);

    /** 배송 시작 처리 */
    Order markShipped(Long orderId, String trackingNumber, String carrier);

    /** 배송 완료 처리 */
    Order markDelivered(Long orderId);

    /** 주문 확정 (결제 완료 후) */
    Order confirm(Long orderId);

    /** 주문 상태 강제 변경 (관리자) */
    Order changeStatus(Long orderId, OrderStatus newStatus, String reason);

    Order findById(Long orderId);

    Optional<Order> findByOrderNumber(String orderNumber);

    /** 사용자 주문 목록 */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /** 상태별 주문 목록 */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /** 기간 + 사용자 조건 검색 */
    Page<Order> search(OrderSearchCondition condition, Pageable pageable);

    /** 사용자의 미결제(또는 처리 대기) 주문 */
    List<Order> findPendingByUser(Long userId);

    /** 일정 시간 이상 미결제 상태인 주문 (스케줄러용) */
    List<Order> findUnpaidOrdersOlderThan(LocalDateTime threshold);

    /** 총 주문 수 / 매출 합계 */
    OrderTotals computeTotals(LocalDateTime from, LocalDateTime to);

    /** 사용자별 누적 주문 통계 */
    UserOrderTotals computeUserTotals(Long userId);

    record PlaceOrderCommand(
        Long userId,
        Long addressId,
        List<LineItem> items,
        Long couponId,
        String paymentMethod,
        String memo
    ) {
        public record LineItem(Long productId, long quantity, BigDecimal expectedUnitPrice) {}
    }

    record OrderSearchCondition(
        Long userId,
        OrderStatus status,
        LocalDateTime from,
        LocalDateTime to,
        String orderNumberContains
    ) {}

    record OrderTotals(long orderCount, BigDecimal grossAmount, BigDecimal discountAmount, BigDecimal netAmount) {}

    record UserOrderTotals(Long userId, long totalOrders, BigDecimal totalSpent, BigDecimal averageOrderValue) {}
}
