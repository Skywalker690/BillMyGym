package com.gymflow.api;

import com.gymflow.api.dto.ReferralDtos.ReferralDashboardResponse;
import com.gymflow.api.dto.ReferralDtos.AdminReferralAnalyticsResponse;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.domain.enums.Role;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        ReferralDashboardResponse response = referralService.getDashboard(organizationId);
        return ResponseEntity.ok(ApiResponse.success("Referral dashboard fetched", response));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<?> getAnalytics() {
        AdminReferralAnalyticsResponse response = referralService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Referral analytics fetched", response));
    }
}
