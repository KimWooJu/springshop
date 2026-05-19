package com.springshop.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 에러 응답 DTO.
 *
 * <p>RFC 7807 Problem Details를 참고하여 errorCode/message/details/path/timestamp를 노출한다.
 * GlobalExceptionHandler 에서 예외 종류에 따라 적절한 인스턴스를 생성한다.
 */
@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "USER_NOT_FOUND")
        String errorCode,

        @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
        String message,

        @Schema(description = "상세 사유 목록")
        List<String> details,

        @Schema(description = "응답 시각")
        LocalDateTime timestamp,

        @Schema(description = "요청 경로", example = "/api/v1/users/123")
        String path,

        @Schema(description = "HTTP 상태 코드", example = "404")
        int status,

        @Schema(description = "트레이스 ID")
        String traceId
) {

    public ErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
        timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    /** 단순 메시지 기반 에러 응답. */
    public static ErrorResponse of(String errorCode, String message, int status) {
        return new ErrorResponse(errorCode, message, List.of(), LocalDateTime.now(), null, status, null);
    }

    /** 경로를 포함한 에러 응답. */
    public static ErrorResponse withPath(String errorCode, String message, int status, String path) {
        return new ErrorResponse(errorCode, message, List.of(), LocalDateTime.now(), path, status, null);
    }

    /** 상세 사유를 포함한 에러 응답. */
    public static ErrorResponse withDetails(String errorCode, String message, int status,
                                            String path, List<String> details) {
        return new ErrorResponse(errorCode, message, details, LocalDateTime.now(), path, status, null);
    }

    /** 트레이스 ID를 포함한 에러 응답. */
    public static ErrorResponse traced(String errorCode, String message, int status,
                                       String path, String traceId) {
        return new ErrorResponse(errorCode, message, List.of(), LocalDateTime.now(), path, status, traceId);
    }

    /** 클라이언트 입력 오류(400) 빠른 생성 헬퍼. */
    public static ErrorResponse badRequest(String message, String path) {
        return withPath("BAD_REQUEST", message, 400, path);
    }

    /** 권한 부족(403) 빠른 생성 헬퍼. */
    public static ErrorResponse forbidden(String path) {
        return withPath("FORBIDDEN", "접근 권한이 없습니다.", 403, path);
    }

    /** 미인증(401) 빠른 생성 헬퍼. */
    public static ErrorResponse unauthorized(String path) {
        return withPath("UNAUTHORIZED", "인증이 필요합니다.", 401, path);
    }

    /** 리소스 없음(404) 빠른 생성 헬퍼. */
    public static ErrorResponse notFound(String resource, String path) {
        return withPath("NOT_FOUND", resource + " 을(를) 찾을 수 없습니다.", 404, path);
    }

    /** 충돌(409) 빠른 생성 헬퍼. */
    public static ErrorResponse conflict(String message, String path) {
        return withPath("CONFLICT", message, 409, path);
    }

    /** 서버 오류(500) 빠른 생성 헬퍼. */
    public static ErrorResponse serverError(String path, String traceId) {
        return traced("INTERNAL_ERROR", "서버 오류가 발생했습니다.", 500, path, traceId);
    }
}
