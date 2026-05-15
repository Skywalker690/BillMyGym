package com.gymflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestSequence = new AtomicLong();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitProperties.Rule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildKey(request);
        long windowMillis = rule.getWindowSeconds() * 1000L;
        long now = System.currentTimeMillis();

        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now));
        if (!counter.tryAcquire(now, rule.getMaxRequests(), windowMillis)) {
            writeTooManyRequests(response, rule);
            log.warn("Rate limit exceeded for key={} path={}", key, request.getRequestURI());
            return;
        }

        maybeCleanup(now, windowMillis);
        filterChain.doFilter(request, response);
    }

    private RateLimitProperties.Rule resolveRule(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/v1/")) {
            return null;
        }
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            if ("/api/v1/auth/login".equals(path) || "/api/v1/auth/google-login".equals(path)) {
                return rateLimitProperties.getAuth();
            }
            if ("/api/v1/auth/forgot-password".equals(path)
                    || "/api/v1/auth/verify-otp".equals(path)
                    || "/api/v1/auth/change-password".equals(path)) {
                return rateLimitProperties.getPasswordReset();
            }
        }
        if (path != null && path.startsWith("/api/v1/notifications")) {
            return rateLimitProperties.getNotifications();
        }
        return rateLimitProperties.getApi();
    }

    private String buildKey(HttpServletRequest request) {
        return request.getMethod() + ":" + request.getRequestURI() + ":" + resolveClientKey(request);
    }

    private String resolveClientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null
                && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return "user:" + authentication.getName().trim().toLowerCase();
        }
        return "ip:" + extractClientIp(request);
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

    private void maybeCleanup(long now, long currentWindowMillis) {
        if (requestSequence.incrementAndGet() % 500 != 0) {
            return;
        }
        long cutoff = now - Math.max(currentWindowMillis * 2, 600_000L);
        counters.entrySet().removeIf(entry -> entry.getValue().getLastSeenAt() < cutoff);
    }

    private void writeTooManyRequests(HttpServletResponse response, RateLimitProperties.Rule rule) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(rule.getWindowSeconds()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Too many requests");
        body.put("message", "Rate limit exceeded. Please try again later.");
        body.put("retryAfterSeconds", rule.getWindowSeconds());
        body.put("timestamp", Instant.now().toString());
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static final class WindowCounter {
        private long windowStartedAt;
        private long lastSeenAt;
        private int count;

        private WindowCounter(long now) {
            this.windowStartedAt = now;
            this.lastSeenAt = now;
        }

        private synchronized boolean tryAcquire(long now, int maxRequests, long windowMillis) {
            if (now - windowStartedAt >= windowMillis) {
                windowStartedAt = now;
                count = 0;
            }
            lastSeenAt = now;
            if (count >= maxRequests) {
                return false;
            }
            count++;
            return true;
        }

        private synchronized long getLastSeenAt() {
            return lastSeenAt;
        }
    }
}
