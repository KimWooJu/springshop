package com.springshop.domain.user;

import java.util.EnumSet;
import java.util.Set;

/**
 * 사용자 역할(권한 그룹) 열거형.
 *
 * <p>역할별로 사용 가능한 권한 집합을 보유하며, {@link #hasPermission(String)}으로
 * 검사한다. 권한 문자열은 "domain:action" 형식 규약을 따른다(예: "order:read",
 * "product:write").</p>
 *
 * @author SpringShop Domain Team
 */
public enum UserRole {

    /** 일반 구매 고객. */
    CUSTOMER(Set.of(
            "order:read", "order:create", "order:cancel:own",
            "product:read", "review:create", "review:update:own",
            "cart:read", "cart:write", "payment:create:own",
            "profile:read:own", "profile:update:own"
    )),

    /** 판매자(셀러). */
    SELLER(Set.of(
            "product:read", "product:create", "product:update:own", "product:delete:own",
            "order:read:seller", "order:ship:own",
            "inventory:read:own", "inventory:update:own",
            "review:read", "review:reply:own",
            "profile:read:own", "profile:update:own"
    )),

    /** 일반 관리자. */
    ADMIN(Set.of(
            "user:read", "user:update", "user:lock",
            "product:read", "product:update", "product:delete",
            "order:read", "order:update", "order:cancel",
            "payment:read", "payment:refund",
            "review:read", "review:moderate",
            "inventory:read", "inventory:update",
            "coupon:read", "coupon:create", "coupon:update",
            "notification:read", "notification:send"
    )),

    /** 시스템 최고 관리자. */
    SUPER_ADMIN(Set.of(
            "*" // 모든 권한
    ));

    private final Set<String> permissions;

    UserRole(Set<String> permissions) {
        this.permissions = EnumSet.noneOf(Permission.class).isEmpty() ? Set.copyOf(permissions) : Set.copyOf(permissions);
    }

    /**
     * 해당 역할이 권한을 가지고 있는지 검사한다.
     * SUPER_ADMIN은 모든 권한("*")을 가진다.
     */
    public boolean hasPermission(String permission) {
        if (permission == null || permission.isBlank()) return false;
        if (this == SUPER_ADMIN) return true;
        if (permissions.contains("*")) return true;
        if (permissions.contains(permission)) return true;
        // 와일드카드 검사: "order:*" 매칭
        int colon = permission.indexOf(':');
        if (colon > 0) {
            String prefix = permission.substring(0, colon) + ":*";
            if (permissions.contains(prefix)) return true;
        }
        return false;
    }

    /**
     * 다른 역할보다 권한 수준이 높은지(또는 같은지) 비교한다.
     * 순서: CUSTOMER < SELLER < ADMIN < SUPER_ADMIN
     */
    public boolean isAtLeast(UserRole other) {
        return this.ordinal() >= other.ordinal();
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    /**
     * 역할의 한국어 표시명을 반환한다.
     */
    public String displayName() {
        return switch (this) {
            case CUSTOMER -> "일반 고객";
            case SELLER -> "판매자";
            case ADMIN -> "관리자";
            case SUPER_ADMIN -> "최고 관리자";
        };
    }

    /**
     * 역할 분류용 내부 마커.
     */
    private enum Permission { /* 명시적 권한 enum은 사용하지 않으나 EnumSet 초기화에 필요 */ }
}
