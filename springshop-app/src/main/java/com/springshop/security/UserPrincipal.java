package com.springshop.security;

import com.springshop.common.constant.SecurityConstants;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Spring Security {@link UserDetails} 구현체.
 *
 * <p>JWT 토큰과 SecurityContext에 저장되는 사용자 인증 주체이며,
 * 도메인 User 엔티티와 분리하여 보안 컨텍스트에서만 필요한 최소 필드를 보유한다.</p>
 */
@Getter
@Builder
public class UserPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String password;
    private final String name;
    private final String role;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean accountNonExpired;
    private final boolean credentialsNonExpired;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null || role.isBlank()) {
            return Collections.emptyList();
        }
        String authority = role.startsWith(SecurityConstants.ROLE_PREFIX)
            ? role
            : SecurityConstants.ROLE_PREFIX + role;
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 지정한 역할 보유 여부.
     * 입력값과 내부 역할 모두 {@code ROLE_} 접두사를 정규화하여 비교한다.
     */
    public boolean hasRole(String roleName) {
        if (role == null || roleName == null) {
            return false;
        }
        String normalized = roleName.startsWith(SecurityConstants.ROLE_PREFIX)
            ? roleName.substring(SecurityConstants.ROLE_PREFIX.length())
            : roleName;
        String myRole = role.startsWith(SecurityConstants.ROLE_PREFIX)
            ? role.substring(SecurityConstants.ROLE_PREFIX.length())
            : role;
        return myRole.equalsIgnoreCase(normalized);
    }

    /**
     * 관리자 여부 단축 메서드.
     */
    public boolean isAdmin() {
        return hasRole(SecurityConstants.ROLE_ADMIN)
            || hasRole(SecurityConstants.ROLE_SUPER_ADMIN);
    }

    /**
     * 익명 사용자 인스턴스를 생성한다 (테스트/공개 엔드포인트용).
     */
    public static UserPrincipal anonymous() {
        return UserPrincipal.builder()
            .id(0L)
            .email("anonymous@springshop.local")
            .name("anonymous")
            .role(SecurityConstants.ROLE_USER)
            .enabled(true)
            .accountNonLocked(true)
            .accountNonExpired(true)
            .credentialsNonExpired(true)
            .build();
    }

    /**
     * 단순 빌더 헬퍼.
     */
    public static UserPrincipal of(Long id, String email, String role) {
        return UserPrincipal.builder()
            .id(id)
            .email(email)
            .role(role)
            .enabled(true)
            .accountNonLocked(true)
            .accountNonExpired(true)
            .credentialsNonExpired(true)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "UserPrincipal{id=%d, email='%s', role='%s'}".formatted(id, email, role);
    }
}
