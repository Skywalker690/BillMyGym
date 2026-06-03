package com.gymflow.security;

import com.gymflow.domain.User;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.service.LegalPolicyService;
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
public class LegalConsentGuardFilter extends OncePerRequestFilter {

    private final AppUserRepository appUserRepository;
    private final LegalPolicyService legalPolicyService;
    private final JsonSecurityErrorWriter errorWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
            || path.startsWith("/actuator/")
            || "/api/subscription/webhook".equals(path)
            || "/api/whatsapp/webhook".equals(path)
            || isPublicAuthEndpoint(path)
            || isConsentAllowedPath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
            ? null
            : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = appUserRepository.findByEmailAndDeletedFalse(authenticatedUser.email()).orElse(null);
        if (user == null) {
            errorWriter.write(response, HttpStatus.UNAUTHORIZED.value(), "User account is no longer available", "USER_NOT_FOUND");
            return;
        }
        if (!legalPolicyService.requiresConsent(user)) {
            filterChain.doFilter(request, response);
            return;
        }
        errorWriter.write(
            response,
            HttpStatus.FORBIDDEN.value(),
            legalPolicyService.consentMessage(user),
            "LEGAL_CONSENT_REQUIRED"
        );
    }

    private boolean isPublicAuthEndpoint(String path) {
        return "/api/auth/login".equals(path)
            || "/api/auth/google-login".equals(path)
            || "/api/auth/forgot-password".equals(path)
            || "/api/auth/verify-otp".equals(path)
            || "/api/auth/change-password".equals(path)
            || "/api/auth/legal-policy".equals(path);
    }

    private boolean isConsentAllowedPath(String path) {
        return "/api/auth/logout".equals(path)
            || "/api/auth/me".equals(path)
            || "/api/auth/consent".equals(path)
            || "/api/auth/password/request-otp".equals(path)
            || "/api/auth/password/confirm".equals(path);
    }
}
