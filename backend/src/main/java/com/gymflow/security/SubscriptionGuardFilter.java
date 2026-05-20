package com.gymflow.security;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.repository.OrganizationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class SubscriptionGuardFilter extends OncePerRequestFilter {

    private final OrganizationRepository organizationRepository;
    private final JsonSecurityErrorWriter errorWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
            || path.startsWith("/actuator/")
            || "/api/subscription/webhook".equals(path)
            || isSubscriptionAllowedPath(path)
            || isPublicAuthEndpoint(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
            ? null
            : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser user) || user.organizationId() == null || user.role() == Role.SUPER_ADMIN) {
            filterChain.doFilter(request, response);
            return;
        }

        Organization organization = organizationRepository.findByIdAndDeletedFalse(user.organizationId()).orElse(null);
        if (organization == null) {
            errorWriter.write(response, HttpStatus.FORBIDDEN.value(), "Organization context is invalid", "ORGANIZATION_CONTEXT_INVALID");
            return;
        }
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            filterChain.doFilter(request, response);
            return;
        }
        if (organization.getStatus() == OrganizationStatus.ACTIVE || organization.getStatus() == OrganizationStatus.TRIAL) {
            filterChain.doFilter(request, response);
            return;
        }
        errorWriter.write(
            response,
            HttpStatus.PAYMENT_REQUIRED.value(),
            "Access denied because the organization subscription is not active.",
            "SUBSCRIPTION_INACTIVE"
        );
    }

    private boolean isSubscriptionAllowedPath(String path) {
        return path.startsWith("/api/subscription/")
            || path.startsWith("/api/payments/")
            || "/api/auth/logout".equals(path);
    }

    private boolean isPublicAuthEndpoint(String path) {
        return "/api/auth/login".equals(path)
            || "/api/auth/google-login".equals(path)
            || "/api/auth/forgot-password".equals(path)
            || "/api/auth/verify-otp".equals(path)
            || "/api/auth/change-password".equals(path);
    }
}
