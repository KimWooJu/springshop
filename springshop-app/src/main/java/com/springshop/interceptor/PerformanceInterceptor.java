package com.springshop.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API 성능 모니터링 인터셉터.
 *
 * <p>각 요청의 처리 시간을 측정하고, 슬로우 쿼리(2초 이상)를 경고 로그로 기록한다.
 * 누적 통계를 {@link ConcurrentHashMap}에 보관하고 주기적으로 보고한다.
 */
@Component
@Slf4j
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "requestStartTime";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 2000;

    /** 경로별 누적 호출 수 */
    private final Map<String, AtomicLong> callCounts = new ConcurrentHashMap<>();
    /** 경로별 누적 처리 시간 (ms) */
    private final Map<String, AtomicLong> totalDurations = new ConcurrentHashMap<>();
    /** 경로별 최대 처리 시간 (ms) */
    private final Map<String, AtomicLong> maxDurations = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime == null) return;

        long duration = System.currentTimeMillis() - startTime;
        String endpoint = request.getMethod() + " " + request.getRequestURI();

        updateStats(endpoint, duration);

        if (duration >= SLOW_REQUEST_THRESHOLD_MS) {
            log.warn("[SLOW] {} completed in {}ms (status={})", endpoint, duration, response.getStatus());
        } else {
            log.debug("[PERF] {} completed in {}ms", endpoint, duration);
        }
    }

    private void updateStats(String endpoint, long duration) {
        callCounts.computeIfAbsent(endpoint, k -> new AtomicLong()).incrementAndGet();
        totalDurations.computeIfAbsent(endpoint, k -> new AtomicLong()).addAndGet(duration);
        maxDurations.computeIfAbsent(endpoint, k -> new AtomicLong())
                .updateAndGet(max -> Math.max(max, duration));
    }

    /**
     * 매 5분마다 슬로우 엔드포인트 통계를 보고한다.
     */
    @Scheduled(fixedDelay = 300_000)
    public void reportSlowEndpoints() {
        if (callCounts.isEmpty()) return;

        log.info("=== Performance Report (Top Slow Endpoints) ===");
        callCounts.entrySet().stream()
                .filter(e -> {
                    long calls = e.getValue().get();
                    long total = totalDurations.getOrDefault(e.getKey(), new AtomicLong()).get();
                    return calls > 0 && (total / calls) >= 500;
                })
                .forEach(e -> {
                    String ep = e.getKey();
                    long calls = e.getValue().get();
                    long total = totalDurations.getOrDefault(ep, new AtomicLong()).get();
                    long max = maxDurations.getOrDefault(ep, new AtomicLong()).get();
                    log.info("  {} | calls={} avg={}ms max={}ms",
                            ep, calls, calls == 0 ? 0 : total / calls, max);
                });
    }
}
