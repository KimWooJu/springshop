package com.springshop.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Spring Security 컨텍스트 유틸리티.
 *
 * <p>현재 인증된 사용자 정보 조회, 권한 확인, 리소스 접근 제어를 위한
 * 정적/인스턴스 메서드를 제공한다.
 *
 * <p>Java 25 sealed interface {@link ResourceAction}으로 타입 안전한
 * 권한 체계를 구현한다.
 */
@Component
@Slf4j
public class SecurityUtils {

    /**
     * 리소스 작업 유형 — 권한 체계의 기반.
     * Pattern matching switch로 각 작업에 필요한 권한을 결정한다.
     */
    public sealed interface ResourceAction
            permits ResourceAction.Read, ResourceAction.Write,
                    ResourceAction.Delete, ResourceAction.Admin {

        /** 조회 작업 */
        record Read(String resource) implements ResourceAction {}

        /** 생성/수정 작업 */
        record Write(String resource, Long ownerId) implements ResourceAction {}

        /** 삭제 작업 */
        record Delete(String resource, Long ownerId) implements ResourceAction {}

        /** 관리자 전용 작업 */
        record Admin(String resource) implements ResourceAction {}
    }

    /**
     * 현재 인증 여부를 반환한다.
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    /**
     * 현재 로그인 사용자 ID를 반환한다.
     *
     * @return 인증된 경우 사용자 ID, 미인증 시 empty
     */
    public Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.getId());
        }
        return Optional.empty();
    }

    /**
     * 현재 로그인 사용자 ID를 반환한다. 미인증 시 예외를 던진다.
     *
     * @throws IllegalStateException 인증되지 않은 상태
     */
    public Long requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("인증되지 않은 요청입니다."));
    }

    /**
     * 현재 사용자 이메일을 반환한다.
     */
    public Optional<String> getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();
        if (auth.getPrincipal() instanceof UserPrincipal p) return Optional.of(p.getEmail());
        return Optional.empty();
    }

    /**
     * 현재 사용자의 권한 목록을 반환한다.
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getAuthorities() : List.of();
    }

    /**
     * 지정된 역할 보유 여부를 확인한다.
     *
     * @param role 역할 (ROLE_ 접두사 없어도 됨)
     */
    public boolean hasRole(String role) {
        String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleName));
    }

    /**
     * 관리자 여부를 확인한다.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 슈퍼 관리자 여부를 확인한다.
     */
    public boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    /**
     * 리소스 작업 가능 여부를 패턴 매칭으로 결정한다.
     *
     * <pre>
     *   // 사용 예:
     *   boolean canWrite = securityUtils.canPerform(
     *       new ResourceAction.Write("order", order.getUserId()));
     * </pre>
     *
     * @param action 수행하려는 작업
     * @return 작업 허용 여부
     */
    public boolean canPerform(ResourceAction action) {
        if (!isAuthenticated()) return false;

        return switch (action) {
            case ResourceAction.Read r -> isAuthenticated();
            case ResourceAction.Write w -> isAdmin() || isOwner(w.ownerId());
            case ResourceAction.Delete d -> isAdmin() || isOwner(d.ownerId());
            case ResourceAction.Admin a -> isAdmin();
        };
    }

    /**
     * 현재 사용자가 지정 리소스의 소유자인지 확인한다.
     * 관리자는 모든 리소스에 접근 가능하다.
     */
    public boolean canAccess(Long resourceOwnerId) {
        if (isAdmin()) return true;
        return getCurrentUserId()
                .map(id -> id.equals(resourceOwnerId))
                .orElse(false);
    }

    /**
     * 지정한 사용자 ID가 현재 사용자와 동일한지 확인한다.
     */
    private boolean isOwner(Long ownerId) {
        return getCurrentUserId()
                .map(id -> id.equals(ownerId))
                .orElse(false);
    }

    /**
     * 임시로 다른 사용자 컨텍스트에서 작업을 실행한다.
     * 내부 시스템 작업(스케줄러, 이벤트 핸들러)에서 사용한다.
     *
     * @param userId     임시로 사용할 사용자 ID
     * @param userEmail  임시로 사용할 사용자 이메일
     * @param task       실행할 작업
     * @param <T>        반환 타입
     */
    public <T> T runAs(Long userId, String userEmail, Callable<T> task) {
        Authentication original = SecurityContextHolder.getContext().getAuthentication();
        try {
            var principal = UserPrincipal.builder()
                    .id(userId)
                    .email(userEmail)
                    .password("")
                    .role("ROLE_USER")
                    .enabled(true)
                    .build();
            var auth = new org.springframework.security.authentication
                    .UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            return task.call();
        } catch (Exception ex) {
            log.error("runAs 실행 중 오류 - userId={}, error={}", userId, ex.getMessage(), ex);
            throw new RuntimeException("runAs 실행 실패", ex);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(original);
        }
    }
}
