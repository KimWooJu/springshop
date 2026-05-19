package com.springshop.web.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비밀번호 정책 검증 어노테이션.
 *
 * <p>대문자, 소문자, 숫자, 특수문자 포함 및 최소/최대 길이 제약을 강제한다.
 * 옵션을 조정해 각 정책을 끄거나 켤 수 있다.
 *
 * <pre>
 *   public record CreateUserRequest(
 *       @PasswordConstraint(minLength = 10, requireSpecialChar = false)
 *       String password) {}
 * </pre>
 *
 * @see PasswordValidator
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordConstraint {

    String message() default
            "비밀번호는 대문자, 소문자, 숫자, 특수문자를 포함한 8~100자여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** 최소 길이. */
    int minLength() default 8;

    /** 최대 길이. */
    int maxLength() default 100;

    /** 대문자 포함 여부. */
    boolean requireUpperCase() default true;

    /** 소문자 포함 여부. */
    boolean requireLowerCase() default true;

    /** 숫자 포함 여부. */
    boolean requireDigit() default true;

    /** 특수문자 포함 여부. */
    boolean requireSpecialChar() default true;

    /** 공백 문자 허용 여부 (기본 비허용). */
    boolean allowWhitespace() default false;

    /** 동일 문자 연속 사용 최대 횟수 (예: aaaa 금지). */
    int maxConsecutiveSameChar() default 3;

    /** 사용 금지 단어 (기본: password, 123456, qwerty 등). */
    String[] forbiddenWords() default {"password", "12345678", "qwerty", "admin", "letmein"};
}
