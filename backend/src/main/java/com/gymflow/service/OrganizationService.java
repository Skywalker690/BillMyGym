package com.gymflow.service;

import com.gymflow.api.dto.OrganizationDtos.OrganizationResponse;
import com.gymflow.api.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.Role;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public List<OrganizationResponse> listOrganizations() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() == Role.SUPER_ADMIN) {
            return organizationRepository.findAllByDeletedFalse().stream().map(this::toResponse).toList();
        }
        if (user.organizationId() == null) {
            return List.of();
        }
        return organizationRepository.findByIdAndDeletedFalse(user.organizationId()).map(this::toResponse).stream().toList();
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, UpdateOrganizationRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN && !id.equals(user.organizationId())) {
            throw new IllegalArgumentException("Forbidden organization access");
        }

        Organization org = organizationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        org.setName(request.name());
        org.setDistrict(request.district());
        org.setLogoUrl(request.logoUrl());
        org.setMapLocationLink(request.mapLocationLink());
        org.setAddress(request.address());
        org.setAdminFullName(request.adminFullName());
        org.setAdminPhoneNumber(request.adminPhoneNumber());
        org.setAdminEmail(request.adminEmail());
        org.setDescription(request.description());
        if (request.status() != null) {
            org.setStatus(request.status());
        }

        return toResponse(org);
    }

    @Transactional
    public void softDeleteOrganization(Long id) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Only SUPER_ADMIN can delete organizations");
        }

        Organization org = organizationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        org.setDeleted(true);
        org.setStatus(com.gymflow.domain.enums.OrganizationStatus.INACTIVE);
    }

    private OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
            org.getId(),
            org.getName(),
            org.getDistrict(),
            org.getLogoUrl(),
            org.getMapLocationLink(),
            org.getAddress(),
            org.getAdminFullName(),
            org.getAdminPhoneNumber(),
            org.getAdminEmail(),
            org.getDescription(),
            org.getStatus()
        );
    }
}
