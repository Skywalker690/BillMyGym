package com.gymflow.security;

import com.gymflow.api.error.ForbiddenException;
import com.gymflow.domain.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private static final AuthenticatedUser SYSTEM_USER = new AuthenticatedUser(null, "SYSTEM", null, null);

    private SecurityUtils() {
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return SYSTEM_USER.email();
        }
        return authentication.getName();
    }

    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }

        throw new ForbiddenException("Invalid authentication principal");
    }

    public static AuthenticatedUser currentUserOrSystem() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return SYSTEM_USER;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }

        return new AuthenticatedUser(null, authentication.getName(), null, null);
    }

    public static AuthenticatedUser requireRole(Role expectedRole) {
        AuthenticatedUser user = currentUser();
        if (user.role() != expectedRole) {
            throw new ForbiddenException("Required role: " + expectedRole.name());
        }
        return user;
    }
}
