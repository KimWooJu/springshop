package com.springshop.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 통합 API 응답 인터페이스.
 *
 * <p>Java 25 sealed interface 패턴을 사용하여 성공/실패/페이징 응답을
 * 타입 안전하게 표현한다. 클라이언트는 {@code switch} 패턴 매칭으로
 * 응답 유형을 분기 처리할 수 있다.
 *
 * <pre>
 *   switch (apiResponse) {
 *       case Success&lt;UserResponse&gt; s   -&gt; render(s.data());
 *       case Failure&lt;UserResponse&gt; f   -&gt; showError(f.message());
 *       case Paginated&lt;UserResponse&gt; p -&gt; renderList(p.content());
 *   }
 * </pre>
 *
 * @param <T> 응답 데이터 타입
 */
@Schema(description = "통합 API 응답 (성공/실패/페이징)")
public sealed interface ApiResponse<T>
        permits ApiResponse.Success, ApiResponse.Failure, ApiResponse.Paginated {

    /** 성공 응답 타입 식별자. */
    String SUCCESS_CODE = "OK";
    /** 일반 실패 응답 타입 식별자. */
    String GENERIC_FAILURE_CODE = "INTERNAL_ERROR";

    /**
     * 성공 응답.
     */
    @Schema(description = "성공 응답")
    record Success<T>(
            @Schema(description = "응답 데이터") T data,
            @Schema(description = "메시지") String message,
            @Schema(description = "응답 시각") LocalDateTime timestamp,
            @Schema(description = "트레이스 ID") String traceId
    ) implements ApiResponse<T> {

        public static <T> Success<T> of(T data) {
            return new Success<>(data, "성공", LocalDateTime.now(), null);
        }

        public static <T> Success<T> of(T data, String message) {
            return new Success<>(data, message, LocalDateTime.now(), null);
        }

        public static <T> Success<T> withTrace(T data, String traceId) {
            return new Success<>(data, "성공", LocalDateTime.now(), traceId);
        }

        public static Success<Void> empty() {
            return new Success<>(null, "성공", LocalDateTime.now(), null);
        }
    }

    /**
     * 실패 응답.
     */
    @Schema(description = "실패 응답")
    record Failure<T>(
            @Schema(description = "에러 코드") String errorCode,
            @Schema(description = "에러 메시지") String message,
            @Schema(description = "상세 사유 목록") List<String> details,
            @Schema(description = "응답 시각") LocalDateTime timestamp,
            @Schema(description = "발생 경로") String path
    ) implements ApiResponse<T> {

        public Failure {
            details = details == null ? List.of() : List.copyOf(details);
        }

        public static <T> Failure<T> of(String code, String message) {
            return new Failure<>(code, message, List.of(), LocalDateTime.now(), null);
        }

        public static <T> Failure<T> of(String code, String message, List<String> details) {
            return new Failure<>(code, message, details, LocalDateTime.now(), null);
        }

        public static <T> Failure<T> at(String code, String message, String path) {
            return new Failure<>(code, message, List.of(), LocalDateTime.now(), path);
        }
    }

    /**
     * 페이징 응답.
     */
    @Schema(description = "페이징 응답")
    record Paginated<T>(
            @Schema(description = "페이지 데이터") List<T> content,
            @Schema(description = "총 요소 수") long totalElements,
            @Schema(description = "총 페이지 수") int totalPages,
            @Schema(description = "현재 페이지(0-base)") int page,
            @Schema(description = "페이지 크기") int size,
            @Schema(description = "추가 메타데이터") Map<String, Object> meta
    ) implements ApiResponse<T> {

        public Paginated {
            content = content == null ? List.of() : List.copyOf(content);
            meta = meta == null ? Map.of() : Map.copyOf(meta);
        }

        public static <T> Paginated<T> of(List<T> content, long total, int page, int size) {
            int pages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
            return new Paginated<>(content, total, pages, page, size, Map.of());
        }
    }

    /** 응답이 성공인지 여부를 패턴 매칭으로 판정한다. */
    default boolean isSuccess() {
        return switch (this) {
            case Success<T> s -> true;
            case Failure<T> f -> false;
            case Paginated<T> p -> true;
        };
    }
}
