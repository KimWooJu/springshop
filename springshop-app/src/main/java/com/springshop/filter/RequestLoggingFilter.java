package com.springshop.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * HTTP 요청/응답 로깅 필터.
 *
 * <p>{@link ContentCachingRequestWrapper}로 요청 본문을 캐싱하여,
 * 컨트롤러 처리 후 응답 상태와 소요 시간을 함께 로그에 기록한다.
 * MDC에 correlationId, method, path를 설정하여 분산 추적을 지원한다.
 *
 * <p>민감 경로(Swagger, H2 콘솔, Actuator)는 상세 로그를 생략한다.
 */
@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LOG_SIZE = 1000;

    private static final List<String> SKIP_BODY_PATHS = Arrays.asList(
            "/swagger-ui", "/api-docs", "/h2-console", "/actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var cachedRequest = new ContentCachingRequestWrapper(request);
        var cachedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();

        MDC.put("method", method);
        MDC.put("path", path);

        try {
            logRequest(method, path, queryString, cachedRequest);
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = cachedResponse.getStatus();
            logResponse(method, path, status, duration);
            cachedResponse.copyBodyToResponse();
            MDC.remove("method");
            MDC.remove("path");
        }
    }

    private void logRequest(String method, String path, String queryString,
                            ContentCachingRequestWrapper request) {
        if (shouldSkipBodyLog(path)) {
            log.debug(">>> {} {} {}", method, path, queryString != null ? "?" + queryString : "");
            return;
        }

        String fullPath = queryString != null ? path + "?" + queryString : path;
        log.info(">>> {} {}", method, fullPath);
    }

    private void logResponse(String method, String path, int status, long durationMs) {
        if (status >= 500) {
            log.error("<<< {} {} {} {}ms", method, path, status, durationMs);
        } else if (status >= 400) {
            log.warn("<<< {} {} {} {}ms", method, path, status, durationMs);
        } else if (durationMs > 2000) {
            log.warn("<<< {} {} {} {}ms [SLOW]", method, path, status, durationMs);
        } else {
            log.info("<<< {} {} {} {}ms", method, path, status, durationMs);
        }
    }

    private String truncateBody(byte[] body) {
        if (body == null || body.length == 0) return "";
        String content = new String(body, StandardCharsets.UTF_8);
        return content.length() > MAX_BODY_LOG_SIZE
                ? content.substring(0, MAX_BODY_LOG_SIZE) + "...[truncated]"
                : content;
    }

    private boolean shouldSkipBodyLog(String path) {
        return SKIP_BODY_PATHS.stream().anyMatch(path::startsWith);
    }
}
