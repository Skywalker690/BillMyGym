package com.gymflow.api;

import com.gymflow.api.dto.ReferralDtos.ReferralDashboardResponse;
import com.gymflow.api.dto.ReferralDtos.AdminReferralAnalyticsResponse;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.PlatformSettingService;
import com.gymflow.service.ReferralService;
import com.gymflow.api.error.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;
    private final PlatformSettingService platformSettingService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        if (!platformSettingService.getSettings().isReferralsEnabled()) {
            throw new ForbiddenException("Referral program is currently disabled");
        }
        Long organizationId = SecurityUtils.currentUser().organizationId();
        ReferralDashboardResponse response = referralService.getDashboard(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Referral dashboard fetched", response));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAnalytics() {
        AdminReferralAnalyticsResponse response = referralService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Referral analytics fetched", response));
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        boolean enabled = platformSettingService.getSettings().isReferralsEnabled();
        return ResponseEntity.ok(ApiResponse.success("Referral settings fetched", enabled));
    }

    @PostMapping("/settings/toggle")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggleReferrals(@RequestParam boolean enabled) {
        platformSettingService.updateReferralsEnabled(enabled);
        return ResponseEntity.ok(ApiResponse.success("Referral program status updated", enabled));
    }
}
