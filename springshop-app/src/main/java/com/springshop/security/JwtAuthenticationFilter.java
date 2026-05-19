package com.springshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 인증 필터.
 *
 * <p>모든 요청에 대해 Authorization 헤더에서 Bearer 토큰을 추출하고,
 * 서명 검증 및 Redis 블랙리스트 확인 후 SecurityContextHolder에 인증을 설정한다.
 *
 * <p>공개 경로(Swagger, 로그인, 건강 체크 등)는 필터를 건너뛴다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/h2-console/**",
            "/actuator/health",
            "/actuator/info"
    );

    private final JwtTokenProvider jwtTokenProvider;
    private final UserPrincipalService userPrincipalService;
    private final StringRedisTemplate redisTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                processToken(token, request);
            }
        } catch (Exception ex) {
            log.warn("JWT 인증 처리 실패 - path={}, error={}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 토큰을 검증하고 SecurityContextHolder에 인증 정보를 설정한다.
     */
    private void processToken(String token, HttpServletRequest request) {
        if (!jwtTokenProvider.validateToken(token)) {
            log.debug("유효하지 않은 JWT 토큰 - path={}", request.getRequestURI());
            return;
        }

        String jti = jwtTokenProvider.getJti(token);
        if (isBlacklisted(jti)) {
            log.warn("블랙리스트 토큰 사용 시도 - jti={}, path={}", jti, request.getRequestURI());
            return;
        }

        String email = jwtTokenProvider.getSubject(token);
        UserDetails userDetails = userPrincipalService.loadUserByUsername(email);

        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // MDC에 사용자 정보 추가 (로깅용)
        if (userDetails instanceof UserPrincipal principal) {
            MDC.put("userId", String.valueOf(principal.getId()));
            MDC.put("userEmail", principal.getEmail());
        }

        log.debug("JWT 인증 성공 - email={}, path={}", email, request.getRequestURI());
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출한다.
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * Redis에서 토큰 블랙리스트를 확인한다.
     * 로그아웃된 토큰은 jti를 키로 Redis에 저장된다.
     */
    private boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            Boolean exists = redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.error("Redis 블랙리스트 조회 실패 - jti={}, error={}", jti, ex.getMessage());
            // Redis 장애 시 통과 허용 (Fail-Open 정책)
            return false;
        }
    }

    /**
     * 공개 경로는 JWT 필터를 적용하지 않는다.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
