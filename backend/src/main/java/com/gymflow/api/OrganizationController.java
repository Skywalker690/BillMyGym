package com.gymflow.api;

import com.gymflow.api.dto.OrganizationDtos.OrganizationResponse;
import com.gymflow.api.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.gymflow.service.OrganizationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public List<OrganizationResponse> listOrganizations() {
        return organizationService.listOrganizations();
    }

    @PatchMapping("/{id}")
    public OrganizationResponse updateOrganization(
        @PathVariable Long id,
        @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return organizationService.updateOrganization(id, request);
    }

    @DeleteMapping("/{id}")
    public void softDeleteOrganization(@PathVariable Long id) {
        organizationService.softDeleteOrganization(id);
    }
}
