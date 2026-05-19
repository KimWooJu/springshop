package com.springshop.security;

import com.springshop.common.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 발급/검증/파싱 컴포넌트.
 *
 * <p>HMAC-SHA256 서명을 사용하며, Access/Refresh 토큰 쌍을 발급한다.
 * 토큰 ID(jti)에 UUID를 부여하여 블랙리스트 처리와 감사 추적이 가능하도록 한다.</p>
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /**
     * Access/Refresh 토큰 쌍.
     */
    public record TokenPair(
        String accessToken,
        String refreshToken,
        LocalDateTime accessExpiresAt,
        LocalDateTime refreshExpiresAt
    ) {}

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * 서명 키를 생성한다. 비밀키 길이는 HS256 요구사항(256bit) 이상이어야 한다.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 사용자 정보로부터 Access/Refresh 토큰 쌍을 발급한다.
     */
    public TokenPair generateTokenPair(UserPrincipal user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime accessExpiry = now.plusMinutes(SecurityConstants.ACCESS_TOKEN_EXPIRY_MINUTES);
        LocalDateTime refreshExpiry = now.plusDays(SecurityConstants.REFRESH_TOKEN_EXPIRY_DAYS);

        String accessToken = buildToken(user, SecurityConstants.JWT_TYPE_ACCESS, accessExpiry);
        String refreshToken = buildToken(user, SecurityConstants.JWT_TYPE_REFRESH, refreshExpiry);

        return new TokenPair(accessToken, refreshToken, accessExpiry, refreshExpiry);
    }

    /**
     * 토큰 빌드 공통 로직.
     */
    private String buildToken(UserPrincipal user, String type, LocalDateTime expiry) {
        Date expiryDate = Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant());
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(user.getId().toString())
            .issuer(SecurityConstants.JWT_ISSUER)
            .claim(SecurityConstants.JWT_CLAIM_USER_ID, user.getId())
            .claim(SecurityConstants.JWT_CLAIM_EMAIL, user.getEmail())
            .claim(SecurityConstants.JWT_CLAIM_ROLE, user.getRole())
            .claim(SecurityConstants.JWT_CLAIM_TYPE, type)
            .issuedAt(new Date())
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * 토큰 서명과 만료를 검증한다.
     * 검증 실패 사유는 로그 레벨을 분리하여 기록한다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 JWT 토큰: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있습니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰 클레임 추출. 검증 실패 시 예외를 던진다.
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get(SecurityConstants.JWT_CLAIM_USER_ID, Long.class);
    }

    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).get(SecurityConstants.JWT_CLAIM_EMAIL, String.class);
    }

    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get(SecurityConstants.JWT_CLAIM_ROLE, String.class);
    }

    public String getTokenId(String token) {
        return getClaimsFromToken(token).getId();
    }

    /**
     * 만료 여부 (예외 발생 시 만료로 간주).
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Access 토큰 여부.
     */
    public boolean isAccessToken(String token) {
        return SecurityConstants.JWT_TYPE_ACCESS.equals(
            getClaimsFromToken(token).get(SecurityConstants.JWT_CLAIM_TYPE, String.class)
        );
    }

    /**
     * Refresh 토큰 여부.
     */
    public boolean isRefreshToken(String token) {
        return SecurityConstants.JWT_TYPE_REFRESH.equals(
            getClaimsFromToken(token).get(SecurityConstants.JWT_CLAIM_TYPE, String.class)
        );
    }

    /**
     * Refresh 토큰으로부터 새 Access 토큰을 발급한다.
     */
    public String generateAccessTokenFromRefresh(String refreshToken) {
        Claims claims = getClaimsFromToken(refreshToken);
        Long userId = claims.get(SecurityConstants.JWT_CLAIM_USER_ID, Long.class);
        String email = claims.get(SecurityConstants.JWT_CLAIM_EMAIL, String.class);
        String role = claims.get(SecurityConstants.JWT_CLAIM_ROLE, String.class);

        LocalDateTime expiry = LocalDateTime.now()
            .plusMinutes(SecurityConstants.ACCESS_TOKEN_EXPIRY_MINUTES);
        Date expiryDate = Date.from(expiry.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId.toString())
            .issuer(SecurityConstants.JWT_ISSUER)
            .claim(SecurityConstants.JWT_CLAIM_USER_ID, userId)
            .claim(SecurityConstants.JWT_CLAIM_EMAIL, email)
            .claim(SecurityConstants.JWT_CLAIM_ROLE, role)
            .claim(SecurityConstants.JWT_CLAIM_TYPE, SecurityConstants.JWT_TYPE_ACCESS)
            .issuedAt(new Date())
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * 토큰의 남은 유효 시간 (밀리초).
     */
    public long getRemainingTimeMs(String token) {
        try {
            return getClaimsFromToken(token).getExpiration().getTime() - System.currentTimeMillis();
        } catch (JwtException e) {
            return 0L;
        }
    }
}
