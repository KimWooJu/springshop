package com.springshop.web.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * {@link CouponCodeConstraint} 검증 구현.
 */
public class CouponCodeValidator implements ConstraintValidator<CouponCodeConstraint, String> {

    private int minLength;
    private int maxLength;
    private Pattern pattern;

    @Override
    public void initialize(CouponCodeConstraint constraint) {
        this.minLength = constraint.minLength();
        this.maxLength = constraint.maxLength();

        StringBuilder regex = new StringBuilder("^[A-Z0-9");
        if (constraint.allowLowercase()) regex.append("a-z");
        if (constraint.allowHyphen()) regex.append("\\-");
        if (constraint.allowUnderscore()) regex.append("_");
        regex.append("]{").append(minLength).append(",").append(maxLength).append("}$");

        this.pattern = Pattern.compile(regex.toString());
    }

    @Override
    public boolean isValid(String code, ConstraintValidatorContext context) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.trim();
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            return false;
        }
        return pattern.matcher(normalized).matches();
    }

    /** 외부에서 동일 정책으로 검증 (정규화 포함). */
    public static boolean isValidCode(String code) {
        if (code == null) return false;
        String upper = code.trim().toUpperCase();
        return upper.matches("^[A-Z0-9]{6,20}$");
    }
}
