package com.gymflow.api;

import com.gymflow.api.dto.AnalyticsDtos.OrganizationAnalyticsResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformAnalyticsResponse;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.domain.enums.Role;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/platform")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public PlatformAnalyticsResponse platformAnalytics() {
        return analyticsService.platformAnalytics();
    }

    @GetMapping("/organization")
    public OrganizationAnalyticsResponse organizationAnalytics() {
        return analyticsService.organizationAnalytics();
    }
}
