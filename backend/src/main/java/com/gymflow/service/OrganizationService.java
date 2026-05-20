package com.gymflow.service;

import com.gymflow.api.dto.OrganizationDtos.ReplaceOrganizationAdminRequest;
import com.gymflow.api.dto.OrganizationDtos.OrganizationDirectoryResponse;
import com.gymflow.api.dto.OrganizationDtos.OrganizationDirectorySummary;
import com.gymflow.api.dto.OrganizationDtos.OrganizationPlanSummary;
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
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    public OrganizationDirectoryResponse directory(
        int page,
        int size,
        String search,
        boolean pilotOnly,
        OrganizationStatus status,
        com.gymflow.domain.enums.SubscriptionPlan plan
    ) {
        SecurityUtils.requireRole(Role.SUPER_ADMIN);
        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        var result = organizationRepository.findDirectoryPage(
            normalizedSearch,
            pilotOnly,
            status,
            plan,
            PageRequest.of(safePage, safeSize)
        );
        return new OrganizationDirectoryResponse(
            result.getContent().stream().map(organizationMapper::toResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext(),
            buildDirectorySummary()
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

    private OrganizationDirectorySummary buildDirectorySummary() {
        long totalOrganizations = organizationRepository.findAllByDeletedFalse().size();
        long totalActiveOrganizations = organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.ACTIVE);
        long totalRenewalPendingOrganizations = organizationRepository.countByDeletedFalseAndStatusIn(
            List.of(OrganizationStatus.PAUSED, OrganizationStatus.DEACTIVATED)
        );
        long totalPilotOrganizations = organizationRepository.countByDeletedFalseAndOrganizationType(OrganizationType.PILOT);
        List<OrganizationPlanSummary> subscriptionDistribution = Arrays.stream(com.gymflow.domain.enums.SubscriptionPlan.values())
            .map(plan -> {
                long count = organizationRepository.countByDeletedFalseAndSubscriptionPlan(plan);
                int percentage = totalOrganizations <= 0 ? 0 : (int) Math.round((count * 100.0) / totalOrganizations);
                String label = switch (plan) {
                    case STARTER -> "Startup";
                    case GROWTH -> "Growth";
                    case PRO -> "Pro";
                    case PILOT -> "Founding Partner Access";
                };
                return new OrganizationPlanSummary(plan.name(), label, count, percentage);
            })
            .toList();
        return new OrganizationDirectorySummary(
            totalOrganizations,
            totalActiveOrganizations,
            totalRenewalPendingOrganizations,
            totalPilotOrganizations,
            subscriptionDistribution
        );
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
