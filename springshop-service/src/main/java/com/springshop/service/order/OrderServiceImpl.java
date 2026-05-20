package com.springshop.service.order;

import com.springshop.domain.order.Order;
import com.springshop.domain.order.OrderItem;
import com.springshop.domain.order.OrderRepository;
import com.springshop.domain.order.OrderStatus;
import com.springshop.domain.product.Product;
import com.springshop.domain.product.ProductRepository;
import com.springshop.domain.product.ProductStatus;
import com.springshop.common.exception.InvalidStateException;
import com.springshop.common.exception.ResourceNotFoundException;
import com.springshop.service.coupon.CouponService;
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
import java.util.stream.Collectors;

/**
 * {@link OrderService} 기본 구현.
 *
 * <p>주문 생성 시 다음 흐름을 보장한다:
 * <ol>
 *   <li>입력 유효성 검증 (수량/사용자)</li>
 *   <li>상품 조회 + 가격 정합성 검사</li>
 *   <li>재고 예약 ({@link InventoryService#reserve})</li>
 *   <li>쿠폰 적용/할인 계산</li>
 *   <li>주문 엔티티 영속화</li>
 *   <li>도메인 이벤트 발행</li>
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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Order placeOrder(PlaceOrderCommand command) {
        log.info("주문 생성 시도: userId={}, items={}", command.userId(), command.items().size());
        validate(command);

        var lineItems = resolveLineItems(command.items());

        // 재고 예약
        for (var item : lineItems) {
            inventoryService.reserve(item.product().getId(), null, (int) item.quantity());
        }

        // 합계 계산
        var subtotal = lineItems.stream()
            .map(LineDetail::lineAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 주문 생성
        var order = Order.create(command.userId());

        // 쿠폰/할인 적용
        if (command.couponId() != null) {
            try {
                var couponDto = couponService.getCoupon(command.couponId());
                var result = couponService.applyCoupon(couponDto.code(), command.userId(), subtotal);
                order.applyCoupon(couponDto.code(), result.discountAmount());
            } catch (Exception e) {
                log.warn("쿠폰 적용 실패, 할인 없이 진행: couponId={}", command.couponId(), e);
            }
        }

        // 항목 추가
        lineItems.forEach(d ->
            order.addItem(OrderItem.of(
                order,
                d.product().getId(),
                null,
                d.product().getName(),
                null,
                (int) d.quantity(),
                d.unitPrice()
            ))
        );

        // 메모
        if (command.memo() != null) {
            order.updateMemo(command.memo());
        }

        // 결제 요청 → PAYMENT_PENDING 으로 전이 + OrderPlacedEvent 등록
        order.requestPayment();

        var saved = orderRepository.save(order);
        publishDomainEvents(saved);

        log.info("주문 생성 완료: id={}, orderNumber={}, final={}",
            saved.getId(), saved.getOrderNumber(), saved.getFinalAmount());
        return saved;
    }

    @Override
    @Transactional
    public Order cancel(Long orderId, String reason, String requester) {
        var order = load(orderId);
        if (!order.getStatus().isCancellable()) {
            throw new InvalidStateException(
                "취소 불가능한 상태입니다: " + order.getStatus().label()
            );
        }
        order.cancel(reason);
        var saved = orderRepository.save(order);

        // 재고 해제
        order.getItems().forEach(item ->
            inventoryService.release(item.getProductId(), item.getVariantId(), item.getQuantity())
        );

        publishDomainEvents(saved);
        log.info("주문 취소 완료: id={}, reason={}", orderId, reason);
        return saved;
    }

    @Override
    @Transactional
    public Order requestReturn(Long orderId, String reason, List<Long> orderItemIds) {
        var order = load(orderId);
        if (!(order.getStatus() instanceof OrderStatus.Delivered)) {
            throw new InvalidStateException("배송 완료된 주문만 반품 가능합니다.");
        }
        order.requestReturn(reason);
        var saved = orderRepository.save(order);
        publishDomainEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public Order approveReturn(Long orderId, String approver) {
        var order = load(orderId);
        if (!(order.getStatus() instanceof OrderStatus.ReturnRequested)) {
            throw new InvalidStateException("반품 신청 상태가 아닙니다.");
        }
        order.completeReturn();
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order rejectReturn(Long orderId, String reason, String approver) {
        var order = load(orderId);
        if (!(order.getStatus() instanceof OrderStatus.ReturnRequested)) {
            throw new InvalidStateException("반품 신청 상태가 아닙니다.");
        }
        // 반품 거절 — 배송 완료 상태로 복원 (DELIVERED)
        log.info("반품 거절: orderId={}, reason={}, approver={}", orderId, reason, approver);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order markShipped(Long orderId, String trackingNumber, String carrier) {
        var order = load(orderId);
        order.ship(trackingNumber, carrier);
        var saved = orderRepository.save(order);
        publishDomainEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public Order markDelivered(Long orderId) {
        var order = load(orderId);
        order.deliver();
        var saved = orderRepository.save(order);
        publishDomainEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public Order confirm(Long orderId) {
        var order = load(orderId);
        order.confirm("SYSTEM");
        var saved = orderRepository.save(order);
        publishDomainEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public Order changeStatus(Long orderId, OrderStatus newStatus, String reason) {
        var order = load(orderId);
        String label = newStatus.label();
        switch (label) {
            case "CONFIRMED" -> order.confirm("ADMIN");
            case "PROCESSING" -> order.process();
            case "CANCELLED" -> order.cancel(reason != null ? reason : "관리자 변경");
            case "RETURNED" -> order.completeReturn();
            default -> log.warn("지원하지 않는 상태 전이: {} → {}", order.getStatus().label(), label);
        }
        var saved = orderRepository.save(order);
        publishDomainEvents(saved);
        return saved;
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
        return pageOf(orderRepository.findByUserId(userId), pageable);
    }

    @Override
    public Page<Order> findByStatus(OrderStatus status, Pageable pageable) {
        String targetLabel = status.label();
        return pageOf(orderRepository.findAll().stream()
            .filter(o -> o.getStatus().label().equals(targetLabel))
            .toList(), pageable);
    }

    @Override
    public Page<Order> search(OrderSearchCondition c, Pageable pageable) {
        var hits = orderRepository.findAll().stream()
            .filter(o -> c.userId() == null || c.userId().equals(o.getUserId()))
            .filter(o -> c.status() == null || o.getStatus().label().equals(c.status().label()))
            .filter(o -> c.from() == null || (o.getCreatedAt() != null && !o.getCreatedAt().isBefore(c.from())))
            .filter(o -> c.to() == null || (o.getCreatedAt() != null && !o.getCreatedAt().isAfter(c.to())))
            .filter(o -> c.orderNumberContains() == null
                || (o.getOrderNumber() != null && o.getOrderNumber().contains(c.orderNumberContains())))
            .toList();
        return pageOf(hits, pageable);
    }

    @Override
    public List<Order> findPendingByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream()
            .filter(o -> o.getStatus() instanceof OrderStatus.PaymentPending
                || o.getStatus() instanceof OrderStatus.Pending)
            .toList();
    }

    @Override
    public List<Order> findUnpaidOrdersOlderThan(LocalDateTime threshold) {
        return orderRepository.findAll().stream()
            .filter(o -> o.getStatus() instanceof OrderStatus.PaymentPending)
            .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isBefore(threshold))
            .toList();
    }

    @Override
    public OrderTotals computeTotals(LocalDateTime from, LocalDateTime to) {
        var orders = orderRepository.findAll().stream()
            .filter(o -> !(o.getStatus() instanceof OrderStatus.Cancelled))
            .filter(o -> from == null || (o.getCreatedAt() != null && !o.getCreatedAt().isBefore(from)))
            .filter(o -> to == null || (o.getCreatedAt() != null && !o.getCreatedAt().isAfter(to)))
            .toList();
        var gross = orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        var discount = orders.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        var net = orders.stream().map(Order::getFinalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderTotals(orders.size(), gross, discount, net);
    }

    @Override
    public UserOrderTotals computeUserTotals(Long userId) {
        var orders = orderRepository.findByUserId(userId).stream()
            .filter(o -> !(o.getStatus() instanceof OrderStatus.Cancelled))
            .toList();
        var spent = orders.stream().map(Order::getFinalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        var avg = orders.isEmpty() ? BigDecimal.ZERO
            : spent.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
        return new UserOrderTotals(userId, orders.size(), spent, avg);
    }

    // ---- helpers ----

    private Order load(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private void validate(PlaceOrderCommand c) {
        if (c.userId() == null) throw new InvalidStateException("사용자는 필수");
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
                .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));
            if (!(product.getStatus() instanceof ProductStatus.Active))
                throw new InvalidStateException("판매중이 아닌 상품입니다: " + product.getName());

            var unitPrice = product.getBasePrice();
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
            .sorted(Comparator.comparing(Order::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sorted.size());
        var content = start >= sorted.size() ? List.<Order>of() : sorted.subList(start, end);
        return new PageImpl<>(content, pageable, sorted.size());
    }

    private void publishDomainEvents(Order order) {
        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearDomainEvents();
    }

    private record LineDetail(Product product, long quantity, BigDecimal unitPrice) {
        BigDecimal lineAmount() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
