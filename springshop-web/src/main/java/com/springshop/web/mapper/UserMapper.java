package com.springshop.web.mapper;

import com.springshop.domain.user.User;
import com.springshop.domain.user.UserAddress;
import com.springshop.domain.user.UserProfile;
import com.springshop.domain.user.UserRole;
import com.springshop.domain.user.UserStatus;
import com.springshop.web.dto.response.UserAddressResponse;
import com.springshop.web.dto.response.UserProfileResponse;
import com.springshop.web.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 도메인 → 응답 DTO 매퍼.
 *
 * <p>MapStruct 대신 명시적 매핑을 사용하여 변환 로직을 투명하게 유지한다.
 * 마스킹, null 안전 처리, 권한 목록 변환 등을 한 곳에서 수행한다.
 *
 * <p>관리자용 / 일반 사용자용 응답을 분리해 PII(개인 식별 정보) 노출을 제어한다.
 */
@Component
public class UserMapper {

    /** 일반 사용자용 응답 — 마스킹 처리됨. */
    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                emailString(user),
                user.getName(),
                maskPhone(phoneString(user)),
                statusLabel(user.getStatus()),
                roleLabel(user.getRole()),
                true,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                List.of("ROLE_" + roleLabel(user.getRole()))
        );
    }

    /** 관리자용 응답 — 마스킹 없이 모든 정보 노출. */
    public UserResponse toAdminResponse(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                emailString(user),
                user.getName(),
                phoneString(user),
                statusLabel(user.getStatus()),
                roleLabel(user.getRole()),
                true,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                List.of("ROLE_" + roleLabel(user.getRole()), "ROLE_VIEWED_BY_ADMIN")
        );
    }

    /** User + UserProfile → UserProfileResponse. */
    public UserProfileResponse toProfileResponse(User user, UserProfile profile) {
        if (user == null) return null;
        return new UserProfileResponse(
                user.getId(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getBio() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                null,
                profile != null ? profile.getBirthDate() : null,
                profile != null ? profile.getGender() : null,
                null,
                List.of(),
                null,
                0L,
                "BRONZE",
                0,
                0L,
                0,
                user.getLastLoginAt(),
                Map.of()
        );
    }

    /** UserAddress → UserAddressResponse. */
    public UserAddressResponse toAddressResponse(UserAddress address) {
        if (address == null) return null;
        return new UserAddressResponse(
                address.getId(),
                address.getUser() != null ? address.getUser().getId() : null,
                address.getAddressName(),
                address.getRecipientName(),
                Optional.ofNullable(address.getRecipientPhone()).map(Object::toString).orElse(null),
                null,
                Optional.ofNullable(address.getAddress()).map(Object::toString).orElse(null),
                null,
                null,
                null,
                "KR",
                address.isDefault(),
                address.getDeliveryMemo(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }

    public List<UserResponse> toResponseList(List<User> users) {
        return users == null
                ? List.of()
                : users.stream().map(this::toResponse).toList();
    }

    public List<UserResponse> toAdminResponseList(List<User> users) {
        return users == null
                ? List.of()
                : users.stream().map(this::toAdminResponse).toList();
    }

    public List<UserAddressResponse> toAddressResponseList(List<UserAddress> addresses) {
        return addresses == null
                ? List.of()
                : addresses.stream().map(this::toAddressResponse).toList();
    }

    // ----------------------------------------------------------
    // 헬퍼 메서드
    // ----------------------------------------------------------

    private String statusLabel(UserStatus status) {
        return status == null ? "UNKNOWN" : status.getClass().getSimpleName();
    }

    private String roleLabel(UserRole role) {
        return role == null ? "USER" : role.name();
    }

    private String emailString(User user) {
        try {
            return Optional.ofNullable(user.getEmail()).map(Object::toString).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String phoneString(User user) {
        try {
            return Optional.ofNullable(user.getPhone()).map(Object::toString).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, phone.length() - 4) + "****";
    }

    /** 응답에 트레이스 ID를 부여한 사본 생성. */
    public UserResponse withRefreshTimestamp(UserResponse base) {
        if (base == null) return null;
        return new UserResponse(
                base.id(), base.email(), base.name(), base.phoneMasked(),
                base.status(), base.role(), base.emailVerified(),
                LocalDateTime.now(), base.createdAt(), base.authorities()
        );
    }
}
