package com.springshop.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 상관 ID(Correlation ID) 필터.
 *
 * <p>분산 시스템에서 요청 추적을 위해, 입력 헤더 {@code X-Correlation-ID}를
 * 그대로 사용하거나 없는 경우 UUID를 생성하여 MDC와 응답 헤더에 추가한다.
 *
 * <p>다른 모든 필터/인터셉터보다 먼저 실행되어야 하므로 {@code @Order(0)}으로 설정한다.
 */
@Component
@Order(0)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    /**
     * 요청 헤더에서 상관 ID를 추출하거나, 없으면 UUID를 새로 생성한다.
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        String headerValue = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.hasText(headerValue)) {
            // 보안상 길이 제한 (DoS 방어)
            return headerValue.length() > 64
                    ? headerValue.substring(0, 64)
                    : headerValue;
        }
        return UUID.randomUUID().toString();
    }
}
