package com.springshop.web.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 쿠폰 코드 형식 검증 어노테이션.
 *
 * <p>대문자 영문 + 숫자만 허용. 길이 6~20자.
 * 옵션으로 하이픈/언더스코어 허용 가능.
 */
@Documented
@Constraint(validatedBy = CouponCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CouponCodeConstraint {

    String message() default "쿠폰 코드는 대문자 영문과 숫자 조합으로 6~20자여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int minLength() default 6;

    int maxLength() default 20;

    /** 하이픈(-) 허용. */
    boolean allowHyphen() default false;

    /** 언더스코어(_) 허용. */
    boolean allowUnderscore() default false;

    /** 소문자 허용 (자동 대문자화 후 검증할 때 사용). */
    boolean allowLowercase() default false;
}
