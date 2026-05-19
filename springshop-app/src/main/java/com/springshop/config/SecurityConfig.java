package com.springshop.config;

import com.springshop.common.constant.SecurityConstants;
import com.springshop.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 핵심 설정.
 *
 * <p>JWT 기반 무상태(stateless) 인증을 사용한다. 세션 저장을 사용하지 않으며,
 * 모든 요청은 {@link JwtAuthenticationFilter} 를 거쳐 인증 컨텍스트를 구성한다.</p>
 *
 * <p>역할(Role) 정의:</p>
 * <ul>
 *   <li>USER - 일반 회원</li>
 *   <li>SELLER - 판매자</li>
 *   <li>ADMIN - 관리자</li>
 *   <li>SUPER_ADMIN - 슈퍼 관리자</li>
 * </ul>
 *
 * <p>주요 정책:</p>
 * <ul>
 *   <li>CSRF 비활성화 (JWT 사용)</li>
 *   <li>CORS 활성화 (허용 Origin 은 {@link SecurityConstants#ALLOWED_ORIGINS})</li>
 *   <li>세션 정책 STATELESS</li>
 *   <li>예외 발생 시 JSON 응답 (401/403)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 보안 필터 체인을 구성한다.
     *
     * <p>경로별 인가 정책:</p>
     * <ul>
     *   <li>OPTIONS - permitAll (Preflight 요청 허용)</li>
     *   <li>PUBLIC_URLS - permitAll (회원가입, 로그인, 정적 리소스 등)</li>
     *   <li>GET /api/v1/products, /api/v1/categories, /api/v1/reviews/products - permitAll</li>
     *   <li>/api/v1/admin/** - hasRole("ADMIN")</li>
     *   <li>그 외 - 인증 필요</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("[SecurityConfig] Spring Security 필터 체인 구성 시작");

        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(opt -> {})
                .xssProtection(xss -> {})
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(SecurityConstants.PUBLIC_URLS).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/products/**",
                    "/api/v1/categories/**",
                    "/api/v1/brands/**",
                    "/api/v1/reviews/products/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole(
                    SecurityConstants.ROLE_ADMIN,
                    SecurityConstants.ROLE_SUPER_ADMIN)
                .requestMatchers("/api/v1/seller/**").hasAnyRole(
                    SecurityConstants.ROLE_SELLER,
                    SecurityConstants.ROLE_ADMIN,
                    SecurityConstants.ROLE_SUPER_ADMIN)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    log.debug("[SecurityConfig] 인증 실패: path={}, reason={}",
                        request.getRequestURI(), e.getMessage());
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("""
                        {
                          "errorCode": "UNAUTHORIZED",
                          "message": "로그인이 필요합니다",
                          "timestamp": "%s",
                          "path": "%s"
                        }
                        """.formatted(LocalDateTime.now(), request.getRequestURI()));
                })
                .accessDeniedHandler((request, response, e) -> {
                    log.warn("[SecurityConfig] 접근 거부: path={}, reason={}",
                        request.getRequestURI(), e.getMessage());
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("""
                        {
                          "errorCode": "FORBIDDEN",
                          "message": "접근 권한이 없습니다",
                          "timestamp": "%s",
                          "path": "%s"
                        }
                        """.formatted(LocalDateTime.now(), request.getRequestURI()));
                })
            )
            .build();
    }

    /**
     * BCrypt 기반 비밀번호 인코더.
     * Strength={@link SecurityConstants#BCRYPT_STRENGTH} 로 충분한 계산 비용을 부여한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("[SecurityConfig] BCryptPasswordEncoder 구성 strength={}",
            SecurityConstants.BCRYPT_STRENGTH);
        return new BCryptPasswordEncoder(SecurityConstants.BCRYPT_STRENGTH);
    }

    /**
     * Spring Security {@link AuthenticationManager} 노출.
     * 로그인 서비스 내부에서 인증 시도 시 사용된다.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS 설정 소스.
     *
     * <p>Allowed origins, methods, headers 는 {@link SecurityConstants} 에 정의한다.
     * Credentials 전송을 허용하며, Preflight 응답을 1시간 캐싱한다.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(SecurityConstants.ALLOWED_ORIGINS));
        config.setAllowedMethods(Arrays.asList(SecurityConstants.ALLOWED_METHODS));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
            "Authorization",
            "X-Refresh-Token",
            "X-Correlation-Id",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(SecurityConstants.MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        log.info("[SecurityConfig] CORS 정책 등록: origins={}, methods={}",
            SecurityConstants.ALLOWED_ORIGINS.length,
            SecurityConstants.ALLOWED_METHODS.length);
        return source;
    }
}
