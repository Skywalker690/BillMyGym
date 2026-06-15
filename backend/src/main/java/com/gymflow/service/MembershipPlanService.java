package com.gymflow.service;

import com.gymflow.api.dto.MembershipPlanDtos.CreateMembershipPlanRequest;
import com.gymflow.api.dto.MembershipPlanDtos.MembershipPlanListResponse;
import com.gymflow.api.dto.MembershipPlanDtos.MembershipPlanResponse;
import com.gymflow.api.dto.MembershipPlanDtos.UpdateMembershipPlanRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.MembershipPlan;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MembershipPlanRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.SecurityUtils;
import com.gymflow.shared.money.MoneyUtils;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final String DEFAULT_PLAN_NAME = "Regular";

    private final MembershipPlanRepository membershipPlanRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final OrganizationRepository organizationRepository;
    private final ReportCacheService reportCacheService;

    @Transactional(readOnly = true)
    public MembershipPlanListResponse listActivePlansForCurrentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return new MembershipPlanListResponse(toResponses(
            membershipPlanRepository.findByOrganizationIdAndActiveTrueAndDeletedFalseOrderByDefaultPlanDescNameAsc(organizationId)
        ));
    }

    @Transactional(readOnly = true)
    public MembershipPlanListResponse listPlansForOrganization(Long organizationId) {
        return new MembershipPlanListResponse(toResponses(
            membershipPlanRepository.findByOrganizationIdAndDeletedFalseOrderByDefaultPlanDescActiveDescNameAsc(organizationId)
        ));
    }

    @Transactional
    public MembershipPlanResponse createPlan(Long organizationId, CreateMembershipPlanRequest request) {
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        String name = normalizeName(request.name());
        if (membershipPlanRepository.existsByOrganizationIdAndNameIgnoreCaseAndDeletedFalse(organizationId, name)) {
            throw new BadRequestException("Membership plan name already exists for this organization");
        }

        long activePlanCount = membershipPlanRepository.countByOrganizationIdAndActiveTrueAndDeletedFalse(organizationId);
        boolean defaultPlan = Boolean.TRUE.equals(request.defaultPlan()) || activePlanCount == 0;
        boolean active = defaultPlan || request.active() == null || request.active() || activePlanCount == 0;

        MembershipPlan plan = new MembershipPlan();
        plan.setOrganization(organization);
        plan.setName(name);
        plan.setPrice(MoneyUtils.normalize(request.price()));
        plan.setActive(active);
        plan.setDefaultPlan(defaultPlan);
        plan = membershipPlanRepository.save(plan);
        if (plan.isDefaultPlan()) {
            membershipPlanRepository.clearOtherDefaults(organizationId, plan.getId());
        }
        reportCacheService.evictOrganization(organizationId);
        return toResponse(plan);
    }

    @Transactional
    public MembershipPlanResponse updatePlan(Long organizationId, Long planId, UpdateMembershipPlanRequest request) {
        MembershipPlan plan = membershipPlanRepository.findByIdAndOrganizationIdAndDeletedFalse(planId, organizationId)
            .orElseThrow(() -> new NotFoundException("Membership plan not found"));

        if (request.name() != null) {
            String name = normalizeName(request.name());
            if (membershipPlanRepository.existsByOrganizationIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(organizationId, name, planId)) {
                throw new BadRequestException("Membership plan name already exists for this organization");
            }
            plan.setName(name);
        }
        if (request.price() != null) {
            plan.setPrice(MoneyUtils.normalize(request.price()));
        }
        if (request.active() != null) {
            applyActiveState(plan, request.active());
        }
        if (request.defaultPlan() != null) {
            applyDefaultState(plan, request.defaultPlan());
        }
        plan = membershipPlanRepository.save(plan);
        if (plan.isDefaultPlan()) {
            membershipPlanRepository.clearOtherDefaults(organizationId, plan.getId());
        }
        reportCacheService.evictOrganization(organizationId);
        return toResponse(plan);
    }

    @Transactional
    public MembershipPlan createDefaultPlanForOrganization(Organization organization, BigDecimal price) {
        MembershipPlan existingDefault = membershipPlanRepository
            .findTopByOrganizationIdAndDefaultPlanTrueAndDeletedFalseOrderByIdAsc(organization.getId())
            .orElse(null);
        if (existingDefault != null) {
            return existingDefault;
        }
        BigDecimal planPrice = price == null ? organization.getMonthlyMembershipFee() : price;
        MembershipPlan plan = new MembershipPlan();
        plan.setOrganization(organization);
        plan.setName(DEFAULT_PLAN_NAME);
        plan.setPrice(MoneyUtils.normalize(planPrice == null ? BigDecimal.ZERO : planPrice));
        plan.setActive(true);
        plan.setDefaultPlan(true);
        return membershipPlanRepository.save(plan);
    }

    @Transactional
    public MembershipPlan ensureRegularDefaultPlan(Organization organization, BigDecimal price) {
        MembershipPlan plan = membershipPlanRepository
            .findTopByOrganizationIdAndDefaultPlanTrueAndDeletedFalseOrderByIdAsc(organization.getId())
            .orElseGet(() -> {
                MembershipPlan created = new MembershipPlan();
                created.setOrganization(organization);
                return created;
            });
        plan.setName(DEFAULT_PLAN_NAME);
        plan.setPrice(MoneyUtils.normalize(price == null ? BigDecimal.ZERO : price));
        plan.setActive(true);
        plan.setDefaultPlan(true);
        plan = membershipPlanRepository.save(plan);
        membershipPlanRepository.clearOtherDefaults(organization.getId(), plan.getId());
        return plan;
    }

    @Transactional(readOnly = true)
    public MembershipPlan requireActivePlan(Long organizationId, Long planId) {
        if (planId == null) {
            return requireDefaultPlan(organizationId);
        }
        return membershipPlanRepository.findByIdAndOrganizationIdAndActiveTrueAndDeletedFalse(planId, organizationId)
            .orElseThrow(() -> new BadRequestException("Select an active membership plan"));
    }

    @Transactional(readOnly = true)
    public MembershipPlan requireDefaultPlan(Long organizationId) {
        return membershipPlanRepository.findTopByOrganizationIdAndDefaultPlanTrueAndDeletedFalseOrderByIdAsc(organizationId)
            .filter(MembershipPlan::isActive)
            .or(() -> membershipPlanRepository.findTopByOrganizationIdAndActiveTrueAndDeletedFalseOrderByDefaultPlanDescNameAsc(organizationId))
            .orElseThrow(() -> new BadRequestException("No active membership plan is configured for this organization"));
    }

    private void applyActiveState(MembershipPlan plan, boolean active) {
        if (!active && plan.isActive()) {
            long activeCount = membershipPlanRepository.countByOrganizationIdAndActiveTrueAndDeletedFalse(plan.getOrganization().getId());
            if (activeCount <= 1) {
                throw new BadRequestException("At least one active membership plan is required");
            }
            if (plan.isDefaultPlan()) {
                throw new BadRequestException("Set another default plan before deactivating this plan");
            }
            if (memberMembershipRepository.existsByMembershipPlanIdAndOrganizationIdAndDeletedFalseAndStatus(
                plan.getId(),
                plan.getOrganization().getId(),
                MembershipStatus.ACTIVE
            )) {
                throw new BadRequestException("Cannot deactivate a plan used by active memberships");
            }
        }
        plan.setActive(active);
    }

    private void applyDefaultState(MembershipPlan plan, boolean defaultPlan) {
        if (!defaultPlan && plan.isDefaultPlan()) {
            throw new BadRequestException("Set another plan as default instead of clearing the current default");
        }
        if (defaultPlan) {
            plan.setActive(true);
            plan.setDefaultPlan(true);
        }
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Membership plan name is required");
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 120) {
            throw new BadRequestException("Membership plan name must be 120 characters or fewer");
        }
        return normalized;
    }

    private List<MembershipPlanResponse> toResponses(List<MembershipPlan> plans) {
        return plans.stream().map(this::toResponse).toList();
    }

    private MembershipPlanResponse toResponse(MembershipPlan plan) {
        return new MembershipPlanResponse(
            plan.getId(),
            plan.getOrganization().getId(),
            plan.getName(),
            plan.getPrice(),
            plan.isActive(),
            plan.isDefaultPlan(),
            plan.getCreatedAt(),
            plan.getUpdatedAt()
        );
    }

    public void softDeletePlan(MembershipPlan plan) {
        plan.setDeleted(true);
        plan.setDeletedAt(OffsetDateTime.now(APP_ZONE));
    }

    @Transactional
    public void deletePlan(Long organizationId, Long planId) {
        MembershipPlan plan = membershipPlanRepository.findByIdAndOrganizationIdAndDeletedFalse(planId, organizationId)
            .orElseThrow(() -> new NotFoundException("Membership plan not found"));

        if (plan.isDefaultPlan()) {
            throw new BadRequestException("Cannot delete the default membership plan. Set another plan as default first.");
        }

        if (memberMembershipRepository.existsByMembershipPlanIdAndOrganizationIdAndDeletedFalseAndStatus(
            plan.getId(),
            organizationId,
            MembershipStatus.ACTIVE
        )) {
            throw new BadRequestException("Cannot delete a plan that is currently used by active memberships.");
        }

        softDeletePlan(plan);
        membershipPlanRepository.save(plan);
        reportCacheService.evictOrganization(organizationId);
    }
}
