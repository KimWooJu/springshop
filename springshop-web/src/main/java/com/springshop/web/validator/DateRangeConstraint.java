package com.springshop.web.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 기간 범위 검증 어노테이션 (클래스 레벨).
 *
 * <p>대상 record/클래스는 다음 형태의 접근자를 가져야 한다:
 * <ul>
 *   <li>{@code LocalDate startDate()}</li>
 *   <li>{@code LocalDate endDate()}</li>
 * </ul>
 *
 * <p>{@code startDate} 가 {@code endDate} 보다 늦으면 검증 실패한다.
 * 최대 허용 일수 옵션으로 너무 긴 조회 기간을 제한할 수 있다.
 */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateRangeConstraint {

    String message() default "종료일은 시작일보다 이후여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** startDate 와 endDate 가 같은 날 허용 여부. */
    boolean allowSameDate() default true;

    /** 최대 허용 일수 (0=무제한). */
    int maxDays() default 0;

    /** 미래 일자 허용 여부. */
    boolean allowFuture() default true;
}
