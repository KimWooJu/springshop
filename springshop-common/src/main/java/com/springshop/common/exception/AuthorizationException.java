package com.springshop.common.exception;

import java.io.Serial;

/**
 * 권한 부족 (403 Forbidden) 시 발생하는 예외.
 */
public class AuthorizationException extends SpringShopException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String requiredRole;
    private final String resource;
    private final String action;

    public AuthorizationException() {
        super(ErrorCode.AUTH_PERMISSION_DENIED);
        this.requiredRole = null;
        this.resource = null;
        this.action = null;
    }

    public AuthorizationException(String message) {
        super(ErrorCode.AUTH_PERMISSION_DENIED, message);
        this.requiredRole = null;
        this.resource = null;
        this.action = null;
    }

    public AuthorizationException(ErrorCode errorCode) {
        super(errorCode);
        this.requiredRole = null;
        this.resource = null;
        this.action = null;
    }

    public AuthorizationException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
        this.requiredRole = null;
        this.resource = null;
        this.action = null;
    }

    public AuthorizationException(String requiredRole, String resource, String action) {
        super(ErrorCode.AUTH_PERMISSION_DENIED,
                "권한 부족: role=%s, resource=%s, action=%s".formatted(requiredRole, resource, action));
        this.requiredRole = requiredRole;
        this.resource = resource;
        this.action = action;
    }

    public static AuthorizationException permissionDenied() {
        return new AuthorizationException();
    }

    public static AuthorizationException requireRole(String role) {
        return new AuthorizationException(ErrorCode.AUTH_ROLE_REQUIRED, role);
    }

    public static AuthorizationException accessDenied(String resource, String action) {
        return new AuthorizationException(null, resource, action);
    }

    public static AuthorizationException ownerOnly(String resource) {
        return new AuthorizationException("OWNER", resource, "ACCESS");
    }

    public static AuthorizationException adminOnly() {
        return new AuthorizationException("ADMIN", null, null);
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }

    public boolean shouldLogToAudit() {
        return true;
    }

    public String describeViolation() {
        return "Authorization violation: role=%s, resource=%s, action=%s, code=%s"
                .formatted(requiredRole, resource, action, getErrorCodeString());
    }
}
