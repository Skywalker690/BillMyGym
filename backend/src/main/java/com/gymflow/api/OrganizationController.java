package com.gymflow.api;

import com.gymflow.api.dto.OrganizationDtos.ReplaceOrganizationAdminRequest;
import com.gymflow.api.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<?> listOrganizations() {
        return ResponseEntity.ok(ApiResponse.success("Organizations fetched successfully", organizationService.listOrganizations()));
    }

    @GetMapping("/directory")
    public ResponseEntity<?> organizationDirectory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "false") boolean pilotOnly,
        @RequestParam(required = false) OrganizationStatus status,
        @RequestParam(required = false) SubscriptionPlan plan
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization directory fetched successfully",
            organizationService.directory(page, size, search, pilotOnly, status, plan)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Organization fetched successfully", organizationService.getOrganization(id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", organizationService.updateOrganization(id, request)));
    }

    @PutMapping("/{id}/admin")
    public ResponseEntity<?> replaceOrganizationAdmin(@PathVariable Long id, @Valid @RequestBody ReplaceOrganizationAdminRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization admin replaced successfully",
            organizationService.replaceOrganizationAdmin(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDeleteOrganization(@PathVariable Long id) {
        organizationService.softDeleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("Organization deleted successfully"));
    }
}
