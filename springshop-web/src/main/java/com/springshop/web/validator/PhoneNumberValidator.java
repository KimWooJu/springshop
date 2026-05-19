package com.springshop.web.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * {@link PhoneNumberConstraint} 검증 로직.
 *
 * <p>지원 패턴:
 * <ul>
 *   <li>휴대전화: 010-XXXX-XXXX, 01012345678</li>
 *   <li>일반전화: 02-XXX-XXXX, 031-XXX-XXXX 등</li>
 *   <li>국제: +82-10-XXXX-XXXX</li>
 * </ul>
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumberConstraint, String> {

    private static final Pattern MOBILE = Pattern.compile("^01[016789]-?\\d{3,4}-?\\d{4}$");
    private static final Pattern LANDLINE = Pattern.compile("^0(2|[3-6][1-5])-?\\d{3,4}-?\\d{4}$");
    private static final Pattern INTERNATIONAL = Pattern.compile("^\\+82-?\\d{1,3}-?\\d{3,4}-?\\d{4}$");

    private boolean allowMobile;
    private boolean allowLandline;
    private boolean allowInternational;
    private boolean allowEmpty;

    @Override
    public void initialize(PhoneNumberConstraint constraint) {
        this.allowMobile = constraint.allowMobile();
        this.allowLandline = constraint.allowLandline();
        this.allowInternational = constraint.allowInternational();
        this.allowEmpty = constraint.allowEmpty();
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.isBlank()) {
            return allowEmpty;
        }
        String normalized = phone.replaceAll("\\s", "");

        if (allowMobile && MOBILE.matcher(normalized).matches()) return true;
        if (allowLandline && LANDLINE.matcher(normalized).matches()) return true;
        if (allowInternational && INTERNATIONAL.matcher(normalized).matches()) return true;

        return false;
    }

    /** 전화번호를 표준 형식(010-1234-5678)으로 정규화한다. */
    public static String normalize(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("010")) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return phone;
    }

    /** 마스킹된 전화번호 (010-1234-****). */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
