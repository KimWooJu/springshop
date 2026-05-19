package com.springshop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SpringShop E-Commerce Platform 메인 진입점.
 *
 * <p>스택: Java 25 + Spring Boot 3.4.x + Maven 3.9</p>
 *
 * <p>모듈 구조:</p>
 * <ul>
 *   <li>springshop-common - 상수/예외/유틸/모델</li>
 *   <li>springshop-domain - JPA 엔티티, 리포지토리</li>
 *   <li>springshop-service - 비즈니스 서비스</li>
 *   <li>springshop-web - 컨트롤러, DTO</li>
 *   <li>springshop-app - 부트스트랩, 설정, 보안</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.springshop")
@EnableAsync
@EnableScheduling
@Slf4j
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 애플리케이션 기동 완료 시 부팅 배너를 출력한다.
     * Spring Boot 헬스체크와 별개로 운영자가 기동 완료 시각을 즉시 확인할 수 있게 한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("""

            ╔══════════════════════════════════════════════════╗
            ║         SpringShop E-Commerce Platform           ║
            ║         Java 25 + Spring Boot 3.4.x              ║
            ╠══════════════════════════════════════════════════╣
            ║  API:     http://localhost:8080/api/v1           ║
            ║  Swagger: http://localhost:8080/swagger-ui.html  ║
            ║  Health:  http://localhost:8080/actuator/health  ║
            ║  H2:      http://localhost:8080/h2-console       ║
            ╚══════════════════════════════════════════════════╝
            """);
    }
}
