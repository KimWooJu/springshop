package com.springshop.common.exception;

import java.io.Serial;
import java.time.Duration;

/**
 * 요청 횟수 초과 (429 Too Many Requests) 예외.
 */
public class RateLimitException extends SpringShopException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String clientId;
    private final int limit;
    private final int currentCount;
    private final Duration retryAfter;

    public RateLimitException() {
        super(ErrorCode.SYSTEM_RATE_LIMIT_EXCEEDED);
        this.clientId = null;
        this.limit = 0;
        this.currentCount = 0;
        this.retryAfter = Duration.ofMinutes(1);
    }

    public RateLimitException(String clientId, int limit, int currentCount, Duration retryAfter) {
        super(ErrorCode.SYSTEM_RATE_LIMIT_EXCEEDED);
        this.clientId = clientId;
        this.limit = limit;
        this.currentCount = currentCount;
        this.retryAfter = retryAfter != null ? retryAfter : Duration.ofMinutes(1);
    }

    public RateLimitException(String message, Duration retryAfter) {
        super(ErrorCode.SYSTEM_RATE_LIMIT_EXCEEDED, message);
        this.clientId = null;
        this.limit = 0;
        this.currentCount = 0;
        this.retryAfter = retryAfter != null ? retryAfter : Duration.ofMinutes(1);
    }

    public static RateLimitException forIp(String ip, int limit, int current) {
        return new RateLimitException(ip, limit, current, Duration.ofMinutes(1));
    }

    public static RateLimitException forUser(String userId, int limit, int current) {
        return new RateLimitException("user:" + userId, limit, current, Duration.ofMinutes(1));
    }

    public static RateLimitException forApiKey(String apiKey, int limit, int current) {
        return new RateLimitException("apikey:" + apiKey, limit, current, Duration.ofSeconds(60));
    }

    public String getClientId() {
        return clientId;
    }

    public int getLimit() {
        return limit;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    public long getRetryAfterSeconds() {
        return retryAfter.getSeconds();
    }

    public int getOverflowCount() {
        return Math.max(0, currentCount - limit);
    }

    public String toHeaderValue() {
        return String.valueOf(getRetryAfterSeconds());
    }
}
