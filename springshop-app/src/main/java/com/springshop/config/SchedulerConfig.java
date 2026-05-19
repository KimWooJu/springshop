package com.springshop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.TimeZone;

/**
 * 스케줄러 설정.
 *
 * <p>{@code @Scheduled} 어노테이션이 부여된 메서드 실행을 위한
 * {@link ThreadPoolTaskScheduler} 를 구성한다.</p>
 *
 * <p>기본 타임존: KST (Asia/Seoul). cron 표현식은 KST 기준으로 해석된다.</p>
 *
 * <p>풀 크기 5는 일반적인 배치 작업 수(자정 정리, 5분 단위 통계, 1시간 단위 동기화)를
 * 동시에 처리하기에 충분하다. 동시 실행이 필요한 작업이 늘면 풀 크기를 상향한다.</p>
 */
@Configuration
@EnableScheduling
@Slf4j
public class SchedulerConfig implements SchedulingConfigurer {

    /** 한국 표준시 타임존. */
    public static final String KST_ZONE = "Asia/Seoul";

    /** 기본 스케줄러 풀 크기. */
    public static final int DEFAULT_POOL_SIZE = 5;

    /**
     * 스케줄러용 {@link ThreadPoolTaskScheduler} 빈.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        log.info("[SchedulerConfig] ThreadPoolTaskScheduler 초기화 poolSize={} zone={}",
            DEFAULT_POOL_SIZE, KST_ZONE);

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(DEFAULT_POOL_SIZE);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setErrorHandler(t ->
            log.error("[Scheduler] 스케줄 작업 실패: {}", t.getMessage(), t));
        scheduler.initialize();

        // 기본 타임존을 KST 로 설정 (cron 표현식 해석 기준)
        TimeZone.setDefault(TimeZone.getTimeZone(KST_ZONE));
        return scheduler;
    }

    /**
     * {@link SchedulingConfigurer} 콜백.
     * 사용자 정의 TaskScheduler 를 Spring 의 스케줄 인프라에 등록한다.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setTaskScheduler(taskScheduler());
        log.debug("[SchedulerConfig] ScheduledTaskRegistrar 에 사용자 TaskScheduler 주입");
    }
}
