package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 기본 정보 응답.
 *
 * <p>회원 가입 직후, 일반 사용자 단건 조회, 관리자 목록 응답에 공통으로 사용된다.
 * 개인정보 보호를 위해 비밀번호 해시, 보안 답변 등은 절대로 노출하지 않는다.
 */
@Schema(description = "사용자 기본 정보 응답")
public record UserResponse(
        @Schema(description = "사용자 ID", example = "1024")
        Long id,

        @Schema(description = "이메일", example = "alice@example.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "전화번호 (마스킹)", example = "010-1234-****")
        String phoneMasked,

        @Schema(description = "상태", example = "ACTIVE",
                allowableValues = {"ACTIVE", "INACTIVE", "LOCKED", "DELETED"})
        String status,

        @Schema(description = "역할", example = "USER",
                allowableValues = {"USER", "ADMIN", "SELLER", "GUEST"})
        String role,

        @Schema(description = "이메일 인증 여부", example = "true")
        boolean emailVerified,

        @Schema(description = "최근 로그인 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastLoginAt,

        @Schema(description = "가입 일시")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @Schema(description = "보유 권한 목록")
        List<String> authorities
) {

    public UserResponse {
        authorities = authorities == null ? List.of() : List.copyOf(authorities);
    }

    /** 마스킹된 사용자 응답을 생성한다. */
    public static UserResponse of(Long id, String email, String name, String phone,
                                  String status, String role, boolean emailVerified) {
        return new UserResponse(
                id, email, name, maskPhone(phone), status, role, emailVerified,
                null, LocalDateTime.now(), List.of("ROLE_" + role)
        );
    }

    /** 관리자용 - 마스킹 없이 모든 필드 전달. */
    public static UserResponse forAdmin(Long id, String email, String name, String phone,
                                        String status, String role, boolean emailVerified,
                                        LocalDateTime lastLoginAt, LocalDateTime createdAt) {
        return new UserResponse(
                id, email, name, phone, status, role, emailVerified,
                lastLoginAt, createdAt, List.of("ROLE_" + role)
        );
    }

    /** 010-1234-5678 → 010-1234-**** 마스킹. */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone;
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }

    /** 이메일을 마스킹한 사본을 반환한다. */
    public UserResponse withMaskedEmail() {
        if (email == null || !email.contains("@")) {
            return this;
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String maskedLocal = local.length() <= 2 ? "**" : local.charAt(0) + "***" + local.charAt(local.length() - 1);
        return new UserResponse(id, maskedLocal + email.substring(at), name, phoneMasked,
                status, role, emailVerified, lastLoginAt, createdAt, authorities);
    }
}
