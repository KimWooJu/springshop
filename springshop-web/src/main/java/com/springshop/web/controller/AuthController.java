package com.springshop.web.controller;

import com.springshop.web.dto.request.CreateUserRequest;
import com.springshop.web.dto.request.LoginRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.AuthTokenResponse;
import com.springshop.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인증 API 컨트롤러.
 *
 * <p>로그인, 로그아웃, 토큰 리프레시, 비밀번호 재설정, 이메일 인증,
 * 그리고 OAuth2 외부 인증 시작 등 인증 라이프사이클의 엔드포인트를 제공한다.
 *
 * <p>JWT(Access + Refresh) 토큰 발급, 디바이스 식별, 2FA 검증을 통합 처리한다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "인증 API", description = "로그인/로그아웃/토큰 갱신/패스워드 재설정/이메일 인증")
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(2000);

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인하고 JWT 토큰을 발급한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "계정 잠금")
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Parameter(description = "로그인 요청", required = true)
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        LOG.info("로그인 시도 - email={}, device={}, ua={}",
                request.email(), request.deviceType(), userAgent);

        if (request.requires2FA() && Boolean.TRUE.equals(needs2FA(request.email()))) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                    .body(ApiResponse.Failure.of("OTP_REQUIRED", "2FA 코드가 필요합니다."));
        }

        String access = "eyJhbGciOiJIUzI1NiJ9." + UUID.randomUUID().toString();
        String refresh = "rf-" + UUID.randomUUID().toString();
        AuthTokenResponse data = AuthTokenResponse.of(
                access, refresh, request.preferredExpiresIn(),
                1024L, request.email(), "로그인사용자", "USER");

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, data.authorizationHeader())
                .body(ApiResponse.Success.of(data, "로그인 성공"));
    }

    @Operation(summary = "로그아웃", description = "현재 토큰을 무효화하고 세션을 종료한다.")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(defaultValue = "false") boolean allDevices) {
        LOG.info("로그아웃 요청 - allDevices={}", allDevices);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "토큰 리프레시", description = "Refresh Token으로 새 Access Token을 발급한다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @RequestHeader(value = "X-Refresh-Token", required = true) String refreshToken) {
        LOG.debug("토큰 리프레시 - len={}", refreshToken == null ? 0 : refreshToken.length());
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.Failure.of("INVALID_TOKEN", "리프레시 토큰이 비어있습니다."));
        }
        String newAccess = "eyJhbGciOiJIUzI1NiJ9." + UUID.randomUUID();
        String newRefresh = "rf-" + UUID.randomUUID();
        AuthTokenResponse data = AuthTokenResponse.of(
                newAccess, newRefresh, 3600L,
                1024L, "user@example.com", "사용자", "USER");
        return ResponseEntity.ok(ApiResponse.Success.of(data, "토큰 갱신 완료"));
    }

    @Operation(summary = "회원가입 (인증 컨텍스트)", description = "회원가입 후 즉시 자동 로그인 토큰을 발급한다.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> register(
            @Valid @RequestBody CreateUserRequest request) {
        LOG.info("자동 로그인 회원가입 - email={}", request.email());
        Long newId = ID_SEQ.incrementAndGet();
        String access = "eyJhbGciOiJIUzI1NiJ9." + UUID.randomUUID();
        String refresh = "rf-" + UUID.randomUUID();
        AuthTokenResponse data = AuthTokenResponse.of(
                access, refresh, 3600L,
                newId, request.email(), request.name(), "USER");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "회원가입 및 로그인 완료"));
    }

    @Operation(summary = "비밀번호 재설정 요청", description = "이메일로 재설정 토큰을 발송한다.")
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestPasswordReset(
            @RequestParam String email) {
        LOG.info("비밀번호 재설정 요청 - email={}", email);
        String token = UUID.randomUUID().toString();
        return ResponseEntity.ok(ApiResponse.Success.of(
                Map.of("email", email, "tokenIssued", "true",
                        "expiresAt", LocalDateTime.now().plusMinutes(30).toString()),
                "재설정 메일을 발송했습니다."
        ));
    }

    @Operation(summary = "비밀번호 재설정 확인", description = "토큰을 검증하고 새 비밀번호를 적용한다.")
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @RequestParam String token,
            @RequestParam String newPassword) {
        LOG.info("비밀번호 재설정 확인 - tokenLen={}", token == null ? 0 : token.length());
        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.Failure.of("WEAK_PASSWORD", "비밀번호는 8자 이상이어야 합니다."));
        }
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "이메일 인증 확인", description = "이메일 발송된 토큰으로 계정을 활성화한다.")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmail(
            @RequestParam String token) {
        LOG.info("이메일 인증 토큰 확인 - len={}", token == null ? 0 : token.length());
        UserResponse data = UserResponse.of(
                1024L, "verified@example.com", "인증완료",
                "010-0000-0000", "ACTIVE", "USER", true);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "이메일 인증 완료"));
    }

    @Operation(summary = "인증 이메일 재발송", description = "활성화되지 않은 사용자에게 인증 메일을 재발송한다.")
    @PostMapping("/email/resend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendVerification(
            @RequestParam String email) {
        LOG.info("인증 이메일 재발송 - email={}", email);
        Map<String, Object> meta = Map.of(
                "email", email,
                "sentAt", LocalDateTime.now().toString(),
                "nextAvailableAt", LocalDateTime.now().plusMinutes(5).toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(meta, "인증 메일을 발송했습니다."));
    }

    @Operation(summary = "OAuth2 인증 시작", description = "외부 OAuth2 프로바이더 인증 페이지로 리다이렉트할 URL을 반환한다.")
    @GetMapping("/oauth2/{provider}")
    public ResponseEntity<ApiResponse<Map<String, String>>> oauth2Authorize(
            @Parameter(description = "프로바이더 (google, kakao, naver, github)", required = true)
            @PathVariable String provider,
            @RequestParam(required = false) String redirectUri) {
        LOG.debug("OAuth2 시작 - provider={}, redirectUri={}", provider, redirectUri);
        String state = UUID.randomUUID().toString();
        String authorizeUrl = switch (provider.toLowerCase()) {
            case "google" -> "https://accounts.google.com/o/oauth2/v2/auth?state=" + state;
            case "kakao" -> "https://kauth.kakao.com/oauth/authorize?state=" + state;
            case "naver" -> "https://nid.naver.com/oauth2.0/authorize?state=" + state;
            case "github" -> "https://github.com/login/oauth/authorize?state=" + state;
            default -> null;
        };
        if (authorizeUrl == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.Failure.of("UNKNOWN_PROVIDER", "지원하지 않는 OAuth2 제공자: " + provider));
        }
        Map<String, String> data = Map.of(
                "provider", provider,
                "authorizeUrl", authorizeUrl,
                "state", state,
                "redirectUri", redirectUri == null ? "/oauth2/callback" : redirectUri
        );
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "OAuth2 콜백", description = "외부 프로바이더에서 인증 코드를 받아 토큰을 발급한다.")
    @GetMapping("/oauth2/{provider}/callback")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> oauth2Callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        LOG.info("OAuth2 콜백 - provider={}, state={}", provider, state);
        String access = "eyJhbGciOiJIUzI1NiJ9." + UUID.randomUUID();
        String refresh = "rf-" + UUID.randomUUID();
        AuthTokenResponse data = AuthTokenResponse.of(
                access, refresh, 3600L,
                ID_SEQ.incrementAndGet(),
                provider + "_user@example.com",
                provider + " 사용자",
                "USER");
        return ResponseEntity.ok(ApiResponse.Success.of(data, provider + " 로그인 성공"));
    }

    @Operation(summary = "현재 인증 상태 조회", description = "현재 토큰의 유효성을 확인한다.")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> currentUser() {
        UserResponse data = UserResponse.of(
                1024L, "me@example.com", "현재사용자",
                "010-1234-5678", "ACTIVE", "USER", true);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "2FA OTP 발급", description = "2단계 인증 OTP를 발급하여 이메일/SMS로 전송한다.")
    @PostMapping("/2fa/otp/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> send2FAOtp(
            @RequestParam String email,
            @RequestParam(defaultValue = "EMAIL") String channel) {
        LOG.info("2FA OTP 발급 - email={}, channel={}", email, channel);
        Map<String, Object> meta = Map.of(
                "email", email,
                "channel", channel,
                "expiresIn", 300L,
                "issuedAt", LocalDateTime.now().toString()
        );
        return ResponseEntity.ok(ApiResponse.Success.of(meta, "OTP를 발송했습니다."));
    }

    @Operation(summary = "2FA OTP 검증", description = "발급된 OTP의 유효성을 검증한다.")
    @PostMapping("/2fa/otp/verify")
    public ResponseEntity<ApiResponse<Boolean>> verify2FAOtp(
            @RequestParam String email,
            @RequestParam String otpCode) {
        LOG.info("2FA OTP 검증 - email={}", email);
        if (otpCode == null || otpCode.length() != 6) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.Failure.of("INVALID_OTP", "OTP는 6자리여야 합니다."));
        }
        return ResponseEntity.ok(ApiResponse.Success.of(Boolean.TRUE, "OTP 검증 성공"));
    }

    private Boolean needs2FA(String email) {
        return email != null && email.endsWith("@admin.example.com");
    }

    private List<String> defaultAuthorities() {
        return List.of("ROLE_USER", "SCOPE_read", "SCOPE_write");
    }
}
