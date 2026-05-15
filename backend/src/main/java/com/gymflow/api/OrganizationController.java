package com.gymflow.api;

import com.gymflow.api.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable Long id, @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Organization updated successfully", organizationService.updateOrganization(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDeleteOrganization(@PathVariable Long id) {
        organizationService.softDeleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("Organization deleted successfully"));
    }
}
