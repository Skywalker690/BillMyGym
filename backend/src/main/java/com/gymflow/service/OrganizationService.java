package com.gymflow.service;

import com.gymflow.api.dto.OrganizationDtos.ReplaceOrganizationAdminRequest;
import com.gymflow.api.dto.OrganizationDtos.OrganizationResponse;
import com.gymflow.api.dto.OrganizationDtos.UpdateOrganizationRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.api.mapper.OrganizationMapper;
import com.gymflow.domain.Organization;
import com.gymflow.domain.User;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationMapper organizationMapper;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final BillingLifecycleService billingLifecycleService;

    public List<OrganizationResponse> listOrganizations() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() == Role.SUPER_ADMIN) {
            return organizationRepository.findAllByDeletedFalse().stream().map(organizationMapper::toResponse).toList();
        }
        if (user.organizationId() == null) {
            return List.of();
        }
        return organizationRepository.findByIdAndDeletedFalse(user.organizationId())
            .map(organizationMapper::toResponse)
            .stream()
            .toList();
    }

    public OrganizationResponse getOrganization(Long id) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN && !id.equals(user.organizationId())) {
            throw new ForbiddenException("Forbidden organization access");
        }
        return organizationRepository.findByIdAndDeletedFalse(id)
            .map(organizationMapper::toResponse)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    @Transactional
    public OrganizationResponse updateOrganization(Long id, UpdateOrganizationRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN && !id.equals(user.organizationId())) {
            throw new ForbiddenException("Forbidden organization access");
        }

        Organization organization = organizationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        validateAdminUniqueness(request.adminEmail(), request.adminPhoneNumber(), organization.getId());
        OrganizationType previousType = organization.getOrganizationType();
        OrganizationStatus previousStatus = organization.getStatus();
        organizationMapper.applyUpdate(organization, request);
        normalizeLifecycleAfterUpdate(organization, previousType, previousStatus);

        auditLogService.log(
            "ORGANIZATION_UPDATED",
            "Updated organization " + organization.getName() + " (id=" + organization.getId() + ")",
            AuditCategory.ORGANIZATION,
            "ORGANIZATION"
        );
        return organizationMapper.toResponse(organization);
    }

    @Transactional
    public void softDeleteOrganization(Long id) {
        SecurityUtils.requireRole(Role.SUPER_ADMIN);
        Organization organization = organizationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        organization.setDeleted(true);
        organization.setDeletedAt(OffsetDateTime.now());
        organization.setStatus(OrganizationStatus.DEACTIVATED);

        auditLogService.log(
            "ORGANIZATION_SOFT_DELETED",
            "Soft deleted organization " + organization.getName() + " (id=" + organization.getId() + ")",
            AuditCategory.ORGANIZATION,
            "ORGANIZATION"
        );
    }

    @Transactional
    public OrganizationResponse replaceOrganizationAdmin(Long id, ReplaceOrganizationAdminRequest request) {
        SecurityUtils.requireRole(Role.SUPER_ADMIN);
        Organization organization = organizationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        validateAdminUniqueness(request.adminEmail(), request.adminPhoneNumber(), organization.getId());

        for (User existingAdmin : appUserRepository.findByOrganizationIdAndRoleAndDeletedFalse(id, Role.ORG_ADMIN)) {
            existingAdmin.setStatus(UserStatus.INACTIVE);
        }

        User admin = new User();
        admin.setOrganization(organization);
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ORG_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        appUserRepository.save(admin);

        organization.setAdminFullName(request.adminFullName());
        organization.setAdminPhoneNumber(request.adminPhoneNumber());
        organization.setAdminEmail(request.adminEmail());

        auditLogService.log(
            "ORGANIZATION_ADMIN_REPLACED",
            "Replaced organization admin for " + organization.getName() + " (id=" + organization.getId() + ")",
            AuditCategory.ORGANIZATION,
            "ORGANIZATION"
        );
        return organizationMapper.toResponse(organization);
    }

    private void validateAdminUniqueness(String adminEmail, String adminPhoneNumber, Long organizationId) {
        if (organizationRepository.existsByAdminEmailAndDeletedFalseAndIdNot(adminEmail, organizationId)) {
            throw new BadRequestException("Admin email already exists");
        }
        if (organizationRepository.existsByAdminPhoneNumberAndDeletedFalseAndIdNot(adminPhoneNumber, organizationId)) {
            throw new BadRequestException("Admin phone number already exists");
        }
    }

    private void normalizeLifecycleAfterUpdate(
        Organization organization,
        OrganizationType previousType,
        OrganizationStatus previousStatus
    ) {
        LocalDate today = LocalDate.now();
        if (organization.getOrganizationType() == OrganizationType.PILOT) {
            if (previousType != OrganizationType.PILOT
                || organization.getPilotStartDate() == null
                || organization.getPilotEndDate() == null) {
                billingLifecycleService.startPilotAccess(organization, today);
            }
            return;
        }

        if (previousType == OrganizationType.PILOT) {
            billingLifecycleService.startRegularTrial(organization, today);
            return;
        }

        if (organization.getStatus() == OrganizationStatus.TRIAL) {
            if (previousStatus != OrganizationStatus.TRIAL
                || organization.getTrialStartDate() == null
                || organization.getTrialEndDate() == null) {
                billingLifecycleService.startRegularTrial(organization, today);
            }
            return;
        }

        if (organization.getStatus() == OrganizationStatus.ACTIVE
            && !billingLifecycleService.hasCurrentPaidAccess(organization, today)) {
            throw new BadRequestException("Regular organizations become ACTIVE only after successful subscription payment.");
        }

        if (organization.getStatus() == OrganizationStatus.PAUSED) {
            billingLifecycleService.markPaused(organization);
            return;
        }

        if (organization.getStatus() == OrganizationStatus.DEACTIVATED && organization.getDeactivatedAt() == null) {
            organization.setDeactivatedAt(OffsetDateTime.now());
        }
    }
}
