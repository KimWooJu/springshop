package com.springshop.web.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 업로드 파일 검증 어노테이션.
 *
 * <p>{@link org.springframework.web.multipart.MultipartFile} 필드/파라미터에 부착하여
 * 파일 크기, 확장자, MIME 타입을 검증한다.
 */
@Documented
@Constraint(validatedBy = FileUploadValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FileUploadConstraint {

    String message() default "업로드 파일 조건이 맞지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** 최대 파일 크기 (bytes). 기본 10MB. */
    long maxSizeBytes() default 10L * 1024 * 1024;

    /** 허용 확장자 (소문자, 점 제외). 빈 배열=무제한. */
    String[] allowedExtensions() default {"jpg", "jpeg", "png", "gif", "webp", "pdf"};

    /** 허용 MIME 타입. 빈 배열=무제한. */
    String[] allowedMimeTypes() default {};

    /** null/빈 파일 허용 여부. */
    boolean allowEmpty() default false;

    /** 파일명 길이 최대. */
    int maxFilenameLength() default 255;
}
