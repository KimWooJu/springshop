package com.springshop.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson {@link ObjectMapper} 설정.
 *
 * <p>주요 정책:</p>
 * <ul>
 *   <li>java.time API 직렬화 (JavaTimeModule)</li>
 *   <li>날짜를 timestamp 가 아닌 ISO-8601 문자열로 직렬화</li>
 *   <li>알 수 없는 프로퍼티 무시 (FAIL_ON_UNKNOWN_PROPERTIES=false)</li>
 *   <li>null 필드 직렬화 제외</li>
 *   <li>단일 인용부호 허용</li>
 * </ul>
 *
 * <p>두 가지 빈을 제공한다:</p>
 * <ul>
 *   <li>{@link #objectMapper()} - 기본 (Primary) ObjectMapper</li>
 *   <li>{@link #prettyPrintObjectMapper()} - INDENT_OUTPUT 활성화 (디버깅용)</li>
 * </ul>
 */
@Configuration
@Slf4j
public class ObjectMapperConfig {

    /**
     * 기본 {@link ObjectMapper}.
     * Spring MVC 메시지 컨버터, 캐시 직렬화, Redis 등에서 사용된다.
     */
    @Primary
    @Bean
    public ObjectMapper objectMapper() {
        log.info("[ObjectMapperConfig] 기본 ObjectMapper 초기화");

        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
            .modules(new JavaTimeModule())
            .featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                SerializationFeature.FAIL_ON_EMPTY_BEANS,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE
            )
            .featuresToEnable(
                MapperFeature.DEFAULT_VIEW_INCLUSION,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                JsonParser.Feature.ALLOW_SINGLE_QUOTES,
                JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES
            )
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build();

        return mapper;
    }

    /**
     * INDENT_OUTPUT 이 활성화된 ObjectMapper.
     * 로그 출력, 디버깅용으로 사용한다.
     */
    @Bean(name = "prettyPrintObjectMapper")
    public ObjectMapper prettyPrintObjectMapper() {
        log.info("[ObjectMapperConfig] PrettyPrint ObjectMapper 초기화");
        ObjectMapper mapper = objectMapper().copy();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }
}
