package com.springshop.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 사용자 프로필 상세 응답.
 *
 * <p>마이페이지 화면에서 사용되는 확장 프로필 정보. 닉네임/생일/성별/자기소개 등
 * UserResponse 보다 더 풍부한 필드를 포함한다.
 */
@Schema(description = "사용자 프로필 상세 응답")
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "1024")
        Long userId,

        @Schema(description = "닉네임", example = "shopper-7")
        String nickname,

        @Schema(description = "한 줄 소개")
        String bio,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "커버 이미지 URL")
        String coverImageUrl,

        @Schema(description = "생년월일")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate birthDate,

        @Schema(description = "성별", example = "M", allowableValues = {"M", "F", "N"})
        String gender,

        @Schema(description = "직업")
        String occupation,

        @Schema(description = "관심 카테고리 슬러그 목록")
        java.util.List<String> interests,

        @Schema(description = "주소 록 (기본 주소만)")
        UserAddressResponse defaultAddress,

        @Schema(description = "포인트 잔액", example = "12500")
        long pointBalance,

        @Schema(description = "회원 등급", example = "GOLD",
                allowableValues = {"BRONZE", "SILVER", "GOLD", "PLATINUM", "VIP"})
        String memberGrade,

        @Schema(description = "누적 구매 횟수", example = "47")
        int totalOrderCount,

        @Schema(description = "누적 구매 금액", example = "1280500")
        long totalSpentAmount,

        @Schema(description = "위시리스트 항목 수", example = "12")
        int wishlistCount,

        @Schema(description = "마지막 활동 시간")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastActivityAt,

        @Schema(description = "사용자 설정값 맵")
        Map<String, Object> preferences
) {

    public UserProfileResponse {
        interests = interests == null ? java.util.List.of() : java.util.List.copyOf(interests);
        preferences = preferences == null ? Map.of() : Map.copyOf(preferences);
    }

    /** 미니멀 응답 생성 헬퍼. */
    public static UserProfileResponse minimal(Long userId, String nickname, String profileImageUrl) {
        return new UserProfileResponse(
                userId, nickname, null, profileImageUrl, null, null, null, null,
                java.util.List.of(), null, 0L, "BRONZE", 0, 0L, 0, null, Map.of()
        );
    }

    /** 등급 갱신된 신규 인스턴스 반환. */
    public UserProfileResponse withGrade(String newGrade) {
        return new UserProfileResponse(
                userId, nickname, bio, profileImageUrl, coverImageUrl, birthDate, gender, occupation,
                interests, defaultAddress, pointBalance, newGrade, totalOrderCount,
                totalSpentAmount, wishlistCount, lastActivityAt, preferences
        );
    }
}
