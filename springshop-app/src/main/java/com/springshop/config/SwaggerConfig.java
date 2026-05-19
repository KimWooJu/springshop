package com.springshop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI 3 문서화 설정.
 *
 * <p>그룹별 API 명세를 분리하여 영역별 문서를 제공한다:</p>
 * <ul>
 *   <li>public  - 인증 없이 호출 가능한 공개 API</li>
 *   <li>auth    - 회원가입/로그인/토큰 갱신</li>
 *   <li>user    - 회원 전용 API (장바구니, 주문, 마이페이지)</li>
 *   <li>seller  - 판매자 API</li>
 *   <li>admin   - 관리자 API</li>
 * </ul>
 *
 * <p>인증 스키마: HTTP Bearer (JWT). Swagger UI 우측 상단의 "Authorize" 버튼에서
 * Access Token 을 입력하면 모든 API 호출에 자동으로 헤더가 부착된다.</p>
 */
@Configuration
@Slf4j
public class SwaggerConfig {

    /** 보안 스키마 이름 (security requirement 키). */
    public static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * OpenAPI 메타데이터 빈.
     */
    @Bean
    public OpenAPI springShopOpenAPI() {
        log.info("[SwaggerConfig] OpenAPI 명세 초기화");

        return new OpenAPI()
            .info(apiInfo())
            .servers(servers())
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerSecurityScheme()))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * API 정보 (제목, 버전, 설명, 연락처).
     */
    private Info apiInfo() {
        return new Info()
            .title("SpringShop API")
            .version("1.0.0")
            .description("""
                SpringShop E-Commerce Platform REST API.

                Stack: Java 25 + Spring Boot 3.4.x + JPA + Redis

                인증 방식: JWT Bearer Token
                - 로그인 후 발급받은 Access Token 을 우측 상단 'Authorize' 버튼을 통해 입력하세요.
                - Access Token 만료 시 Refresh Token 으로 재발급 가능합니다.

                응답 코드:
                - 200: 정상 처리
                - 201: 생성 완료
                - 400: 잘못된 요청 (검증 실패 포함)
                - 401: 인증 필요
                - 403: 권한 없음
                - 404: 리소스 없음
                - 409: 충돌 (중복 데이터 등)
                - 429: 요청 한도 초과
                - 500: 서버 내부 오류
                """)
            .contact(new Contact()
                .name("SpringShop Dev Team")
                .email("dev@springshop.com")
                .url("https://github.com/springshop/api"))
            .license(new License()
                .name("Apache License 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    /**
     * 환경별 서버 목록.
     */
    private List<Server> servers() {
        return List.of(
            new Server().url("http://localhost:8080").description("Development"),
            new Server().url("https://staging.springshop.com").description("Staging"),
            new Server().url("https://api.springshop.com").description("Production")
        );
    }

    /**
     * HTTP Bearer 인증 스키마 (JWT).
     */
    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .name(SECURITY_SCHEME_NAME)
            .description("JWT Access Token을 입력하세요. (Bearer 접두사 자동 부착)");
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch(
                "/api/v1/products/**",
                "/api/v1/categories/**",
                "/api/v1/brands/**",
                "/api/v1/reviews/**"
            )
            .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("auth")
            .pathsToMatch("/api/v1/auth/**")
            .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("user")
            .pathsToMatch(
                "/api/v1/users/**",
                "/api/v1/cart/**",
                "/api/v1/orders/**",
                "/api/v1/wishlist/**",
                "/api/v1/coupons/**"
            )
            .build();
    }

    @Bean
    public GroupedOpenApi sellerApi() {
        return GroupedOpenApi.builder()
            .group("seller")
            .pathsToMatch("/api/v1/seller/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/v1/admin/**")
            .build();
    }
}
