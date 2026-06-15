package com.gymflow.api;

import com.gymflow.api.dto.MembershipPlanDtos.CreateMembershipPlanRequest;
import com.gymflow.api.dto.MembershipPlanDtos.UpdateMembershipPlanRequest;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.domain.enums.Role;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.MembershipPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MembershipPlanController {

    private final MembershipPlanService membershipPlanService;

    private void requireOrganizationAccess(Long organizationId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN && !organizationId.equals(user.organizationId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    @GetMapping("/api/membership-plans")
    public ResponseEntity<?> listCurrentOrganizationPlans() {
        return ResponseEntity.ok(ApiResponse.success(
            "Membership plans fetched successfully",
            membershipPlanService.listActivePlansForCurrentOrganization()
        ));
    }

    @GetMapping("/api/organizations/{organizationId}/membership-plans")
    public ResponseEntity<?> listOrganizationPlans(@PathVariable Long organizationId) {
        requireOrganizationAccess(organizationId);
        return ResponseEntity.ok(ApiResponse.success(
            "Membership plans fetched successfully",
            membershipPlanService.listPlansForOrganization(organizationId)
        ));
    }

    @PostMapping("/api/organizations/{organizationId}/membership-plans")
    public ResponseEntity<?> createOrganizationPlan(
        @PathVariable Long organizationId,
        @Valid @RequestBody CreateMembershipPlanRequest request
    ) {
        requireOrganizationAccess(organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            "Membership plan created successfully",
            membershipPlanService.createPlan(organizationId, request)
        ));
    }

    @PatchMapping("/api/organizations/{organizationId}/membership-plans/{planId}")
    public ResponseEntity<?> updateOrganizationPlan(
        @PathVariable Long organizationId,
        @PathVariable Long planId,
        @Valid @RequestBody UpdateMembershipPlanRequest request
    ) {
        requireOrganizationAccess(organizationId);
        return ResponseEntity.ok(ApiResponse.success(
            "Membership plan updated successfully",
            membershipPlanService.updatePlan(organizationId, planId, request)
        ));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/api/organizations/{organizationId}/membership-plans/{planId}")
    public ResponseEntity<?> deleteOrganizationPlan(
        @PathVariable Long organizationId,
        @PathVariable Long planId
    ) {
        requireOrganizationAccess(organizationId);
        membershipPlanService.deletePlan(organizationId, planId);
        return ResponseEntity.ok(ApiResponse.success("Membership plan deleted successfully"));
    }
}
