package com.springshop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springshop.interceptor.AuditInterceptor;
import com.springshop.interceptor.LocaleInterceptor;
import com.springshop.interceptor.PerformanceInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring MVC 설정.
 *
 * <p>인터셉터, 메시지 컨버터, CORS, 리소스 핸들러, 인자 리졸버 등을 등록한다.
 * Security 의 CORS 와 별도로 MVC 레벨 CORS 도 구성하여, 정적 리소스/Actuator 응답에도
 * CORS 헤더가 일관되게 부착되도록 한다.</p>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    private final PerformanceInterceptor performanceInterceptor;
    private final LocaleInterceptor localeInterceptor;
    private final AuditInterceptor auditInterceptor;
    private final ObjectMapper objectMapper;

    /**
     * CORS 매핑 설정 (SecurityConfig CORS와 통합 정책).
     * 정적 리소스/에러 응답에도 동일한 정책을 적용한다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("[WebMvcConfig] CORS 매핑 추가 (전역)");
        registry.addMapping("/**")
            .allowedOriginPatterns("http://localhost:*", "https://*.springshop.com")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Authorization", "X-Correlation-Id")
            .allowCredentials(true)
            .maxAge(3600L);
    }

    /**
     * 인터셉터 등록.
     * <ol>
     *   <li>PerformanceInterceptor - 모든 API 경로</li>
     *   <li>LocaleInterceptor - 모든 API 경로</li>
     *   <li>AuditInterceptor - /api/v1/admin/** 한정</li>
     * </ol>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("[WebMvcConfig] HandlerInterceptor 등록");

        registry.addInterceptor(performanceInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**");

        registry.addInterceptor(localeInterceptor)
            .addPathPatterns("/api/**");

        registry.addInterceptor(auditInterceptor)
            .addPathPatterns("/api/v1/admin/**");
    }

    /**
     * HTTP 메시지 컨버터 설정.
     * 공통 ObjectMapper 를 사용하여 JSON 직렬화 정책을 통일한다.
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("[WebMvcConfig] 메시지 컨버터 등록");

        MappingJackson2HttpMessageConverter jsonConverter =
            new MappingJackson2HttpMessageConverter(objectMapper);
        jsonConverter.setSupportedMediaTypes(List.of(
            MediaType.APPLICATION_JSON,
            new MediaType("application", "json", StandardCharsets.UTF_8)
        ));

        StringHttpMessageConverter stringConverter =
            new StringHttpMessageConverter(StandardCharsets.UTF_8);

        converters.add(0, jsonConverter);
        converters.add(0, stringConverter);
    }

    /**
     * 정적 리소스 핸들러 (Swagger UI, WebJars).
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/springdoc-openapi-ui/")
            .resourceChain(false);
        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")
            .resourceChain(false);
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true);
    }

    /**
     * Pageable, Sort 등의 인자 리졸버 등록.
     * - 기본 페이지 크기 20
     * - 최대 페이지 크기 100
     * - 기본 정렬 createdAt DESC
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
        sortResolver.setSortParameter("sort");
        sortResolver.setFallbackSort(Sort.by(Sort.Direction.DESC, "createdAt"));

        PageableHandlerMethodArgumentResolver pageableResolver =
            new PageableHandlerMethodArgumentResolver(sortResolver);
        pageableResolver.setOneIndexedParameters(false);
        pageableResolver.setMaxPageSize(100);

        resolvers.add(sortResolver);
        resolvers.add(pageableResolver);
        log.info("[WebMvcConfig] PageableHandlerMethodArgumentResolver 등록 maxPageSize=100");
    }
}
