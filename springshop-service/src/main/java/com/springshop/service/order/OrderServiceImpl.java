package com.springshop.service.order;

import com.springshop.domain.order.Order;
import com.springshop.domain.order.OrderItem;
import com.springshop.domain.order.OrderRepository;
import com.springshop.domain.order.OrderStatus;
import com.springshop.domain.order.event.OrderCancelledEvent;
import com.springshop.domain.order.event.OrderDeliveredEvent;
import com.springshop.domain.order.event.OrderPlacedEvent;
import com.springshop.domain.order.event.OrderShippedEvent;
import com.springshop.domain.order.event.OrderReturnRequestedEvent;
import com.springshop.domain.product.Product;
import com.springshop.domain.product.ProductRepository;
import com.springshop.domain.product.ProductStatus;
import com.springshop.domain.common.exception.InvalidStateException;
import com.springshop.domain.common.exception.ResourceNotFoundException;
import com.springshop.service.coupon.CouponService;
import com.springshop.service.coupon.DiscountCalculatorService;
import com.springshop.service.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@link OrderService} 기본 구현.
 *
 * <p>주문 생성 시 다음 흐름을 보장한다:
 * <ol>
 *   <li>입력 유효성 검증 (수량/주소/사용자)</li>
 *   <li>상품 조회 + 가격 정합성 검사</li>
 *   <li>재고 예약 ({@link InventoryService#reserveStock})</li>
 *   <li>쿠폰 적용/할인 계산</li>
 *   <li>주문 엔티티 영속화</li>
 *   <li>{@code OrderPlacedEvent} 이벤트 발행</li>
 * </ol>
 *
 * <p>실패 시 트랜잭션 롤백으로 재고 예약도 자동 해제된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final DiscountCalculatorService discountCalculator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        log.info("주문 생성 시도: userId={}, items={}", command.userId(), command.items().size());
        validate(command);

        var orderNumber = generateOrderNumber();
        var lineItems = resolveLineItems(command.items());

        // 재고 예약
        var reservations = new ArrayList<InventoryService.Reservation>();
        for (var item : lineItems) {
            var reservation = inventoryService.reserveStock(
                item.product().getId(), item.quantity(), orderNumber
            );
            reservations.add(reservation);
        }

        // 합계 계산
        var subtotal = lineItems.stream()
            .map(LineDetail::lineAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 쿠폰/할인 적용
        BigDecimal discount = BigDecimal.ZERO;
        if (command.couponId() != null) {
            var policy = couponService.resolveDiscountPolicy(command.couponId(), command.userId());
            discount = discountCalculator.calculateDiscount(policy, subtotal);
            couponService.reserveUsage(command.couponId(), command.userId(), orderNumber);
        }

        var net = subtotal.subtract(discount).max(BigDecimal.ZERO);

        var order = Order.create(
            orderNumber,
            command.userId(),
            command.addressId(),
            subtotal,
            discount,
            net,
            command.paymentMethod(),
            command.memo()
        );
        lineItems.forEach(d ->
            order.addItem(OrderItem.of(
                d.product().getId(),
                d.product().getName(),
                d.product().getSku(),
                d.unitPrice(),
                d.quantity()
            ))
        );

        var saved = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderPlacedEvent(
            saved.getId(),
            saved.getOrderNumber(),
            saved.getUserId(),
            saved.getNetAmount(),
            saved.getItems().stream().map(OrderItem::getProductId).toList(),
            LocalDateTime.now()
        ));

        log.info("주문 생성 완료: id={}, orderNumber={}, net={}",
            saved.getId(), saved.getOrderNumber(), saved.getNetAmount());
        return saved;
    }

    @Override
    @Transactional
    public Order cancel(Long orderId, String reason, String requester) {
        var order = load(orderId);
        if (!order.getStatus().isCancellable()) {
            throw new InvalidStateException(
                "취소 불가능한 상태입니다: " + order.getStatus()
            );
        }
        order.cancel(reason, requester);
        var saved = orderRepository.save(order);

        // 재고 해제
        order.getItems().forEach(item ->
            inventoryService.releaseReservation(item.getProductId(), item.getQuantity(), order.getOrderNumber())
        );
        // 쿠폰 사용 취소
        if (order.getCouponId() != null) {
            couponService.cancelUsage(order.getCouponId(), order.getOrderNumber());
        }

        eventPublisher.publishEvent(new OrderCancelledEvent(
            orderId, order.getOrderNumber(), order.getUserId(), reason, LocalDateTime.now()
        ));
        log.info("주문 취소 완료: id={}, reason={}", orderId, reason);
        return saved;
    }

    @Override
    @Transactional
    public Order requestReturn(Long orderId, String reason, List<Long> orderItemIds) {
        var order = load(orderId);
        if (order.getStatus() != OrderStatus.DELIVERED)
            throw new InvalidStateException("배송 완료된 주문만 반품 가능합니다.");
        order.requestReturn(reason, orderItemIds);
        var saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderReturnRequestedEvent(
            orderId, order.getOrderNumber(), reason, orderItemIds, LocalDateTime.now()
        ));
        return saved;
    }

    @Override
    @Transactional
    public Order approveReturn(Long orderId, String approver) {
        var order = load(orderId);
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED)
            throw new InvalidStateException("반품 신청 상태가 아닙니다.");
        order.approveReturn(approver);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order rejectReturn(Long orderId, String reason, String approver) {
        var order = load(orderId);
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED)
            throw new InvalidStateException("반품 신청 상태가 아닙니다.");
        order.rejectReturn(reason, approver);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order markShipped(Long orderId, String trackingNumber, String carrier) {
        var order = load(orderId);
        order.markShipped(trackingNumber, carrier);
        var saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderShippedEvent(
            orderId, order.getOrderNumber(), order.getUserId(), trackingNumber, carrier, LocalDateTime.now()
        ));
        return saved;
    }

    @Override
    @Transactional
    public Order markDelivered(Long orderId) {
        var order = load(orderId);
        order.markDelivered();
        var saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderDeliveredEvent(
            orderId, order.getOrderNumber(), order.getUserId(), LocalDateTime.now()
        ));
        return saved;
    }

    @Override
    @Transactional
    public Order confirm(Long orderId) {
        var order = load(orderId);
        order.confirm();
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order changeStatus(Long orderId, OrderStatus newStatus, String reason) {
        var order = load(orderId);
        order.changeStatus(newStatus, reason);
        return orderRepository.save(order);
    }

    @Override
    public Order findById(Long orderId) {
        return load(orderId);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    @Override
    public Page<Order> findByUserId(Long userId, Pageable pageable) {
        return pageOf(orderRepository.findAllByUserId(userId), pageable);
    }

    @Override
    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        return pageOf(orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == status).toList(), pageable);
    }

    @Override
    public Page<Order> search(OrderSearchCondition c, Pageable pageable) {
        var hits = orderRepository.findAll().stream()
            .filter(o -> c.userId() == null || c.userId().equals(o.getUserId()))
            .filter(o -> c.status() == null || c.status() == o.getStatus())
            .filter(o -> c.from() == null || (o.getCreatedAt() != null && !o.getCreatedAt().isBefore(c.from())))
            .filter(o -> c.to() == null || (o.getCreatedAt() != null && !o.getCreatedAt().isAfter(c.to())))
            .filter(o -> c.orderNumberContains() == null
                || (o.getOrderNumber() != null && o.getOrderNumber().contains(c.orderNumberContains())))
            .toList();
        return pageOf(hits, pageable);
    }

    @Override
    public List<Order> findPendingByUser(Long userId) {
        return orderRepository.findAllByUserId(userId).stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT
                || o.getStatus() == OrderStatus.PAYMENT_FAILED)
            .toList();
    }

    @Override
    public List<Order> findUnpaidOrdersOlderThan(LocalDateTime threshold) {
        return orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT)
            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isBefore(threshold))
            .toList();
    }

    @Override
    public OrderTotals computeTotals(LocalDateTime from, LocalDateTime to) {
        var orders = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .filter(o -> from == null || (o.getCreatedAt() != null && !o.getCreatedAt().isBefore(from)))
            .filter(o -> to == null || (o.getCreatedAt() != null && !o.getCreatedAt().isAfter(to)))
            .toList();
        var gross = orders.stream().map(Order::getSubtotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        var discount = orders.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        var net = orders.stream().map(Order::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderTotals(orders.size(), gross, discount, net);
    }

    @Override
    public UserOrderTotals computeUserTotals(Long userId) {
        var orders = orderRepository.findAllByUserId(userId).stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .toList();
        var spent = orders.stream().map(Order::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        var avg = orders.isEmpty() ? BigDecimal.ZERO
            : spent.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
        return new UserOrderTotals(userId, orders.size(), spent, avg);
    }

    // ---- helpers ----

    private Order load(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다: " + id));
    }

    private void validate(PlaceOrderCommand c) {
        if (c.userId() == null) throw new InvalidStateException("사용자는 필수");
        if (c.addressId() == null) throw new InvalidStateException("배송지는 필수");
        if (c.items() == null || c.items().isEmpty()) throw new InvalidStateException("주문 항목은 최소 1개");
        c.items().forEach(i -> {
            if (i.productId() == null) throw new InvalidStateException("상품 ID 누락");
            if (i.quantity() <= 0) throw new InvalidStateException("수량은 1 이상");
        });
    }

    private List<LineDetail> resolveLineItems(List<PlaceOrderCommand.LineItem> items) {
        var details = new ArrayList<LineDetail>();
        for (var item : items) {
            var product = productRepository.findById(item.productId())
                .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다: " + item.productId()));
            if (product.getStatus() != ProductStatus.ON_SALE)
                throw new InvalidStateException("판매중이 아닌 상품입니다: " + product.getName());

            var unitPrice = product.getEffectivePrice();
            // 클라이언트가 예상한 가격과 5% 이상 차이 나면 거부 (가격 변동 보호)
            if (item.expectedUnitPrice() != null) {
                var diff = unitPrice.subtract(item.expectedUnitPrice()).abs();
                var tolerance = item.expectedUnitPrice().multiply(BigDecimal.valueOf(0.05));
                if (diff.compareTo(tolerance) > 0) {
                    throw new InvalidStateException(
                        "상품 가격이 변경되었습니다. 다시 확인해 주세요: " + product.getName()
                    );
                }
            }
            details.add(new LineDetail(product, item.quantity(), unitPrice));
        }
        return details;
    }

    private Page<Order> pageOf(List<Order> all, Pageable pageable) {
        var sorted = all.stream()
            .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
            .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        var content = start >= sorted.size() ? List.<Order>of() : sorted.subList(start, end);
        return new PageImpl<>(content, pageable, sorted.size());
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDateTime.now().toString().replaceAll("[^0-9]", "").substring(0, 14)
            + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private record LineDetail(Product product, long quantity, BigDecimal unitPrice) {
        BigDecimal lineAmount() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
