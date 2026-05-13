package com.gymflow.api;

import com.gymflow.api.dto.AnalyticsDtos.OrganizationAnalyticsResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformAnalyticsResponse;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.domain.enums.Role;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/platform")
    public PlatformAnalyticsResponse platformAnalytics() {
        if (SecurityUtils.currentUser().role() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only SUPER_ADMIN can access platform analytics");
        }
        return analyticsService.platformAnalytics();
    }

    @GetMapping("/organization")
    public OrganizationAnalyticsResponse organizationAnalytics() {
        return analyticsService.organizationAnalytics();
    }
}
