package com.springshop.domain.payment;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 결제 PG 통신 로그 엔티티.
 *
 * <p>각 결제 시도마다 PG로 보낸 요청과 받은 응답을 그대로 저장한다. 장애 분석/CS 대응을
 * 위한 추적용으로 활용되며, 민감 정보(카드 전체 번호 등)는 저장 전에 마스킹된다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_pay_tx_payment", columnList = "payment_id"),
                @Index(name = "idx_pay_tx_pg", columnList = "pg_name"),
                @Index(name = "idx_pay_tx_success", columnList = "is_success")
        }
)
public class PaymentTransaction extends BaseEntity {

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "pg_name", length = 50, nullable = false)
    private String pgName;

    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "is_success", nullable = false)
    private boolean isSuccess;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "endpoint_url", length = 300)
    private String endpointUrl;

    @Column(name = "request_at", nullable = false)
    private LocalDateTime requestAt;

    @Column(name = "response_at")
    private LocalDateTime responseAt;

    protected PaymentTransaction() {
        super();
    }

    private PaymentTransaction(Long paymentId, String pgName) {
        super();
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId 필수");
        this.pgName = Objects.requireNonNull(pgName, "PG 이름 필수");
        this.requestAt = LocalDateTime.now();
        this.isSuccess = false;
    }

    /**
     * 성공한 트랜잭션 기록.
     */
    public static PaymentTransaction success(Long paymentId,
                                             String pgName,
                                             String requestData,
                                             String responseData,
                                             long processingTimeMs) {
        PaymentTransaction tx = new PaymentTransaction(paymentId, pgName);
        tx.requestData = requestData;
        tx.responseData = responseData;
        tx.isSuccess = true;
        tx.processingTimeMs = processingTimeMs;
        tx.httpStatusCode = 200;
        tx.responseAt = LocalDateTime.now();
        return tx;
    }

    /**
     * 실패한 트랜잭션 기록.
     */
    public static PaymentTransaction failure(Long paymentId,
                                             String pgName,
                                             String requestData,
                                             String responseData,
                                             String errorCode,
                                             String errorMessage,
                                             long processingTimeMs,
                                             int httpStatusCode) {
        PaymentTransaction tx = new PaymentTransaction(paymentId, pgName);
        tx.requestData = requestData;
        tx.responseData = responseData;
        tx.errorCode = errorCode;
        tx.errorMessage = errorMessage;
        tx.processingTimeMs = processingTimeMs;
        tx.httpStatusCode = httpStatusCode;
        tx.isSuccess = false;
        tx.responseAt = LocalDateTime.now();
        return tx;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getPgName() {
        return pgName;
    }

    public String getRequestData() {
        return requestData;
    }

    public String getResponseData() {
        return responseData;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public LocalDateTime getRequestAt() {
        return requestAt;
    }

    public LocalDateTime getResponseAt() {
        return responseAt;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    /**
     * 응답 데이터 보완 (비동기 콜백 등).
     */
    public void appendResponse(String additionalData) {
        if (this.responseData == null) {
            this.responseData = additionalData;
        } else {
            this.responseData = this.responseData + "\n---\n" + Objects.requireNonNullElse(additionalData, "");
        }
        this.responseAt = LocalDateTime.now();
    }

    /**
     * 처리 시간이 임계치를 초과한 슬로우 트랜잭션 판정.
     */
    public boolean isSlow(long thresholdMs) {
        return processingTimeMs != null && processingTimeMs > thresholdMs;
    }

    @Override
    public String toString() {
        return "PaymentTransaction[id=%s, paymentId=%s, pg=%s, success=%s, processing=%sms]"
                .formatted(getId(), paymentId, pgName, isSuccess, processingTimeMs);
    }
}
