package com.springshop.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 캐시 설정 (Caffeine 기반).
 *
 * <p>캐시 이름별 TTL/사이즈 정책:</p>
 * <table border="1">
 *   <tr><th>cache</th><th>maxSize</th><th>expireAfterWrite</th></tr>
 *   <tr><td>products</td><td>1000</td><td>5분</td></tr>
 *   <tr><td>categories</td><td>200</td><td>1시간</td></tr>
 *   <tr><td>brands</td><td>100</td><td>30분</td></tr>
 *   <tr><td>users</td><td>500</td><td>3분</td></tr>
 *   <tr><td>inventory</td><td>2000</td><td>1분</td></tr>
 *   <tr><td>review-stats</td><td>500</td><td>10분</td></tr>
 *   <tr><td>coupon</td><td>300</td><td>2분</td></tr>
 *   <tr><td>top-products</td><td>10</td><td>10분</td></tr>
 *   <tr><td>dashboard-stats</td><td>1</td><td>10분</td></tr>
 * </table>
 *
 * <p>Caffeine 의 Window TinyLFU 알고리즘은 일반적인 LRU 대비 적중률이 높으며,
 * 메모리 풋프린트도 작아 인메모리 캐시로 적합하다.</p>
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    /**
     * 캐시 정의 record.
     * 각 캐시는 이름, 최대 엔트리 수, write 후 만료 시간을 가진다.
     */
    public record CacheSpec(String name, long maxSize, Duration expireAfterWrite) {

        /** Caffeine 빌더 생성. */
        Caffeine<Object, Object> toCaffeine() {
            return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfterWrite)
                .recordStats();
        }

        /** Spring {@link Cache} 인스턴스 생성. */
        Cache toSpringCache() {
            return new CaffeineCache(name, toCaffeine().build());
        }
    }

    /**
     * 캐시 명세 목록.
     */
    public static final List<CacheSpec> CACHE_SPECS = List.of(
        new CacheSpec("products", 1000L, Duration.ofMinutes(5)),
        new CacheSpec("categories", 200L, Duration.ofHours(1)),
        new CacheSpec("brands", 100L, Duration.ofMinutes(30)),
        new CacheSpec("users", 500L, Duration.ofMinutes(3)),
        new CacheSpec("inventory", 2000L, Duration.ofMinutes(1)),
        new CacheSpec("review-stats", 500L, Duration.ofMinutes(10)),
        new CacheSpec("coupon", 300L, Duration.ofMinutes(2)),
        new CacheSpec("top-products", 10L, Duration.ofMinutes(10)),
        new CacheSpec("dashboard-stats", 1L, Duration.ofMinutes(10)),
        new CacheSpec("orders-summary", 100L, Duration.ofMinutes(5)),
        new CacheSpec("product-rating", 1000L, Duration.ofMinutes(15))
    );

    /**
     * {@link SimpleCacheManager} 기반 캐시 매니저.
     * 캐시별로 개별 Caffeine 인스턴스를 구성하여 TTL/사이즈를 독립적으로 관리한다.
     */
    @Override
    @Bean
    public CacheManager cacheManager() {
        log.info("[CacheConfig] Caffeine 캐시 매니저 초기화 - 등록 캐시 수={}",
            CACHE_SPECS.size());

        List<Cache> caches = new ArrayList<>();
        for (CacheSpec spec : CACHE_SPECS) {
            caches.add(spec.toSpringCache());
            log.debug("[CacheConfig] 캐시 등록 name={} maxSize={} ttl={}",
                spec.name(), spec.maxSize(), spec.expireAfterWrite());
        }

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(caches);
        manager.initializeCaches();
        return manager;
    }

    /**
     * 캐시 작업 중 오류 발생 시 fallback 처리.
     * 캐시 미스/오류가 비즈니스 로직을 중단시키지 않도록 한다.
     */
    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler();
    }

    /**
     * 캐시 오류 처리기 구현체.
     */
    @Slf4j
    static class ResilientCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException ex, Cache cache, Object key) {
            log.warn("[CacheError] GET 실패 cache={} key={} reason={}",
                cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException ex, Cache cache, Object key, Object value) {
            log.warn("[CacheError] PUT 실패 cache={} key={} reason={}",
                cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException ex, Cache cache, Object key) {
            log.warn("[CacheError] EVICT 실패 cache={} key={} reason={}",
                cache.getName(), key, ex.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException ex, Cache cache) {
            log.warn("[CacheError] CLEAR 실패 cache={} reason={}",
                cache.getName(), ex.getMessage());
        }
    }
}
