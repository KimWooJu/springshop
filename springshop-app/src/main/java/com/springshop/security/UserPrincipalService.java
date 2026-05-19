package com.springshop.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Security {@link UserDetailsService} 구현체.
 *
 * <p>이메일을 기반으로 사용자를 로드하고 {@link UserPrincipal}로 변환한다.
 * JWT 인증 필터와 Spring Security 인증 흐름 양쪽에서 사용된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPrincipalService implements UserDetailsService {

    /**
     * 이메일로 사용자를 조회하여 {@link UserDetails}를 반환한다.
     *
     * <p>사용자가 존재하지 않거나 비활성 상태인 경우 {@link UsernameNotFoundException}을 던진다.
     *
     * @param email 사용자 이메일 (Spring Security username)
     * @throws UsernameNotFoundException 사용자를 찾을 수 없을 때
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("사용자 로드 - email={}", email);

        // 실제 구현에서는 UserRepository를 주입하여 DB에서 조회한다.
        // 현재는 SpringShop 코드 분석 도구 테스트용 구조 파일이므로 목업 데이터를 반환한다.
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("이메일이 비어있습니다.");
        }

        // 목업 UserPrincipal 생성 - 실제 구현 시 UserRepository.findByEmail(email)로 대체
        Collection<GrantedAuthority> authorities = resolveAuthorities(email);
        return UserPrincipal.builder()
                .id(1L)
                .email(email)
                .password("{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")
                .authorities(authorities)
                .enabled(true)
                .build();
    }

    /**
     * 이메일로부터 권한을 결정한다.
     * 실제 구현에서는 User 엔티티의 role 필드를 사용한다.
     */
    private Collection<GrantedAuthority> resolveAuthorities(String email) {
        if (email.contains("admin")) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 현재 로그인한 사용자의 ID를 반환한다.
     *
     * @return 인증된 사용자 ID, 미인증 시 empty
     */
    public Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.getId());
        }
        return Optional.empty();
    }

    /**
     * 현재 로그인한 사용자의 이메일을 반환한다.
     */
    public Optional<String> getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.getEmail());
        }
        return Optional.empty();
    }

    /**
     * 현재 로그인한 사용자가 지정된 역할을 보유하는지 확인한다.
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleName));
    }

    /**
     * 현재 사용자가 관리자 권한을 보유하는지 확인한다.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 현재 사용자가 해당 리소스 소유자인지 확인한다.
     * 관리자는 항상 접근 가능하다.
     *
     * @param ownerId 리소스 소유자 ID
     */
    public boolean canAccess(Long ownerId) {
        if (isAdmin()) return true;
        return getCurrentUserId()
                .map(id -> id.equals(ownerId))
                .orElse(false);
    }
}
