package com.springshop.service.user;

import com.springshop.domain.user.User;
import com.springshop.domain.user.UserRepository;
import com.springshop.domain.user.UserStatus;
import com.springshop.domain.common.exception.InvalidStateException;
import com.springshop.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * {@link AuthService} 표준 구현.
 *
 * <p>JWT는 HMAC-SHA256으로 서명한다.
 * 블랙리스트와 리프레시 토큰 매핑은 Redis(`StringRedisTemplate`)에 저장한다.
 *
 * <p>OAuth2 흐름은 PG/소셜 어댑터로 위임할 수 있으나, 이 샘플에서는
 * 모의 응답을 반환하도록 단순화한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
    private static final Duration TWO_FACTOR_TTL = Duration.ofMinutes(5);

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String TWO_FACTOR_PREFIX = "auth:2fa:";
    private static final String API_KEY_PREFIX = "auth:apikey:";

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${springshop.security.jwt.secret:default-secret-for-development-only-change-me-please}")
    private String jwtSecret;

    private final Map<Long, String> twoFactorStore = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public AuthToken login(String email, String rawPassword, String userAgent, String ipAddress) {
        var user = userRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new InvalidStateException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new InvalidStateException("잠금된 계정입니다. 관리자에게 문의하세요.");
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new InvalidStateException("탈퇴 처리된 계정입니다.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            int failures = userService.incrementLoginFailureCount(user.getId());
            log.warn("로그인 실패: userId={}, failures={}, ip={}", user.getId(), failures, ipAddress);
            throw new InvalidStateException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        userService.resetLoginFailureCount(user.getId());
        userService.recordLoginTimestamp(user.getId(), ipAddress);

        return issueTokenPair(user.getId());
    }

    @Override
    public AuthToken refreshToken(String refreshToken) {
        var refreshKey = REFRESH_PREFIX + refreshToken;
        var userIdStr = redisTemplate.opsForValue().get(refreshKey);
        if (userIdStr == null) {
            throw new InvalidStateException("유효하지 않은 리프레시 토큰입니다.");
        }
        Long userId = Long.valueOf(userIdStr);

        // 일회용: 기존 리프레시 토큰 폐기 후 신규 발급
        redisTemplate.delete(refreshKey);
        return issueTokenPair(userId);
    }

    @Override
    public void logout(String accessToken) {
        var ttl = remainingTtl(accessToken);
        if (ttl <= 0) {
            log.debug("이미 만료된 토큰의 로그아웃 요청 무시");
            return;
        }
        redisTemplate.opsForValue().set(
            BLACKLIST_PREFIX + accessToken, "1", ttl, TimeUnit.SECONDS
        );
        log.info("로그아웃 처리 — 토큰 블랙리스트 등록 (TTL {}s)", ttl);
    }

    @Override
    public void revokeAllTokensOf(Long userId, String reason) {
        log.warn("사용자의 모든 토큰 폐기: userId={}, reason={}", userId, reason);
        // 운영 환경에서는 user별 token version을 증가시켜
        // 발급된 모든 토큰을 일괄 무효화한다.
        redisTemplate.opsForValue().set(
            "auth:token-version:" + userId,
            String.valueOf(System.currentTimeMillis()),
            REFRESH_TOKEN_TTL.toSeconds(), TimeUnit.SECONDS
        );
    }

    @Override
    public void revokeToken(String token, String reason) {
        var ttl = Math.max(remainingTtl(token), 60L);
        redisTemplate.opsForValue().set(
            BLACKLIST_PREFIX + token, reason, ttl, TimeUnit.SECONDS
        );
    }

    @Override
    public TokenValidationResult validateToken(String token) {
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token))) {
                return TokenValidationResult.invalid("블랙리스트에 등록된 토큰");
            }
            var parsed = parseToken(token);
            if (parsed.expiresAt().isBefore(LocalDateTime.now())) {
                return TokenValidationResult.invalid("만료된 토큰");
            }
            return new TokenValidationResult(true, parsed.userId(), null, parsed.expiresAt());
        } catch (Exception e) {
            return TokenValidationResult.invalid("토큰 파싱 실패: " + e.getMessage());
        }
    }

    @Override
    public Optional<Long> extractUserId(String token) {
        try {
            return Optional.of(parseToken(token).userId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public OAuthTokenResponse exchangeAuthorizationCode(String provider, String code, String redirectUri) {
        log.info("OAuth 인가 코드 교환: provider={}, redirectUri={}", provider, redirectUri);
        // 실제 환경에서는 RestClient로 IdP 토큰 엔드포인트 호출
        return new OAuthTokenResponse(
            "mock-" + provider + "-access-" + code,
            "mock-" + provider + "-refresh-" + code,
            "Bearer",
            3600L,
            "openid profile email",
            "mock-id-token"
        );
    }

    @Override
    @Transactional
    public AuthToken loginWithOAuth(String provider, String oauthAccessToken) {
        var oauthProfile = fetchOAuthProfile(provider, oauthAccessToken);
        var email = oauthProfile.email().toLowerCase();

        var user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                log.info("OAuth 신규 가입: provider={}, email={}", provider, email);
                return userRepository.save(User.createOAuth(
                    email,
                    oauthProfile.name(),
                    oauthProfile.nickname(),
                    provider,
                    oauthProfile.subject()
                ));
            });

        if (user.getStatus() == UserStatus.WITHDRAWN || user.getStatus() == UserStatus.LOCKED) {
            throw new InvalidStateException("해당 계정으로는 로그인할 수 없습니다.");
        }
        userService.recordLoginTimestamp(user.getId(), "oauth:" + provider);
        return issueTokenPair(user.getId());
    }

    @Override
    public String sendTwoFactorCode(Long userId, TwoFactorChannel channel) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }
        var code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        twoFactorStore.put(userId, code);
        redisTemplate.opsForValue().set(
            TWO_FACTOR_PREFIX + userId, code,
            TWO_FACTOR_TTL.toSeconds(), TimeUnit.SECONDS
        );
        log.info("2FA 코드 발송: userId={}, channel={}", userId, channel);
        return code; // 데모용 반환 — 실 환경에서는 발송만 수행
    }

    @Override
    public boolean verifyTwoFactorCode(Long userId, String code) {
        var stored = redisTemplate.opsForValue().get(TWO_FACTOR_PREFIX + userId);
        if (stored == null) stored = twoFactorStore.get(userId);
        if (stored == null) return false;
        boolean ok = stored.equals(code);
        if (ok) {
            redisTemplate.delete(TWO_FACTOR_PREFIX + userId);
            twoFactorStore.remove(userId);
        }
        return ok;
    }

    @Override
    public String issueApiKey(Long userId, String description) {
        var key = "sk_" + base64Url(generateRandomBytes(48));
        redisTemplate.opsForValue().set(API_KEY_PREFIX + key, userId + ":" + description);
        log.info("API 키 발급: userId={}, description={}", userId, description);
        return key;
    }

    @Override
    public void revokeApiKey(String apiKey) {
        redisTemplate.delete(API_KEY_PREFIX + apiKey);
        log.info("API 키 폐기");
    }

    // ---- 내부 유틸 ----

    private AuthToken issueTokenPair(Long userId) {
        var now = LocalDateTime.now();
        var accessExp = now.plus(ACCESS_TOKEN_TTL);
        var refreshExp = now.plus(REFRESH_TOKEN_TTL);

        var accessToken = signToken(userId, accessExp);
        var refreshToken = base64Url(generateRandomBytes(64));

        redisTemplate.opsForValue().set(
            REFRESH_PREFIX + refreshToken,
            String.valueOf(userId),
            REFRESH_TOKEN_TTL.toSeconds(), TimeUnit.SECONDS
        );

        return new AuthToken(
            accessToken,
            refreshToken,
            ACCESS_TOKEN_TTL.toSeconds(),
            now,
            userId
        );
    }

    private String signToken(Long userId, LocalDateTime expiresAt) {
        var payload = userId + "." + expiresAt.toEpochSecond(ZoneOffset.UTC);
        var sig = hmacSha256(payload, jwtSecret);
        return base64Url(payload.getBytes(StandardCharsets.UTF_8)) + "." + sig;
    }

    private ParsedToken parseToken(String token) {
        var parts = token.split("\\.");
        if (parts.length != 2) throw new IllegalArgumentException("invalid token format");
        var payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        var expectedSig = hmacSha256(payload, jwtSecret);
        if (!expectedSig.equals(parts[1])) {
            throw new IllegalArgumentException("signature mismatch");
        }
        var fields = payload.split("\\.");
        long userId = Long.parseLong(fields[0]);
        long epoch = Long.parseLong(fields[1]);
        return new ParsedToken(userId, LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC));
    }

    private long remainingTtl(String token) {
        try {
            var exp = parseToken(token).expiresAt();
            return Math.max(0, java.time.Duration.between(LocalDateTime.now(), exp).toSeconds());
        } catch (Exception e) {
            return 60L;
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 계산 실패", e);
        }
    }

    private static byte[] generateRandomBytes(int length) {
        var bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private OAuthProfile fetchOAuthProfile(String provider, String oauthAccessToken) {
        // 운영 환경에서는 provider별 userinfo 엔드포인트를 호출하여 프로필을 가져온다.
        return new OAuthProfile(
            "oauth-" + provider + "-" + oauthAccessToken.hashCode() + "@example.com",
            "OAuth User",
            "oauth_user_" + Math.abs(oauthAccessToken.hashCode() % 10_000),
            "subject-" + oauthAccessToken.hashCode()
        );
    }

    private record ParsedToken(Long userId, LocalDateTime expiresAt) {}

    private record OAuthProfile(String email, String name, String nickname, String subject) {}
}
