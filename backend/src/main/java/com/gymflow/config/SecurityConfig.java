package com.gymflow.config;

import com.gymflow.security.ApiFailureLoggingFilter;
import com.gymflow.security.JwtAuthenticationFilter;
import com.gymflow.security.LegalConsentGuardFilter;
import com.gymflow.security.RateLimitingFilter;
import com.gymflow.security.RestAccessDeniedHandler;
import com.gymflow.security.RestAuthenticationEntryPoint;
import com.gymflow.security.SubscriptionGuardFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ORG_ADMIN = "ORG_ADMIN";
    private static final List<String> ALLOWED_ORIGINS = List.of(
        "http://localhost:5173",
        "https://www.billmygym.tech",
            "https://billmygym.tech",
            "https://api.billmygym.tech"
    );

    private final ApiFailureLoggingFilter apiFailureLoggingFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LegalConsentGuardFilter legalConsentGuardFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final SubscriptionGuardFilter subscriptionGuardFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/**", "/actuator/**")
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/google-login",
                    "/api/auth/signup",
                    "/api/auth/legal-policy",
                    "/api/auth/forgot-password",
                    "/api/auth/verify-otp",
                    "/api/auth/change-password",
                    "/api/subscription/webhook",
                    "/api/whatsapp/webhook"
                ).permitAll()
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole(SUPER_ADMIN)
                .requestMatchers(
                    "/api/auth/logout",
                    "/api/auth/me",
                    "/api/auth/consent",
                    "/api/auth/password/request-otp",
                    "/api/auth/password/confirm"
                ).hasAnyRole(SUPER_ADMIN, ORG_ADMIN)
                .requestMatchers(
                    "/api/audit-logs/**",
                    "/api/analytics/platform",
                    "/api/analytics/platform/**",
                    "/api/referrals/analytics",
                    "/api/organizations/directory",
                    "/api/subscription/organizations/**"
                ).hasRole(SUPER_ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/organizations/*/admin").hasRole(SUPER_ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/organizations/*").hasRole(SUPER_ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/organizations/*/members/bulk-upload").hasRole(SUPER_ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/referrals/settings/toggle").hasRole(SUPER_ADMIN)
                .requestMatchers("/api/organizations/*/audit-logs/**").hasRole(SUPER_ADMIN)
                .requestMatchers(
                    "/api/members/**",
                    "/api/members/*/memberships",
                    "/api/payments/**",
                    "/api/reports/**",
                    "/api/reminders/**",
                    "/api/settings/**",
                    "/api/notifications/**",
                    "/api/tracking/**",
                    "/api/analytics/organization",
                    "/api/analytics/revenue",
                    "/api/analytics/revenue/**",
                    "/api/subscription/create",
                    "/api/subscription/verify",
                    "/api/subscription/status",
                    "/api/subscription/payments",
                    "/api/referrals/dashboard"
                ).hasRole(ORG_ADMIN)
                .requestMatchers(
                    "/api/organizations",
                    "/api/organizations/*",
                    "/api/referrals/settings"
                ).hasAnyRole(SUPER_ADMIN, ORG_ADMIN)
                .requestMatchers("/api/**").denyAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(apiFailureLoggingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(legalConsentGuardFilter, JwtAuthenticationFilter.class)
            .addFilterAfter(subscriptionGuardFilter, LegalConsentGuardFilter.class)
            .addFilterAfter(rateLimitingFilter, SubscriptionGuardFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(Arrays.asList("GET", "PATCH", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Rate-Limit-Remaining", "X-Rate-Limit-Retry-After", "Retry-After"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
