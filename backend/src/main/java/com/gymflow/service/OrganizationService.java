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
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
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
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationMapper organizationMapper;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final BillingLifecycleService billingLifecycleService;
    private final SubscriptionPricingService subscriptionPricingService;
    private final JdbcTemplate jdbcTemplate;

    public List<OrganizationResponse> listOrganizations() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() == Role.SUPER_ADMIN) {
            return toResponses(organizationRepository.findAllByDeletedFalse());
        }
        if (user.organizationId() == null) {
            return List.of();
        }
        return organizationRepository.findByIdAndDeletedFalse(user.organizationId())
            .map(this::toResponse)
            .stream()
            .toList();
    }

    public OrganizationResponse getOrganization(Long id) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN && !id.equals(user.organizationId())) {
            throw new ForbiddenException("Forbidden organization access");
        }
        return organizationRepository.findByIdAndDeletedFalse(id)
            .map(this::toResponse)
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
        User primaryAdmin = resolvePrimaryOrganizationAdmin(organization);
        if (user.role() != Role.SUPER_ADMIN) {
            validateDemoSelfServiceUpdate(organization, request);
        }
        validateAdminUniqueness(request.adminEmail(), request.adminPhoneNumber(), organization.getId(), primaryAdmin);
        OrganizationType previousType = organization.getOrganizationType();
        OrganizationStatus previousStatus = organization.getStatus();
        String previousAdminEmail = organization.getAdminEmail();
        organizationMapper.applyUpdate(organization, request);
        syncPrimaryAdminCredentials(primaryAdmin, previousAdminEmail, request.adminEmail());
        normalizeLifecycleAfterUpdate(organization, previousType, previousStatus);

        auditLogService.log(
            "ORGANIZATION_UPDATED",
            "Updated organization " + organization.getName() + " (id=" + organization.getId() + ")",
            AuditCategory.ORGANIZATION,
            "ORGANIZATION"
        );
        auditLogService.logOrganizationActivity(
            "ORGANIZATION_UPDATED",
            "Updated organization " + organization.getName() + " (id=" + organization.getId() + ")",
            AuditActivityScope.ADMIN_ACTIVITY,
            AuditEventStatus.INFO,
            "ORGANIZATION",
            organization.getId(),
            organization.getName()
        );
        return toResponse(organization);
    }

    @Transactional
    public void hardDeleteOrganization(Long id) {
        SecurityUtils.requireRole(Role.SUPER_ADMIN);
        Organization organization = organizationRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        auditLogService.log(
            "ORGANIZATION_HARD_DELETED",
            "Hard deleted organization " + organization.getName() + " (id=" + organization.getId() + ")",
            AuditCategory.ORGANIZATION,
            "ORGANIZATION"
        );
        auditLogService.logOrganizationActivity(
            "ORGANIZATION_HARD_DELETED",
            "Hard deleted organization " + organization.getName() + " (id=" + organization.getId() + ")",
            AuditActivityScope.ADMIN_ACTIVITY,
            AuditEventStatus.INFO,
            "ORGANIZATION",
            organization.getId(),
            organization.getName()
        );

        List<String> tables = List.of(
            "member_payments",
            "member_memberships",
            "member_credit_ledger",
            "whatsapp_reminder_logs",
            "members",
            "membership_plans",
            "notification_delivery_logs",
            "notifications",
            "organization_settings",
            "organization_subscription_payments",
            "referral_rewards",
            "usage_metrics",
            "audit_logs",
            "users"
        );
        for (String table : tables) {
            jdbcTemplate.update("DELETE FROM " + table + " WHERE organization_id = ?", id);
        }
        jdbcTemplate.update("DELETE FROM referrals WHERE referrer_org_id = ? OR referred_org_id = ?", id, id);
        jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", id);
    }

    public OrganizationDirectoryResponse directory(
        int page,
        int size,
        String search,
        boolean includeAllTypes,
        boolean pilotOnly,
        OrganizationType organizationType,
        OrganizationStatus status,
        com.gymflow.domain.enums.SubscriptionPlan plan
    ) {
        SecurityUtils.requireRole(Role.SUPER_ADMIN);
        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String pattern = normalizedSearch == null ? null : "%" + normalizedSearch.toLowerCase() + "%";
        var result = organizationRepository.findDirectoryPage(
            normalizedSearch,
            pattern,
            includeAllTypes,
            pilotOnly,
            organizationType,
            status,
            plan,
            PageRequest.of(safePage, safeSize)
        );
        return new OrganizationDirectoryResponse(
            toResponses(result.getContent()),
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
        validateAdminUniqueness(request.adminEmail(), request.adminPhoneNumber(), organization.getId(), null);

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
        auditLogService.logOrganizationActivity(
            "ORGANIZATION_ADMIN_REPLACED",
            "Replaced organization admin for " + organization.getName() + " (id=" + organization.getId() + ")",
            AuditActivityScope.ADMIN_ACTIVITY,
            AuditEventStatus.INFO,
            "ORGANIZATION",
            organization.getId(),
            organization.getName()
        );
        return toResponse(organization);
    }

    private void validateAdminUniqueness(String adminEmail, String adminPhoneNumber, Long organizationId, User primaryAdmin) {
        if (organizationRepository.existsByAdminEmailAndDeletedFalseAndIdNot(adminEmail, organizationId)) {
            throw new BadRequestException("Admin email already exists");
        }
        if (organizationRepository.existsByAdminPhoneNumberAndDeletedFalseAndIdNot(adminPhoneNumber, organizationId)) {
            throw new BadRequestException("Admin phone number already exists");
        }
        boolean emailExistsOnAnotherUser = primaryAdmin == null
            ? appUserRepository.existsByEmailAndDeletedFalse(adminEmail)
            : appUserRepository.existsByEmailAndDeletedFalseAndIdNot(adminEmail, primaryAdmin.getId());
        if (emailExistsOnAnotherUser) {
            throw new BadRequestException("Admin email already exists");
        }
    }

    private User resolvePrimaryOrganizationAdmin(Organization organization) {
        List<User> organizationAdmins = appUserRepository.findByOrganizationIdAndRoleAndDeletedFalse(
            organization.getId(),
            Role.ORG_ADMIN
        );
        User matchingEmailAdmin = organizationAdmins.stream()
            .filter(admin -> Objects.equals(admin.getEmail(), organization.getAdminEmail()))
            .findFirst()
            .orElse(null);
        if (matchingEmailAdmin != null) {
            return matchingEmailAdmin;
        }
        List<User> activeAdmins = organizationAdmins.stream()
            .filter(admin -> admin.getStatus() == UserStatus.ACTIVE)
            .toList();
        if (activeAdmins.size() == 1) {
            return activeAdmins.getFirst();
        }
        if (organizationAdmins.size() == 1) {
            return organizationAdmins.getFirst();
        }
        return null;
    }

    private void syncPrimaryAdminCredentials(User primaryAdmin, String previousAdminEmail, String requestedAdminEmail) {
        if (primaryAdmin == null || Objects.equals(previousAdminEmail, requestedAdminEmail)) {
            return;
        }
        primaryAdmin.setEmail(requestedAdminEmail);
    }

    private OrganizationDirectorySummary buildDirectorySummary() {
        long totalOrganizations = organizationRepository.countByDeletedFalseAndOrganizationType(OrganizationType.REGULAR);
        long totalActiveOrganizations = organizationRepository.countByDeletedFalseAndOrganizationTypeAndStatus(OrganizationType.REGULAR, OrganizationStatus.ACTIVE);
        long totalRenewalPendingOrganizations = organizationRepository.countByDeletedFalseAndOrganizationTypeAndStatusIn(
            OrganizationType.REGULAR,
            List.of(OrganizationStatus.PAUSED, OrganizationStatus.DEACTIVATED)
        );
        long totalTrialOrganizations = organizationRepository.countByDeletedFalseAndOrganizationTypeAndStatus(OrganizationType.REGULAR, OrganizationStatus.TRIAL);
        Map<com.gymflow.domain.enums.SubscriptionPlan, Long> regularPlanCounts = organizationRepository
            .countBySubscriptionPlanForOrganizationTypeExcludingStatus(OrganizationType.REGULAR, OrganizationStatus.TRIAL)
            .stream()
            .collect(java.util.stream.Collectors.toMap(
                OrganizationRepository.OrganizationPlanCountProjection::getSubscriptionPlan,
                projection -> projection.getCount() == null ? 0L : projection.getCount()
            ));
        List<OrganizationPlanSummary> subscriptionDistribution = new ArrayList<>(Arrays.stream(com.gymflow.domain.enums.SubscriptionPlan.values())
            .map(plan -> {
                long count = regularPlanCounts.getOrDefault(plan, 0L);
                int percentage = totalOrganizations <= 0 ? 0 : (int) Math.round((count * 100.0) / totalOrganizations);
                String label = plan == com.gymflow.domain.enums.SubscriptionPlan.BASE ? "Blaze Plan" : "Automation Plan";
                return new OrganizationPlanSummary(plan.name(), label, count, percentage);
            })
            .toList());
        subscriptionDistribution.add(new OrganizationPlanSummary(
            "TRIAL",
            "Free Trial Gyms",
            totalTrialOrganizations,
            totalOrganizations <= 0 ? 0 : (int) Math.round((totalTrialOrganizations * 100.0) / totalOrganizations)
        ));
        return new OrganizationDirectorySummary(
            totalOrganizations,
            totalActiveOrganizations,
            totalRenewalPendingOrganizations,
            0,
            totalTrialOrganizations,
            subscriptionDistribution
        );
    }

    private void normalizeLifecycleAfterUpdate(
        Organization organization,
        OrganizationType previousType,
        OrganizationStatus previousStatus
    ) {
        LocalDate today = LocalDate.now(APP_ZONE);
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            if (previousType != OrganizationType.DEMO) {
                billingLifecycleService.startDemoAccess(organization, today);
            }
            return;
        }

        if (previousType == OrganizationType.DEMO) {
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
            organization.setDeactivatedAt(OffsetDateTime.now(APP_ZONE));
        }
    }

    private void validateDemoSelfServiceUpdate(Organization organization, UpdateOrganizationRequest request) {
        if (organization.getOrganizationType() != OrganizationType.DEMO) {
            throw new ForbiddenException("Only demo workspaces can self-manage primary contact details.");
        }
        boolean changingRestrictedField =
            !Objects.equals(request.name(), organization.getName())
                || !Objects.equals(request.district(), organization.getDistrict())
                || !Objects.equals(request.place(), organization.getPlace())
                || !Objects.equals(request.logoUrl(), organization.getLogoUrl())
                || !Objects.equals(request.mapLocationLink(), organization.getMapLocationLink())
                || !Objects.equals(request.adminEmail(), organization.getAdminEmail())
                || compareMoney(request.monthlyMembershipFee(), organization.getMonthlyMembershipFee()) != 0
                || compareMoney(request.admissionFee(), organization.getAdmissionFee()) != 0
                || request.organizationType() != null && request.organizationType() != organization.getOrganizationType()
                || request.subscriptionPlan() != null && request.subscriptionPlan() != organization.getSubscriptionPlan()
                || request.status() != null && request.status() != organization.getStatus();
        if (changingRestrictedField) {
            throw new ForbiddenException("Demo org admins can only update the primary contact name and phone number.");
        }
    }

    private int compareMoney(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private OrganizationResponse toResponse(Organization organization) {
        OrganizationResponse base = organizationMapper.toResponse(organization);
        SubscriptionPricingService.SubscriptionPriceBreakdown pricing = subscriptionPricingService.currentPrice(organization);
        return toResponse(base, pricing);
    }

    private List<OrganizationResponse> toResponses(List<Organization> organizations) {
        if (organizations == null || organizations.isEmpty()) {
            return List.of();
        }
        Map<Long, SubscriptionPricingService.SubscriptionPriceBreakdown> pricingByOrganizationId =
            subscriptionPricingService.currentPrices(organizations);
        return organizations.stream()
            .map(organization -> toResponse(
                organizationMapper.toResponse(organization),
                pricingByOrganizationId.get(organization.getId())
            ))
            .toList();
    }

    private OrganizationResponse toResponse(
        OrganizationResponse base,
        SubscriptionPricingService.SubscriptionPriceBreakdown pricing
    ) {
        return new OrganizationResponse(
            base.id(),
            base.name(),
            base.district(),
            base.place(),
            base.logoUrl(),
            base.mapLocationLink(),
            base.adminFullName(),
            base.adminPhoneNumber(),
            base.adminEmail(),
            base.monthlyMembershipFee(),
            base.admissionFee(),
            base.organizationType(),
            base.subscriptionPlan(),
            base.status(),
            base.trialStartDate(),
            base.trialEndDate(),
            base.subscriptionDueDate(),
            base.pausedAt(),
            base.deactivatedAt(),
            pricing.activeMembers(),
            pricing.includedMembers(),
            pricing.overageMembers(),
            pricing.overageRate(),
            pricing.overageAmount(),
            pricing.totalAmount()
        );
    }
}
