package com.springshop.config;

import com.springshop.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JPA 관련 설정.
 *
 * <p>JPA Auditing 활성화: {@link org.springframework.data.annotation.CreatedBy} /
 * {@link org.springframework.data.annotation.LastModifiedBy} 어노테이션이 부여된
 * 필드에 자동으로 현재 사용자 식별자를 주입한다.</p>
 *
 * <p>배치 Insert/Update 최적화:</p>
 * <ul>
 *   <li>hibernate.jdbc.batch_size=50 (application.yml)</li>
 *   <li>hibernate.order_inserts=true</li>
 *   <li>hibernate.order_updates=true</li>
 * </ul>
 *
 * <p>레포지토리 스캔 베이스 패키지는 도메인 모듈 {@code com.springshop.domain} 이다.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(basePackages = "com.springshop.domain")
@EnableTransactionManagement
@Slf4j
public class JpaConfig {

    /** Auditing 시 사용자 정보를 추출할 수 없을 때 사용하는 기본값. */
    public static final String SYSTEM_AUDITOR = "SYSTEM";

    /** 비인증 사용자(익명) 식별자. */
    public static final String ANONYMOUS_AUDITOR = "ANONYMOUS";

    /**
     * 현재 인증된 사용자 식별자를 반환하는 {@link AuditorAware} 구현.
     *
     * <p>SecurityContext 에 {@link UserPrincipal} 이 존재하면 그 ID 를,
     * 없으면 {@link #SYSTEM_AUDITOR} 또는 {@link #ANONYMOUS_AUDITOR} 를 반환한다.</p>
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            try {
                Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null) {
                    return Optional.of(SYSTEM_AUDITOR);
                }
                if (!authentication.isAuthenticated()) {
                    return Optional.of(ANONYMOUS_AUDITOR);
                }
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserPrincipal up) {
                    return Optional.of(String.valueOf(up.getId()));
                }
                if (principal instanceof String s && !"anonymousUser".equals(s)) {
                    return Optional.of(s);
                }
                return Optional.of(ANONYMOUS_AUDITOR);
            } catch (Exception e) {
                log.debug("[JpaConfig] auditorAware 추출 실패, SYSTEM 반환: {}", e.getMessage());
                return Optional.of(SYSTEM_AUDITOR);
            }
        };
    }

    /**
     * Hibernate 통계 정보 주기적 로깅 (개발 프로파일 한정).
     *
     * <p>운영 환경에서는 generate_statistics 가 비활성화되어 있으므로 이 빈도 의미가 없다.
     * 개발 환경에서 쿼리 횟수, 캐시 히트율, 평균 실행 시간 등을 60초 간격으로 출력한다.</p>
     */
    @Configuration
    @Profile("dev")
    @Slf4j
    static class StatisticsLogger {

        /**
         * 60초 주기 통계 로깅.
         * 실제 Statistics 객체 접근은 EntityManagerFactory 주입 등 추가 구성이 필요하지만,
         * 본 샘플에서는 단순 마커 로그로 대체한다.
         */
        @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
        public void logStatistics() {
            log.debug("[StatisticsLogger] Hibernate 통계 수집 주기");
        }
    }

    /**
     * 트랜잭션 모니터링 보조 컴포넌트.
     * 장시간 트랜잭션이 의심되는 경우 운영자가 식별할 수 있게 마커 로그를 남긴다.
     */
    @Component
    @Slf4j
    static class TransactionMonitor {

        /**
         * 5분마다 트랜잭션 풀 상태 확인 의도의 마커.
         */
        @Scheduled(fixedDelay = 300_000L, initialDelay = 60_000L)
        public void monitor() {
            log.debug("[TransactionMonitor] 활성 트랜잭션 풀 상태 점검 (주기 5분)");
        }
    }
}
