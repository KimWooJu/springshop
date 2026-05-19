package com.springshop.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리기.
 * 모든 컨트롤러에서 발생한 예외를 일관된 ErrorResponse로 변환한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * SpringShop 시스템 예외 - 패턴 매칭으로 분류.
     */
    @ExceptionHandler(SpringShopException.class)
    public ResponseEntity<ErrorResponse> handleSpringShopException(
            SpringShopException ex, HttpServletRequest request) {

        int statusCode = switch (ex) {
            case AuthenticationException _ -> 401;
            case AuthorizationException _ -> 403;
            case ResourceNotFoundException _ -> 404;
            case DuplicateResourceException _ -> 409;
            case RateLimitException _ -> 429;
            case ValidationException _ -> 400;
            case PaymentException _ -> ex.getHttpStatus().value();
            case InventoryException _ -> ex.getHttpStatus().value();
            case OrderException _ -> ex.getHttpStatus().value();
            case ExternalServiceException _ -> 502;
            case BusinessException _ -> ex.getHttpStatus().value();
            default -> 500;
        };

        if (statusCode >= 500) {
            log.error("[SpringShopException] code={}, status={}, path={}, message={}",
                    ex.getErrorCodeString(), statusCode, request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("[SpringShopException] code={}, status={}, path={}, message={}",
                    ex.getErrorCodeString(), statusCode, request.getRequestURI(), ex.getMessage());
        }

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(statusCode)
                .code(ex.getErrorCodeString())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        if (ex instanceof ValidationException ve && ve.hasFieldErrors()) {
            body = body.withFieldErrors(
                    ve.getFieldErrors().stream()
                            .map(fe -> new ErrorResponse.FieldErrorDetail(
                                    fe.field(), fe.rejectedValue(), fe.message()))
                            .collect(Collectors.toList())
            );
        }

        HttpHeaders headers = new HttpHeaders();
        if (ex instanceof RateLimitException rle) {
            headers.add("Retry-After", rle.toHeaderValue());
        }

        return new ResponseEntity<>(body, headers, HttpStatus.valueOf(statusCode));
    }

    /**
     * Bean Validation @Valid 실패.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldErrorDetail> fieldErrors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ErrorResponse.FieldErrorDetail(
                    fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()));
        }

        log.warn("[Validation] path={}, errors={}", request.getRequestURI(), fieldErrors.size());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .code(ErrorCode.SYSTEM_VALIDATION_FAILED.getCode())
                .message("입력값 검증에 실패했습니다")
                .path(request.getRequestURI())
                .build()
                .withFieldErrors(fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Path/Query 파라미터 검증 실패.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldErrorDetail> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            fieldErrors.add(new ErrorResponse.FieldErrorDetail(
                    cv.getPropertyPath().toString(),
                    cv.getInvalidValue(),
                    cv.getMessage()));
        }

        log.warn("[ConstraintViolation] path={}, violations={}",
                request.getRequestURI(), fieldErrors.size());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .code(ErrorCode.SYSTEM_VALIDATION_FAILED.getCode())
                .message("파라미터 검증에 실패했습니다")
                .path(request.getRequestURI())
                .build()
                .withFieldErrors(fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("[HttpMessageNotReadable] path={}, message={}",
                request.getRequestURI(), ex.getMessage());

        String message = ex.getCause() instanceof JsonProcessingException
                ? "잘못된 JSON 형식입니다"
                : "요청 본문을 읽을 수 없습니다";

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .code(ErrorCode.SYSTEM_REQUEST_INVALID.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .code(ErrorCode.SYSTEM_REQUEST_INVALID.getCode())
                .message("필수 파라미터 누락: " + ex.getParameterName())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String requiredType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName() : "?";
        String message = "파라미터 타입 불일치: %s (기대 타입: %s)".formatted(
                ex.getName(), requiredType);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .code(ErrorCode.SYSTEM_REQUEST_INVALID.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(405)
                .code(ErrorCode.SYSTEM_METHOD_NOT_ALLOWED.getCode())
                .message("허용되지 않은 메서드: " + ex.getMethod())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(415)
                .code(ErrorCode.SYSTEM_REQUEST_INVALID.getCode())
                .message("지원하지 않는 미디어 타입: " + ex.getContentType())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(404)
                .code(ErrorCode.SYSTEM_NOT_FOUND.getCode())
                .message("리소스를 찾을 수 없습니다: " + ex.getRequestURL())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("[AccessDenied] path={}, message={}",
                request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(403)
                .code(ErrorCode.AUTH_PERMISSION_DENIED.getCode())
                .message("권한이 없습니다")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Catch-all 예외 핸들러.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("[Unhandled] path={}, type={}, message={}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(500)
                .code(ErrorCode.SYSTEM_INTERNAL_ERROR.getCode())
                .message("내부 서버 오류가 발생했습니다")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.internalServerError().body(body);
    }

    /**
     * 응답 본문 record.
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String code,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors,
            Map<String, Object> extra
    ) {
        public ErrorResponse withFieldErrors(List<FieldErrorDetail> errors) {
            return new ErrorResponse(timestamp, status, code, message, path, errors, extra);
        }

        public static Builder builder() {
            return new Builder();
        }

        public record FieldErrorDetail(String field, Object rejectedValue, String message) {}

        public static class Builder {
            private LocalDateTime timestamp;
            private int status;
            private String code;
            private String message;
            private String path;

            public Builder timestamp(LocalDateTime ts) { this.timestamp = ts; return this; }
            public Builder status(int s) { this.status = s; return this; }
            public Builder code(String c) { this.code = c; return this; }
            public Builder message(String m) { this.message = m; return this; }
            public Builder path(String p) { this.path = p; return this; }

            public ErrorResponse build() {
                return new ErrorResponse(timestamp, status, code, message, path, null, null);
            }
        }
    }
}
