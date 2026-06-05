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
            organization.getPlace(),
            organization.getLogoUrl(),
            organization.getMapLocationLink(),
            organization.getAdminFullName(),
            organization.getAdminPhoneNumber(),
            organization.getAdminEmail(),
            organization.getDescription(),
            organization.getMonthlyMembershipFee(),
            organization.getAdmissionFee(),
            organization.getOrganizationType(),
            organization.getSubscriptionPlan(),
            organization.getStatus(),
            organization.getTrialStartDate(),
            organization.getTrialEndDate(),
            organization.getSubscriptionDueDate(),
            organization.getPausedAt(),
            organization.getDeactivatedAt(),
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public void applyUpdate(Organization organization, UpdateOrganizationRequest request) {
        organization.setName(request.name());
        organization.setDistrict(request.district());
        if (request.place() != null) {
            organization.setPlace(request.place());
        }
        organization.setLogoUrl(request.logoUrl());
        organization.setMapLocationLink(request.mapLocationLink());
        organization.setAdminFullName(request.adminFullName());
        organization.setAdminPhoneNumber(request.adminPhoneNumber());
        organization.setAdminEmail(request.adminEmail());
        organization.setDescription(request.description());
        if (request.monthlyMembershipFee() != null) {
            organization.setMonthlyMembershipFee(request.monthlyMembershipFee());
        }
        if (request.admissionFee() != null) {
            organization.setAdmissionFee(request.admissionFee());
        }
        if (request.organizationType() != null) {
            organization.setOrganizationType(request.organizationType());
        }
        if (request.subscriptionPlan() != null) {
            organization.setSubscriptionPlan(request.subscriptionPlan());
        }
        if (request.status() != null) {
            organization.setStatus(request.status());
        }
    }

    public Organization toNewOrganization(RegisterOrganizationAdminRequest request) {
        Organization organization = new Organization();
        organization.setName(request.organizationName());
        organization.setPlace(request.place());
        organization.setDistrict(request.district());
        organization.setLogoUrl(request.logoUrl());
        organization.setMapLocationLink(request.mapLocationLink());
        organization.setAdminFullName(request.adminFullName());
        organization.setAdminPhoneNumber(request.adminPhoneNumber());
        organization.setAdminEmail(request.adminEmail());
        organization.setDescription(request.organizationDescription());
        if (request.monthlyMembershipFee() != null) {
            organization.setMonthlyMembershipFee(request.monthlyMembershipFee());
        }
        if (request.admissionFee() != null) {
            organization.setAdmissionFee(request.admissionFee());
        }
        if (request.organizationType() != null) {
            organization.setOrganizationType(request.organizationType());
        }
        if (request.subscriptionPlan() != null) {
            organization.setSubscriptionPlan(request.subscriptionPlan());
        }
        return organization;
    }
}
