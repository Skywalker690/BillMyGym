package com.gymflow.service;

import com.gymflow.api.dto.OrganizationDtos;
import com.gymflow.api.dto.OrganizationDtos.OrganizationResponse;
import com.gymflow.api.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.Role;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public List<OrganizationResponse> listOrganizations() {
        AuthenticatedUser user = SecurityUtils.currentUser();

        if (user.role() == Role.SUPER_ADMIN) {
            return organizationRepository.findAllByDeletedFalse()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        if (user.organizationId() == null) {
            return List.of();
        }

        Organization organization = organizationRepository
                .findByIdAndDeletedFalse(user.organizationId())
                .orElseThrow(() ->
                        new NotFoundException("Organization not found"));

        return List.of(toResponse(organization));
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, UpdateOrganizationRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN && !id.equals(user.organizationId())) {
            throw new ForbiddenException("Forbidden organization access");
        }

        Organization org = organizationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        org.setName(request.name());
        toOrgDto(org, request.district(), request.logoUrl(), request.mapLocationLink(), request.address(), request.adminFullName(), request.adminPhoneNumber(), request.adminEmail(), request);
        org.setDescription(request.description());
        if (request.status() != null) {
            org.setStatus(request.status());
        }

        return toResponse(org);
    }

    static void toOrgDto(Organization org, String district, String string, String string2, String address, String string3, String string4, String string5, UpdateOrganizationRequest request) {
        org.setDistrict(district);
        org.setLogoUrl(string);
        org.setMapLocationLink(string2);
        org.setAddress(address);
        org.setAdminFullName(string3);
        org.setAdminPhoneNumber(string4);
        org.setAdminEmail(string5);
    }

    @Transactional
    public void softDeleteOrganization(Long id) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only SUPER_ADMIN can delete organizations");
        }

        Organization org = organizationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
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
