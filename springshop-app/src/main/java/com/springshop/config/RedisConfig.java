package com.springshop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 설정.
 *
 * <p>Lettuce 드라이버 기반 Redis 클라이언트를 구성한다.
 * Lettuce 는 Netty 기반의 non-blocking I/O 를 사용하므로 가상 스레드 환경과 잘 어울린다.</p>
 *
 * <p>제공 빈:</p>
 * <ul>
 *   <li>{@link LettuceConnectionFactory} - 연결 팩토리</li>
 *   <li>{@code redisTemplate} - {@code RedisTemplate<String, Object>} (JSON 직렬화)</li>
 *   <li>{@link StringRedisTemplate} - 문자열 전용</li>
 *   <li>{@code longRedisTemplate} - {@code RedisTemplate<String, Long>} (카운터용)</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.timeout:3000ms}")
    private Duration timeout;

    private final ObjectMapper objectMapper;

    /**
     * 시작 시 Redis 연결 확인. 실패 시 경고만 출력하고 부팅은 진행한다.
     * (개발 환경에서 Redis 가 없어도 헬스체크 외 기능이 동작하도록.)
     */
    @PostConstruct
    public void init() {
        log.info("[RedisConfig] Redis 설정 초기화 host={} port={} timeout={}",
            host, port, timeout);
    }

    /**
     * Lettuce 커넥션 팩토리.
     * 연결 풀(Commons Pool2) 설정과 함께 구성한다.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(host);
        standalone.setPort(port);
        if (password != null && !password.isBlank()) {
            standalone.setPassword(password);
        }

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setMaxWait(Duration.ofMillis(2000));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(timeout)
            .shutdownTimeout(Duration.ofMillis(200))
            .build();

        LettuceConnectionFactory factory =
            new LettuceConnectionFactory(standalone, clientConfig);
        factory.setValidateConnection(false);
        factory.setShareNativeConnection(true);
        return factory;
    }

    /**
     * 범용 RedisTemplate.
     * key 는 String, value 는 JSON 직렬화된 Object.
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(
            LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<Object> valueSerializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.setDefaultSerializer(valueSerializer);
        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * StringRedisTemplate.
     * key/value 모두 String. 카운터, 캐시 마커, 토큰 블랙리스트 등에 사용.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(
            LettuceConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.setEnableTransactionSupport(false);
        return template;
    }

    /**
     * Long 값 전용 RedisTemplate.
     * Rate Limit, 조회수, 좋아요 카운터 등에 사용.
     */
    @Bean(name = "longRedisTemplate")
    @Qualifier("longRedisTemplate")
    public RedisTemplate<String, Long> longRedisTemplate(
            LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<Long> longSerializer =
            new Jackson2JsonRedisSerializer<>(objectMapper, Long.class);
        template.setValueSerializer(longSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(longSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
