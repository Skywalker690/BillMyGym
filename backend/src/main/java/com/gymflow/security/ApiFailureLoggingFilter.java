package com.gymflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class ApiFailureLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAtNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            logIfFailure(request, response, startedAtNanos);
        }
    }

    private void logIfFailure(HttpServletRequest request, HttpServletResponse response, long startedAtNanos) {
        int status = response.getStatus();
        if (status < 400) {
            return;
        }

        long durationMillis = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String requestTarget = query == null || query.isBlank() ? path : path + "?" + query;
        String actor = resolveActor();
        String organizationId = resolveOrganizationId();
        String ip = extractClientIp(request);

        if (status >= 500) {
            log.error(
                "API failure status={} method={} path={} actor={} organizationId={} ip={} durationMs={}",
                status,
                request.getMethod(),
                requestTarget,
                actor,
                organizationId,
                ip,
                durationMillis
            );
            return;
        }

        log.warn(
            "API failure status={} method={} path={} actor={} organizationId={} ip={} durationMs={}",
            status,
            request.getMethod(),
            requestTarget,
            actor,
            organizationId,
            ip,
            durationMillis
        );
    }

    private String resolveActor() {
        String username = SecurityUtils.getCurrentUsername();
        return username == null || username.isBlank() ? "ANONYMOUS" : username;
    }

    private String resolveOrganizationId() {
        AuthenticatedUser user = SecurityUtils.currentUserOrNull();
        if (user == null || user.organizationId() == null) {
            return "-";
        }
        return String.valueOf(user.organizationId());
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
