package com.springshop.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 감사 로그 인터셉터.
 *
 * <p>POST, PUT, PATCH, DELETE 요청에 대해 사용자, 경로, 시각을 감사 로그로 기록한다.
 * 민감 경로(Swagger, 인증, 건강 체크)는 기록에서 제외한다.
 *
 * <p>감사 로그는 별도 로거 {@code AUDIT}를 통해 감사 파일로 분리 저장할 수 있다.
 */
@Component
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {

    private static final org.slf4j.Logger AUDIT_LOG =
            org.slf4j.LoggerFactory.getLogger("AUDIT");

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/h2-console"
    );

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (WRITE_METHODS.contains(method) && !isSkipped(path)) {
            String userId = MDC.get("userId");
            String correlationId = MDC.get("correlationId");
            String userEmail = MDC.get("userEmail");
            String clientIp = getClientIp(request);

            AUDIT_LOG.info("[AUDIT] method={} path={} userId={} email={} ip={} correlationId={}",
                    method, path, userId, userEmail, clientIp, correlationId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (WRITE_METHODS.contains(method) && !isSkipped(path)) {
            int status = response.getStatus();
            String userId = MDC.get("userId");

            if (status >= 400) {
                AUDIT_LOG.warn("[AUDIT_FAIL] method={} path={} userId={} status={}",
                        method, path, userId, status);
            } else {
                AUDIT_LOG.info("[AUDIT_OK] method={} path={} userId={} status={}",
                        method, path, userId, status);
            }
        }
    }

    private boolean isSkipped(String path) {
        return SKIP_PATHS.stream().anyMatch(path::startsWith);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
