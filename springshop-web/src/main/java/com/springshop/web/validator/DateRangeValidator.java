package com.springshop.web.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * {@link DateRangeConstraint} 검증 구현 (클래스 레벨).
 *
 * <p>리플렉션으로 {@code startDate()}, {@code endDate()} 접근자를 호출하여 검증한다.
 * record / POJO 모두 지원.
 */
public class DateRangeValidator implements ConstraintValidator<DateRangeConstraint, Object> {

    private static final Logger log = LoggerFactory.getLogger(DateRangeValidator.class);

    private boolean allowSameDate;
    private int maxDays;
    private boolean allowFuture;

    @Override
    public void initialize(DateRangeConstraint constraint) {
        this.allowSameDate = constraint.allowSameDate();
        this.maxDays = constraint.maxDays();
        this.allowFuture = constraint.allowFuture();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) return true;
        try {
            LocalDate start = invokeDate(obj, "startDate");
            LocalDate end = invokeDate(obj, "endDate");
            if (start == null || end == null) return true;

            context.disableDefaultConstraintViolation();

            if (end.isBefore(start)) {
                addViolation(context, "종료일이 시작일보다 앞섭니다.");
                return false;
            }
            if (!allowSameDate && end.equals(start)) {
                addViolation(context, "종료일과 시작일이 동일할 수 없습니다.");
                return false;
            }
            if (!allowFuture && (start.isAfter(LocalDate.now()) || end.isAfter(LocalDate.now()))) {
                addViolation(context, "미래 일자는 허용되지 않습니다.");
                return false;
            }
            if (maxDays > 0) {
                long days = ChronoUnit.DAYS.between(start, end) + 1;
                if (days > maxDays) {
                    addViolation(context, "기간이 너무 깁니다. (최대 " + maxDays + "일)");
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("DateRangeValidator 호출 중 오류: {}", e.getMessage());
            return true; // 검증 불가 시 통과 (다른 validator 가 잡도록)
        }
    }

    private LocalDate invokeDate(Object obj, String methodName) throws Exception {
        Method method = obj.getClass().getMethod(methodName);
        Object value = method.invoke(obj);
        return (value instanceof LocalDate ld) ? ld : null;
    }

    private void addViolation(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
