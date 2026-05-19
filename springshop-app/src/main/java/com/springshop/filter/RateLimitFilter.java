package com.springshop.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Rate Limiting 필터.
 *
 * <p>Redis Sliding Window 알고리즘으로 IP별/사용자별 요청 수를 제한한다.
 * 분당 {@code RATE_LIMIT_PER_MINUTE}건을 초과하면 HTTP 429를 반환한다.
 *
 * <p>헬스 체크, Swagger, H2 콘솔 경로는 Rate Limit을 적용하지 않는다.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int RATE_LIMIT_PER_MINUTE = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RESET_HEADER = "X-RateLimit-Reset";

    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/actuator/health", "/actuator/info",
            "/swagger-ui", "/api-docs", "/h2-console"
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        RateLimitResult result = checkRateLimit(clientKey);

        response.setHeader(LIMIT_HEADER, String.valueOf(RATE_LIMIT_PER_MINUTE));
        response.setHeader(REMAINING_HEADER, String.valueOf(result.remaining()));
        response.setHeader(RESET_HEADER, String.valueOf(result.resetEpochSeconds()));

        if (!result.allowed()) {
            log.warn("Rate limit 초과 - client={}, path={}", clientKey, path);
            rejectRequest(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Redis INCR + EXPIRE로 슬라이딩 윈도우 카운터를 관리한다.
     */
    private RateLimitResult checkRateLimit(String clientKey) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + clientKey;
        long resetEpoch = System.currentTimeMillis() / 1000 + WINDOW.toSeconds();

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count == null) count = 1L;

            if (count == 1) {
                redisTemplate.expire(redisKey, WINDOW);
            }

            long remaining = Math.max(0, RATE_LIMIT_PER_MINUTE - count);
            boolean allowed = count <= RATE_LIMIT_PER_MINUTE;
            return new RateLimitResult(allowed, (int) remaining, resetEpoch);
        } catch (Exception ex) {
            log.error("Rate limit Redis 오류 - 요청 허용 처리: {}", ex.getMessage());
            return new RateLimitResult(true, RATE_LIMIT_PER_MINUTE, resetEpoch);
        }
    }

    /**
     * 클라이언트 식별 키 — 인증된 경우 userId, 아닌 경우 IP를 사용한다.
     */
    private String resolveClientKey(HttpServletRequest request) {
        String userId = request.getHeader("X-User-ID");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void rejectRequest(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"errorCode":"RATE_LIMIT_EXCEEDED","message":"요청 한도를 초과했습니다. 잠시 후 다시 시도하세요."}
                """.strip());
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Rate limit 확인 결과.
     *
     * @param allowed            요청 허용 여부
     * @param remaining          남은 요청 수
     * @param resetEpochSeconds  카운터 초기화 예정 Unix 시각
     */
    private record RateLimitResult(boolean allowed, int remaining, long resetEpochSeconds) {}
}
