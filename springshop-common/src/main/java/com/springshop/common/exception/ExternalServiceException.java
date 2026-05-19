package com.springshop.common.exception;

import java.io.Serial;

/**
 * 외부 서비스 연동 오류 (PG, 배송, 알림톡 등).
 */
public class ExternalServiceException extends SpringShopException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String serviceName;
    private final String originalMessage;
    private final Integer statusCode;
    private final String endpoint;

    public ExternalServiceException(String serviceName, String originalMessage) {
        super(ErrorCode.SYSTEM_EXTERNAL_SERVICE_ERROR, originalMessage);
        this.serviceName = serviceName;
        this.originalMessage = originalMessage;
        this.statusCode = null;
        this.endpoint = null;
    }

    public ExternalServiceException(String serviceName, String originalMessage, Throwable cause) {
        super(ErrorCode.SYSTEM_EXTERNAL_SERVICE_ERROR, new Object[]{originalMessage}, cause);
        this.serviceName = serviceName;
        this.originalMessage = originalMessage;
        this.statusCode = null;
        this.endpoint = null;
    }

    public ExternalServiceException(String serviceName, String endpoint,
                                    Integer statusCode, String originalMessage) {
        super(ErrorCode.SYSTEM_EXTERNAL_SERVICE_ERROR, originalMessage);
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.originalMessage = originalMessage;
    }

    public ExternalServiceException(String serviceName, String endpoint,
                                    Integer statusCode, String originalMessage,
                                    Throwable cause) {
        super(ErrorCode.SYSTEM_EXTERNAL_SERVICE_ERROR, new Object[]{originalMessage}, cause);
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.originalMessage = originalMessage;
    }

    public static ExternalServiceException pg(String message) {
        return new ExternalServiceException("PG", message);
    }

    public static ExternalServiceException pg(String message, Throwable cause) {
        return new ExternalServiceException("PG", message, cause);
    }

    public static ExternalServiceException shipping(String message) {
        return new ExternalServiceException("SHIPPING", message);
    }

    public static ExternalServiceException sms(String message) {
        return new ExternalServiceException("SMS", message);
    }

    public static ExternalServiceException email(String message) {
        return new ExternalServiceException("EMAIL", message);
    }

    public static ExternalServiceException push(String message) {
        return new ExternalServiceException("PUSH", message);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isRetryable() {
        if (statusCode == null) return true;
        return statusCode >= 500 || statusCode == 408 || statusCode == 429;
    }

    public String describeFailure() {
        return "ExternalService[name=%s, endpoint=%s, status=%s, message=%s]"
                .formatted(serviceName, endpoint, statusCode, originalMessage);
    }
}
