package com.gymflow.api.mapper;

import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
import com.gymflow.api.dto.OrganizationDtos.OrganizationResponse;
import com.gymflow.api.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.gymflow.domain.Organization;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
            organization.getId(),
            organization.getName(),
            organization.getDistrict(),
            organization.getLogoUrl(),
            organization.getMapLocationLink(),
            organization.getAddress(),
            organization.getAdminFullName(),
            organization.getAdminPhoneNumber(),
            organization.getAdminEmail(),
            organization.getDescription(),
            organization.getStatus()
        );
    }

    public void applyUpdate(Organization organization, UpdateOrganizationRequest request) {
        organization.setName(request.name());
        organization.setDistrict(request.district());
        organization.setLogoUrl(request.logoUrl());
        organization.setMapLocationLink(request.mapLocationLink());
        organization.setAddress(request.address());
        organization.setAdminFullName(request.adminFullName());
        organization.setAdminPhoneNumber(request.adminPhoneNumber());
        organization.setAdminEmail(request.adminEmail());
        organization.setDescription(request.description());
        if (request.status() != null) {
            organization.setStatus(request.status());
        }
    }

    public Organization toNewOrganization(RegisterOrganizationAdminRequest request) {
        Organization organization = new Organization();
        organization.setName(request.organizationName());
        organization.setDistrict(request.district());
        organization.setLogoUrl(request.logoUrl());
        organization.setMapLocationLink(request.mapLocationLink());
        organization.setAddress(request.address());
        organization.setAdminFullName(request.adminFullName());
        organization.setAdminPhoneNumber(request.adminPhoneNumber());
        organization.setAdminEmail(request.adminEmail());
        organization.setDescription(request.organizationDescription());
        return organization;
    }
}
