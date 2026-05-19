package com.springshop.web.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 전화번호 형식 검증 어노테이션.
 *
 * <p>한국 휴대전화 / 일반전화 / 국제번호 형식을 선택적으로 검증한다.
 * 기본은 한국 휴대전화 (010-XXXX-XXXX 또는 01012345678) 만 허용.
 *
 * @see PhoneNumberValidator
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumberConstraint {

    String message() default "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** 휴대전화 허용 여부. */
    boolean allowMobile() default true;

    /** 일반전화(02, 031 등) 허용 여부. */
    boolean allowLandline() default false;

    /** 국제 형식(+82) 허용 여부. */
    boolean allowInternational() default false;

    /** null/빈 값 허용 여부. */
    boolean allowEmpty() default true;
}
