package com.springshop.service.payment;

import com.springshop.common.exception.BusinessException;
import com.springshop.common.exception.ErrorCode;
import com.springshop.common.exception.PaymentException;
import com.springshop.common.exception.ResourceNotFoundException;
import com.springshop.domain.payment.Payment;
import com.springshop.domain.payment.PaymentEvents;
import com.springshop.domain.payment.PaymentRepository;
import com.springshop.domain.payment.PaymentStatus;
import com.springshop.domain.payment.Refund;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 환불 서비스 구현체.
 *
 * <p>환불 가능 금액 검증 → Refund 도메인 생성 → PG 환불 API 호출 → 결제 객체에 환불 금액 반영
 * 의 흐름으로 동작한다. 부분 환불을 지원하며, 모든 환불 시도는 추적을 위해 메모리 저장소에
 * 보관된다. 운영 환경에서는 별도 RefundRepository 로 대체되어야 한다.</p>
 */
@Service
@Transactional(readOnly = true)
public class RefundServiceImpl implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayAdapter gatewayAdapter;
    private final ApplicationEventPublisher eventPublisher;

    /** 메모리 저장소 — 실제로는 RefundRepository(JpaRepository) 로 대체된다. */
    private final Map<Long, Refund> refundStore = new ConcurrentHashMap<>();
    private final AtomicLong refundIdSequence = new AtomicLong(1L);

    @Autowired
    public RefundServiceImpl(PaymentRepository paymentRepository,
                             PaymentGatewayAdapter gatewayAdapter,
                             ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.gatewayAdapter = Objects.requireNonNull(gatewayAdapter);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund requestRefund(RefundRequest request) {
        Objects.requireNonNull(request, "환불 요청 정보 필수");

        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> ResourceNotFoundException.payment(request.paymentId()));

        // 결제 상태 검증
        if (!(payment.getStatus() instanceof PaymentStatus.Completed
                || payment.getStatus() instanceof PaymentStatus.PartiallyRefunded)) {
            log.warn("환불 불가 상태: paymentId={}, status={}",
                    payment.getId(), payment.getStatus().label());
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_REFUNDED,
                    "환불 가능한 상태가 아닙니다: " + payment.getStatus().label());
        }

        // 환불 가능 금액 검증
        BigDecimal remaining = payment.getRemainingRefundableAmount();
        if (request.amount().compareTo(remaining) > 0) {
            log.warn("환불 가능 금액 초과: 요청={}, 가능={}", request.amount(), remaining);
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_FAILED,
                    "환불 가능 금액 초과 (가능: " + remaining + ")");
        }

        Refund refund = Refund.request(
                payment.getId(),
                payment.getOrderId(),
                request.amount(),
                request.reason(),
                request.refundMethod()
        );
        // 메모리 저장소에 ID 부여
        Long refundId = refundIdSequence.getAndIncrement();
        assignId(refund, refundId);
        refundStore.put(refundId, refund);

        // 도메인 이벤트 (요청)
        var requestedEvent = PaymentEvents.RefundRequestedEvent.of(
                payment.getId(), payment.getOrderId(), request.amount(), request.reason());
        eventPublisher.publishEvent(requestedEvent);

        // PG 환불 호출
        try {
            refund.startProcessing();
            PaymentGatewayAdapter.RefundResult result = gatewayAdapter.refund(payment, request.amount());

            if (result.success()) {
                refund.complete(result.pgRefundId());
                payment.applyRefund(request.amount());
                paymentRepository.save(payment);

                var completedEvent = PaymentEvents.RefundCompletedEvent.of(
                        payment.getId(),
                        payment.getOrderId(),
                        request.amount()
                );
                eventPublisher.publishEvent(completedEvent);

                log.info("환불 완료: refundId={}, paymentId={}, amount={}, pgRefundId={}",
                        refundId, payment.getId(), request.amount(), result.pgRefundId());
            } else {
                refund.fail(result.errorMessage());
                log.warn("환불 실패: refundId={}, reason={}", refundId, result.errorMessage());
                throw PaymentException.pgError("REFUND_FAILED", result.errorMessage());
            }
        } catch (PaymentException pe) {
            throw pe;
        } catch (RuntimeException e) {
            refund.fail(e.getMessage());
            log.error("환불 처리 중 예외: refundId={}, {}", refundId, e.getMessage(), e);
            throw PaymentException.pgError("REFUND_EXCEPTION", e.getMessage());
        }

        return refund;
    }

    @Override
    public Refund getRefund(Long refundId) {
        Refund refund = refundStore.get(refundId);
        if (refund == null) {
            throw new ResourceNotFoundException("Refund", refundId);
        }
        return refund;
    }

    @Override
    public List<Refund> getRefundsByPaymentId(Long paymentId) {
        if (paymentId == null) return List.of();
        List<Refund> list = new ArrayList<>();
        for (Refund r : refundStore.values()) {
            if (Objects.equals(r.getPaymentId(), paymentId)) {
                list.add(r);
            }
        }
        list.sort(Comparator.comparing(Refund::getRequestedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    @Override
    public BigDecimal getRefundableAmount(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.payment(paymentId));
        if (!payment.isRefundable()) {
            return BigDecimal.ZERO;
        }
        return payment.getRemainingRefundableAmount();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund processRefund(Long refundId) {
        Refund refund = getRefund(refundId);
        if (refund.isFinal()) {
            log.info("이미 종결된 환불: refundId={}, status={}", refundId, refund.getStatus());
            return refund;
        }
        Payment payment = paymentRepository.findById(refund.getPaymentId())
                .orElseThrow(() -> ResourceNotFoundException.payment(refund.getPaymentId()));

        PaymentGatewayAdapter.RefundResult result = gatewayAdapter.refund(payment, refund.getAmount());
        if (result.success()) {
            refund.complete(result.pgRefundId());
            payment.applyRefund(refund.getAmount());
            paymentRepository.save(payment);
        } else {
            refund.fail(result.errorMessage());
        }
        return refund;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Refund failRefund(Long refundId, String reason) {
        Refund refund = getRefund(refundId);
        if (refund.isCompleted()) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_FAILED, "이미 완료된 환불입니다");
        }
        refund.fail(reason);
        log.info("환불 실패 처리: refundId={}, reason={}", refundId, reason);
        return refund;
    }

    /**
     * 메모리 저장소용으로 ID를 강제 부여한다. 실제 영속화에서는 JPA가 자동 할당한다.
     */
    private void assignId(Refund refund, Long id) {
        // 리플렉션을 통한 ID 부여 — 운영에서는 사용 금지. 데모용.
        try {
            var field = findIdField(refund.getClass());
            if (field != null) {
                field.setAccessible(true);
                field.set(refund, id);
            }
        } catch (ReflectiveOperationException e) {
            log.debug("Refund ID 부여 실패 (영속화 시 자동 할당): {}", e.getMessage());
        }
    }

    private java.lang.reflect.Field findIdField(Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            for (var field : clazz.getDeclaredFields()) {
                if ("id".equals(field.getName())) {
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
