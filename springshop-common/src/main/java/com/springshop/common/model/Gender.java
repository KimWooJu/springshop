package com.springshop.common.model;

/**
 * 성별 열거형.
 *
 * <p>사용자 프로필에서 사용된다. 미응답 옵션을 기본값으로 두어 개인정보 보호 정책을 따른다.</p>
 */
public enum Gender {

    /** 남성. */
    MALE("남성", "M"),
    /** 여성. */
    FEMALE("여성", "F"),
    /** 기타. */
    OTHER("기타", "O"),
    /** 응답 안함. */
    PREFER_NOT_TO_SAY("응답 안함", "N");

    private final String displayName;
    private final String code;

    Gender(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    /**
     * 한국어 표시명.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 1글자 코드 (DB 컬럼에 저장하기 위한 형태).
     */
    public String getCode() {
        return code;
    }

    /**
     * 코드/이름 문자열로부터 {@code Gender}를 반환한다.
     * 알 수 없는 값은 {@link #PREFER_NOT_TO_SAY}로 폴백한다.
     */
    public static Gender fromCode(String code) {
        if (code == null) {
            return PREFER_NOT_TO_SAY;
        }
        return switch (code.toUpperCase()) {
            case "M", "MALE" -> MALE;
            case "F", "FEMALE" -> FEMALE;
            case "O", "OTHER" -> OTHER;
            default -> PREFER_NOT_TO_SAY;
        };
    }

    /**
     * 공개 가능한 성별인지 여부 (프로필 노출 정책).
     */
    public boolean isPublic() {
        return this != PREFER_NOT_TO_SAY;
    }
}
