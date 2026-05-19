package com.gymflow.api;

import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.AnalyticsService;
import com.gymflow.service.PlanAccessService;
import com.gymflow.service.RevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final RevenueService revenueService;
    private final PlanAccessService planAccessService;

    @GetMapping("/platform")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> platformAnalytics() {
        return ResponseEntity.ok(ApiResponse.success("Platform analytics fetched successfully", analyticsService.platformAnalytics()));
    }

    @GetMapping("/organization")
    public ResponseEntity<?> organizationAnalytics() {
        planAccessService.requireAdvancedAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Organization analytics fetched successfully", analyticsService.organizationAnalytics()));
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> revenueAnalytics() {
        planAccessService.requireAdvancedAnalytics();
        return ResponseEntity.ok(ApiResponse.success(
            "Revenue analytics fetched successfully",
            revenueService.revenueSummary()
        ));
    }

    @GetMapping("/revenue/monthly")
    public ResponseEntity<?> monthlyRevenueAnalytics() {
        planAccessService.requireAdvancedAnalytics();
        return ResponseEntity.ok(ApiResponse.success(
            "Monthly revenue analytics fetched successfully",
            revenueService.monthlyRevenue()
        ));
    }
}
