package com.gymflow.security;

import com.gymflow.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final JsonSecurityErrorWriter errorWriter;

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
            response.setHeader("Retry-After", String.valueOf(rule.getWindowSeconds()));
            errorWriter.write(response, HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded. Please try again later.", "RATE_LIMIT_EXCEEDED");
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
        if (path == null || !path.startsWith("/api/")) {
            return null;
        }
        if ("POST".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/auth/")) {
            return rateLimitProperties.getAuth();
        }
        if (path.startsWith("/api/notifications")) {
            return rateLimitProperties.getNotifications();
        }
        return rateLimitProperties.getApi();
    }

    private String buildKey(HttpServletRequest request) {
        return request.getMethod() + ":" + request.getRequestURI() + ":" + resolveClientKey(request);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String currentUser = SecurityUtils.getCurrentUsername();
        if (!"SYSTEM".equalsIgnoreCase(currentUser) && currentUser != null && !currentUser.isBlank()) {
            return "user:" + currentUser.trim().toLowerCase();
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
