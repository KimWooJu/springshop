package com.springshop.service.payment;

import com.springshop.common.exception.BusinessException;
import com.springshop.common.exception.ErrorCode;
import com.springshop.common.exception.PaymentException;
import com.springshop.common.exception.ResourceNotFoundException;
import com.springshop.domain.payment.Payment;
import com.springshop.domain.payment.PaymentEvents;
import com.springshop.domain.payment.PaymentMethod;
import com.springshop.domain.payment.PaymentRepository;
import com.springshop.domain.payment.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 결제 서비스 구현체.
 *
 * <p>PG 통신은 비동기 처리하며 30 초 타임아웃을 둔다. 결제 진행 상태는 메모리 캐시로
 * 단순 중복 방지를 제공하지만, 운영 환경에서는 Redis 분산 락으로 대체해야 한다.</p>
 *
 * <p>결제 완료/실패 시 즉시 도메인 이벤트를 발행하여 후속 처리(주문 확정/취소,
 * 영수증 발송, 알림)가 비동기로 진행되도록 한다.</p>
 */
@Service
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    /** PG 응답 타임아웃 (초). */
    private static final long PG_TIMEOUT_SECONDS = 30L;

    /** 보류 상태 만료 기준 (분). */
    private static final long PENDING_EXPIRY_MINUTES = 30L;

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayAdapter gatewayAdapter;
    private final ApplicationEventPublisher eventPublisher;

    /** 중복 결제 방지용 진행 중 결제 키 집합. */
    private final ConcurrentHashMap<String, Long> inflightRequests = new ConcurrentHashMap<>();

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentGatewayAdapter gatewayAdapter,
                              ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository);
        this.gatewayAdapter = Objects.requireNonNull(gatewayAdapter);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment requestPayment(PaymentRequest request) {
        Objects.requireNonNull(request, "결제 요청 정보 필수");
        String idempotencyKey = resolveIdempotencyKey(request);

        // 중복 결제 방지 — 짧은 윈도우 동안 동일 키 차단
        Long previous = inflightRequests.putIfAbsent(idempotencyKey, request.orderId());
        if (previous != null) {
            log.warn("중복 결제 요청 차단: idempotencyKey={}, orderId={}", idempotencyKey, request.orderId());
            throw new BusinessException(ErrorCode.PAYMENT_PG_ERROR, "중복 결제 요청입니다");
        }

        try {
            // requestId 중복 시 기존 결제 반환 (idempotency)
            Optional<Payment> existing = paymentRepository.findByRequestId(idempotencyKey);
            if (existing.isPresent()) {
                log.info("기존 결제 요청 재사용: requestId={}, paymentId={}",
                        idempotencyKey, existing.get().getId());
                return existing.get();
            }

            Payment payment = Payment.request(
                    request.orderId(),
                    request.userId(),
                    request.amount(),
                    request.currency(),
                    request.method()
            );

            Payment saved = paymentRepository.save(payment);
            log.info("결제 요청 생성: paymentId={}, orderId={}, amount={}",
                    saved.getId(), request.orderId(), request.amount());

            // PG 승인 호출 (타임아웃 처리)
            PaymentGatewayAdapter.PaymentApprovalResult result = invokePgWithTimeout(saved, request.method());

            if (result.success()) {
                return doComplete(saved, result.pgTransactionId(), request.method());
            } else {
                return doFail(saved, result.errorMessage());
            }
        } finally {
            inflightRequests.remove(idempotencyKey);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment completePayment(Long paymentId, String pgTransactionId, PaymentMethod method) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.payment(paymentId));
        return doComplete(payment, pgTransactionId, method);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment failPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.payment(paymentId));
        return doFail(payment, reason);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment cancelPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.payment(paymentId));

        // 완료 상태에서 취소는 환불 처리로 위임
        if (payment.getStatus() instanceof PaymentStatus.Completed) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_REFUNDED,
                    "완료된 결제는 RefundService를 이용해야 합니다");
        }

        try {
            payment.cancel(reason);
        } catch (IllegalStateException e) {
            throw PaymentException.pgError("CANCEL_FAILED", e.getMessage());
        }

        Payment saved = paymentRepository.save(payment);
        log.info("결제 취소: paymentId={}, reason={}", paymentId, reason);
        return saved;
    }

    @Override
    public Optional<Payment> findById(Long paymentId) {
        if (paymentId == null) return Optional.empty();
        return paymentRepository.findById(paymentId);
    }

    @Override
    public Optional<Payment> findByRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) return Optional.empty();
        return paymentRepository.findByRequestId(requestId);
    }

    @Override
    public Optional<Payment> findLatestByOrderId(Long orderId) {
        if (orderId == null) return Optional.empty();
        return paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Override
    public List<Payment> findUserPayments(Long userId, int limit) {
        if (userId == null) return List.of();
        var page = paymentRepository.findByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)));
        return page.getContent();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int expirePendingPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_EXPIRY_MINUTES);
        List<Payment> expired = paymentRepository.findExpiredPending(threshold);
        if (expired.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Payment payment : expired) {
            try {
                payment.fail("자동 만료 (보류 상태 " + PENDING_EXPIRY_MINUTES + "분 초과)");
                paymentRepository.save(payment);
                publishFailureEvent(payment, "자동 만료");
                count++;
            } catch (Exception e) {
                log.warn("결제 만료 처리 실패: paymentId={}, {}", payment.getId(), e.getMessage());
            }
        }
        log.info("결제 만료 일괄 처리 완료: {} 건", count);
        return count;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private Payment doComplete(Payment payment, String pgTransactionId, PaymentMethod method) {
        if (payment.getStatus() instanceof PaymentStatus.Completed) {
            log.info("이미 완료된 결제: paymentId={}", payment.getId());
            return payment;
        }
        try {
            payment.complete(pgTransactionId, method);
        } catch (IllegalStateException e) {
            throw PaymentException.pgError("COMPLETE_FAILED", e.getMessage());
        }
        Payment saved = paymentRepository.save(payment);

        // 이벤트 발행
        var event = PaymentEvents.PaymentCompletedEvent.of(
                saved.getId(), saved.getOrderId(), saved.getUserId(), saved.getAmount(), method);
        eventPublisher.publishEvent(event);
        log.info("결제 완료: paymentId={}, pgTxId={}, amount={}",
                saved.getId(), pgTransactionId, saved.getAmount());
        return saved;
    }

    private Payment doFail(Payment payment, String reason) {
        if (payment.getStatus() instanceof PaymentStatus.Failed) {
            return payment;
        }
        try {
            payment.fail(reason);
        } catch (IllegalStateException e) {
            log.warn("결제 실패 전이 불가: paymentId={}, {}", payment.getId(), e.getMessage());
        }
        Payment saved = paymentRepository.save(payment);
        publishFailureEvent(saved, reason);
        log.info("결제 실패: paymentId={}, reason={}", saved.getId(), reason);
        return saved;
    }

    private void publishFailureEvent(Payment payment, String reason) {
        var event = PaymentEvents.PaymentFailedEvent.of(
                payment.getId(), payment.getOrderId(), payment.getUserId(), reason);
        eventPublisher.publishEvent(event);
    }

    /**
     * Virtual Thread Executor 로 PG 호출 타임아웃을 안전하게 제어한다.
     */
    private PaymentGatewayAdapter.PaymentApprovalResult invokePgWithTimeout(Payment payment, PaymentMethod method) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = executor.submit(() -> gatewayAdapter.approve(payment, method));
            try {
                return future.get(PG_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("PG 응답 타임아웃: paymentId={}", payment.getId());
                return new PaymentGatewayAdapter.PaymentApprovalResult(false, null, "TIMEOUT", "PG 응답 시간 초과");
            } catch (ExecutionException e) {
                if (e.getCause() instanceof PaymentException pe) throw pe;
                return new PaymentGatewayAdapter.PaymentApprovalResult(false, null, "PG_ERROR", e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new PaymentGatewayAdapter.PaymentApprovalResult(false, null, "INTERRUPTED", "결제 처리 인터럽트");
            }
        }
    }

    private String resolveIdempotencyKey(PaymentRequest request) {
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return request.idempotencyKey();
        }
        return "order-" + request.orderId() + "-amt-" + request.amount();
    }
}
