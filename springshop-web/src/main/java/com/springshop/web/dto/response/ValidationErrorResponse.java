package com.springshop.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 검증 오류 응답.
 *
 * <p>{@code @Valid} 검증 실패 시 필드별 위반 내역을 모아 반환한다.
 * GlobalExceptionHandler 에서 {@link org.springframework.web.bind.MethodArgumentNotValidException}
 * 처리 시 사용한다.
 */
@Schema(description = "검증 오류 응답")
public record ValidationErrorResponse(
        @Schema(description = "에러 코드", example = "VALIDATION_FAILED")
        String errorCode,

        @Schema(description = "에러 메시지", example = "입력값 검증에 실패했습니다.")
        String message,

        @Schema(description = "필드별 오류 목록")
        List<FieldErrorDetail> errors,

        @Schema(description = "응답 시각")
        LocalDateTime timestamp,

        @Schema(description = "요청 경로")
        String path,

        @Schema(description = "HTTP 상태 코드", example = "400")
        int status
) {

    public ValidationErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
        timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    /**
     * 단일 필드 오류 상세 정보.
     */
    @Schema(description = "필드 오류 상세")
    public record FieldErrorDetail(
            @Schema(description = "필드 이름", example = "email")
            String field,

            @Schema(description = "거부된 입력값", example = "not-an-email")
            Object rejectedValue,

            @Schema(description = "오류 메시지", example = "올바른 이메일 형식이 아닙니다.")
            String message,

            @Schema(description = "위반된 검증 코드", example = "Email")
            String code
    ) {
        public static FieldErrorDetail from(FieldError fe) {
            return new FieldErrorDetail(
                    fe.getField(),
                    fe.getRejectedValue(),
                    fe.getDefaultMessage(),
                    fe.getCode()
            );
        }
    }

    /** BindingResult로부터 검증 오류 응답을 만든다. */
    public static ValidationErrorResponse from(BindingResult bindingResult, String path) {
        List<FieldErrorDetail> errs = bindingResult.getFieldErrors().stream()
                .map(FieldErrorDetail::from)
                .collect(Collectors.toList());
        return new ValidationErrorResponse(
                "VALIDATION_FAILED",
                "입력값 검증에 실패했습니다.",
                errs,
                LocalDateTime.now(),
                path,
                400
        );
    }

    /** 임의 오류 목록으로 만든다. */
    public static ValidationErrorResponse of(List<FieldErrorDetail> errors, String path) {
        return new ValidationErrorResponse(
                "VALIDATION_FAILED",
                "입력값 검증에 실패했습니다.",
                errors,
                LocalDateTime.now(),
                path,
                400
        );
    }

    /** 단일 필드 오류로 만든다. */
    public static ValidationErrorResponse single(String field, Object rejected,
                                                 String message, String path) {
        FieldErrorDetail fe = new FieldErrorDetail(field, rejected, message, "Custom");
        return of(List.of(fe), path);
    }
}
