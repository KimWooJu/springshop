package com.springshop.web.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * {@link PasswordConstraint} 의 실제 검증 로직.
 *
 * <p>다음을 검증한다:
 * <ul>
 *   <li>최소/최대 길이</li>
 *   <li>대문자/소문자/숫자/특수문자 포함</li>
 *   <li>공백 문자 허용 여부</li>
 *   <li>연속 동일 문자 횟수 제한</li>
 *   <li>금지 단어 포함 여부</li>
 * </ul>
 *
 * <p>실패 시 모든 사유를 누적하여 사용자에게 안내한다.
 */
public class PasswordValidator implements ConstraintValidator<PasswordConstraint, String> {

    private static final Logger log = LoggerFactory.getLogger(PasswordValidator.class);

    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*[0-9].*");
    private static final Pattern SPECIAL = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    private static final Pattern WHITESPACE = Pattern.compile(".*\\s.*");

    private int minLength;
    private int maxLength;
    private boolean requireUpperCase;
    private boolean requireLowerCase;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    private boolean allowWhitespace;
    private int maxConsecutiveSameChar;
    private String[] forbiddenWords;

    @Override
    public void initialize(PasswordConstraint constraint) {
        this.minLength = constraint.minLength();
        this.maxLength = constraint.maxLength();
        this.requireUpperCase = constraint.requireUpperCase();
        this.requireLowerCase = constraint.requireLowerCase();
        this.requireDigit = constraint.requireDigit();
        this.requireSpecialChar = constraint.requireSpecialChar();
        this.allowWhitespace = constraint.allowWhitespace();
        this.maxConsecutiveSameChar = constraint.maxConsecutiveSameChar();
        this.forbiddenWords = constraint.forbiddenWords();
        log.debug("PasswordValidator initialized: min={}, max={}, upper={}, lower={}, digit={}, special={}",
                minLength, maxLength, requireUpperCase, requireLowerCase, requireDigit, requireSpecialChar);
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        context.disableDefaultConstraintViolation();
        boolean valid = true;
        StringBuilder reasons = new StringBuilder();

        if (password.length() < minLength) {
            reasons.append("최소 ").append(minLength).append("자 이상이어야 합니다. ");
            valid = false;
        }
        if (password.length() > maxLength) {
            reasons.append("최대 ").append(maxLength).append("자 이하여야 합니다. ");
            valid = false;
        }
        if (requireUpperCase && !UPPER.matcher(password).matches()) {
            reasons.append("대문자를 포함해야 합니다. ");
            valid = false;
        }
        if (requireLowerCase && !LOWER.matcher(password).matches()) {
            reasons.append("소문자를 포함해야 합니다. ");
            valid = false;
        }
        if (requireDigit && !DIGIT.matcher(password).matches()) {
            reasons.append("숫자를 포함해야 합니다. ");
            valid = false;
        }
        if (requireSpecialChar && !SPECIAL.matcher(password).matches()) {
            reasons.append("특수문자(!@#$ 등)를 포함해야 합니다. ");
            valid = false;
        }
        if (!allowWhitespace && WHITESPACE.matcher(password).matches()) {
            reasons.append("공백 문자는 허용되지 않습니다. ");
            valid = false;
        }
        if (hasTooManyConsecutiveChars(password)) {
            reasons.append("동일 문자를 ").append(maxConsecutiveSameChar)
                    .append("회 초과로 연속 사용할 수 없습니다. ");
            valid = false;
        }
        if (containsForbiddenWord(password)) {
            reasons.append("취약한 단어(예: password)를 포함할 수 없습니다. ");
            valid = false;
        }

        if (!valid) {
            context.buildConstraintViolationWithTemplate(reasons.toString().trim())
                    .addConstraintViolation();
        }
        return valid;
    }

    private boolean hasTooManyConsecutiveChars(String s) {
        if (maxConsecutiveSameChar <= 0) return false;
        int count = 1;
        char prev = '\0';
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == prev) {
                count++;
                if (count > maxConsecutiveSameChar) return true;
            } else {
                count = 1;
                prev = c;
            }
        }
        return false;
    }

    private boolean containsForbiddenWord(String password) {
        if (forbiddenWords == null || forbiddenWords.length == 0) return false;
        String lower = password.toLowerCase();
        for (String word : forbiddenWords) {
            if (word != null && !word.isBlank() && lower.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /** 외부에서 비밀번호 강도를 점수화하는 유틸 (0-100). */
    public static int strengthScore(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 10;
        if (UPPER.matcher(password).matches()) score += 15;
        if (LOWER.matcher(password).matches()) score += 15;
        if (DIGIT.matcher(password).matches()) score += 15;
        if (SPECIAL.matcher(password).matches()) score += 25;
        return Math.min(100, score);
    }
}
