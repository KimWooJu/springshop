package com.springshop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 비동기 처리 설정.
 *
 * <p>Java 21+ Virtual Thread 기반 비동기 처리 - 스레드 풀 블로킹 없이 확장성을 확보한다.
 * 가상 스레드는 캐리어 스레드 위에서 협력적으로 마운트/언마운트되며, I/O 대기 시 컨텍스트만
 * 보관하므로 동시성 한계가 OS 스레드 수에 묶이지 않는다.</p>
 *
 * <p>제공되는 Executor:</p>
 * <ul>
 *   <li>기본 ({@code virtualThreadExecutor}) - 일반 비동기 작업</li>
 *   <li>{@code ioExecutor} - I/O 집약 작업 (HTTP, DB, 메일, 파일)</li>
 *   <li>{@code cpuExecutor} - CPU 집약 작업 (썸네일, 압축) - 플랫폼 스레드 풀</li>
 * </ul>
 *
 * <p>주의: 가상 스레드 환경에서 {@code synchronized} 블록은 캐리어 스레드를 점유(pinning)하여
 * 확장성을 떨어뜨릴 수 있으므로 {@link java.util.concurrent.locks.ReentrantLock} 사용을 권장한다.</p>
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    private static final AtomicLong COUNTER = new AtomicLong(0);

    /**
     * 기본 비동기 실행자.
     * Spring 의 {@code @Async} 어노테이션이 부여된 메서드가 이 실행자를 사용한다.
     */
    @Override
    public Executor getAsyncExecutor() {
        log.info("[AsyncConfig] 기본 비동기 실행자: Virtual Thread Per Task");
        return virtualThreadExecutor();
    }

    /**
     * 가상 스레드 실행자.
     * 작업마다 새 가상 스레드를 생성하며, 풀 사이즈 제한이 없다.
     */
    @Primary
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
            .name("vt-default-", 0)
            .uncaughtExceptionHandler((t, e) ->
                log.error("[VirtualThread] 미처리 예외 thread={}", t.getName(), e))
            .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * I/O 작업 전용 가상 스레드 실행자.
     * HTTP 클라이언트, 메일 발송, 외부 API 호출, 파일 업로드 등에 사용한다.
     */
    @Bean(name = "ioExecutor")
    public Executor ioExecutor() {
        log.info("[AsyncConfig] I/O 전용 가상 스레드 실행자 생성");
        ThreadFactory factory = Thread.ofVirtual()
            .name("vt-io-", 0)
            .uncaughtExceptionHandler((t, e) ->
                log.error("[VirtualThread-IO] 미처리 예외 thread={}", t.getName(), e))
            .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * CPU 집약 작업 전용 플랫폼 스레드 풀.
     * 압축, 이미지 처리, 통계 계산 등 CPU 를 오래 점유하는 작업은 가상 스레드 대신
     * 고정 크기 플랫폼 스레드 풀이 더 적합하다.
     */
    @Bean(name = "cpuExecutor")
    public Executor cpuExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        log.info("[AsyncConfig] CPU 작업 전용 플랫폼 스레드 풀 cores={}", cores);

        ThreadFactory factory = r -> {
            Thread t = new Thread(r);
            t.setName("cpu-worker-" + COUNTER.incrementAndGet());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        };
        return Executors.newFixedThreadPool(cores, factory);
    }

    /**
     * 비동기 메서드에서 발생한 미처리 예외 핸들러.
     * 호출 메서드명, 인자 등을 함께 로깅하여 추적이 용이하도록 한다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new DetailedAsyncExceptionHandler();
    }

    /**
     * 비동기 예외 핸들러 구현체.
     */
    static class DetailedAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("[AsyncException] 메서드 실행 실패 method={}, params={}, error={}",
                method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
                Arrays.toString(params),
                ex.getMessage(), ex);
        }
    }
}
