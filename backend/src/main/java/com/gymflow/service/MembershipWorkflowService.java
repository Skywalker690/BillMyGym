package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.CreateMembershipRequest;
import com.gymflow.api.dto.MembershipDtos.MembershipResponse;
import com.gymflow.api.dto.MembershipDtos.SwitchMembershipRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MembershipPlan;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import com.gymflow.domain.enums.IdempotencyResourceType;
import com.gymflow.domain.enums.MemberCreditLedgerReason;
import com.gymflow.domain.enums.MemberCreditLedgerTransactionType;
import com.gymflow.domain.enums.MembershipOperationType;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.shared.money.MoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipWorkflowService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DUE_TODAY_WINDOW_DAYS = 0;

    private final MemberMembershipService memberMembershipService;
    private final MembershipLifecycleService membershipLifecycleService;
    private final MemberCreditLedgerService memberCreditLedgerService;
    private final MembershipOperationIdempotencyService idempotencyService;
    private final OrganizationSettingService organizationSettingService;
    private final MembershipPlanService membershipPlanService;
    private final AuditLogService auditLogService;
    private final ReportCacheService reportCacheService;

    @Transactional
    public MembershipResponse createMembership(Long memberId, CreateMembershipRequest request, String idempotencyKey) {
        Long organizationId = requireOrganizationId();
        int reminderDays = DUE_TODAY_WINDOW_DAYS;
        Member member = memberMembershipService.requireMemberForWrite(memberId, organizationId);
        boolean reactivatedMember = member.getStatus() == com.gymflow.domain.enums.MemberStatus.DEACTIVATED;
        if (reactivatedMember) {
            member.setStatus(com.gymflow.domain.enums.MemberStatus.ACTIVE);
            member.setDeactivatedAt(null);
            member.setDeactivationReason(null);
        }
        String requiredKey = idempotencyService.requireKey(idempotencyKey);
        var existing = idempotencyService.findExisting(
            organizationId,
            memberId,
            MembershipOperationType.MEMBERSHIP_CREATE,
            requiredKey
        ).orElse(null);
        if (existing != null) {
            return memberMembershipService.membershipResponse(existing.getResourceId(), organizationId);
        }

        MemberMembership latestMembership = memberMembershipService.findLatestMembership(memberId, organizationId);
        if (latestMembership != null) {
            membershipLifecycleService.refreshMembership(latestMembership, reminderDays);
        }
        MembershipPlan selectedPlan = resolveCreatePlan(organizationId, request, latestMembership);

        MemberMembership membership = new MemberMembership();
        membership.setMember(member);
        membership.setOrganization(member.getOrganization());
        applyPlanSnapshot(membership, selectedPlan, request.durationMonths());
        membership.setDurationMonths(request.durationMonths());

        LocalDate startDate = resolveCreateStartDate(request, latestMembership);
        // BUG-17: Prevent creating a membership that overlaps an existing ACTIVE one
        if (latestMembership != null
            && latestMembership.getStatus() == MembershipStatus.ACTIVE
            && !startDate.isAfter(latestMembership.getEndDate())) {
            throw new BadRequestException(
                "A new membership cannot start on " + startDate
                + " because the current active membership runs until " + latestMembership.getEndDate()
                + ". Please choose a start date after " + latestMembership.getEndDate().plusDays(1) + "."
            );
        }
        // BUG-08: Only label as renewal when it directly continues from the previous end date
        boolean renewalFlow = latestMembership != null && startDate.equals(latestMembership.getEndDate().plusDays(1));
        membership.setStartDate(startDate);
        membership.setEndDate(membershipLifecycleService.calculateMembershipEndDate(startDate, request.durationMonths()));
        membershipLifecycleService.refreshMembership(membership, reminderDays);
        membership = memberMembershipService.saveMembership(membership);

        idempotencyService.record(
            organizationId,
            memberId,
            MembershipOperationType.MEMBERSHIP_CREATE,
            requiredKey,
            IdempotencyResourceType.MEMBERSHIP,
            membership.getId()
        );
        auditLogService.log(
            renewalFlow ? "MEMBERSHIP_RENEWED" : "MEMBERSHIP_CREATED",
            (renewalFlow ? "Renewed" : "Created") + " membership id=" + membership.getId() + " for member id=" + memberId,
            AuditCategory.MEMBER,
            "MEMBERSHIP"
        );
        auditLogService.logOrganizationActivity(
            renewalFlow ? "MEMBERSHIP_RENEWED" : "MEMBERSHIP_CREATED",
            (renewalFlow ? "Membership renewed for " : "Membership created for ") + member.getFullName(),
            AuditActivityScope.MEMBER_ACTIVITY,
            AuditEventStatus.INFO,
            "MEMBERSHIP",
            organizationId,
            member.getFullName()
        );
        if (reactivatedMember) {
            auditLogService.logOrganizationActivity(
                "MEMBER_REACTIVATED",
                "Member reactivated when a new membership was created for " + member.getFullName(),
                AuditActivityScope.MEMBER_STATUS_ACTIVITY,
                AuditEventStatus.INFO,
                "MEMBER",
                organizationId,
                member.getFullName()
            );
        }
        reportCacheService.evictOrganization(organizationId);
        return memberMembershipService.membershipResponse(membership.getId(), organizationId);
    }

    @Transactional
    public MembershipResponse switchMembership(Long memberId, SwitchMembershipRequest request, String idempotencyKey) {
        Long organizationId = requireOrganizationId();
        Member member = memberMembershipService.requireActiveMember(memberId, organizationId);
        String requiredKey = idempotencyService.requireKey(idempotencyKey);
        var existing = idempotencyService.findExisting(
            organizationId,
            memberId,
            MembershipOperationType.MEMBERSHIP_SWITCH,
            requiredKey
        ).orElse(null);
        if (existing != null) {
            return memberMembershipService.membershipResponse(existing.getResourceId(), organizationId);
        }

        MemberMembership activeMembership = memberMembershipService.findActiveMembershipForUpdate(memberId, organizationId);

        LocalDate switchDate = request.switchDate();
        int completedMonths = resolveCompletedMonths(activeMembership, switchDate);
        int remainingMonths = activeMembership.getDurationMonths() - completedMonths;
        if (remainingMonths <= 0) {
            throw new BadRequestException("No unused membership months remain for package switch");
        }

        BigDecimal successfulPayments = membershipLifecycleService.successfulPaymentTotal(
            memberMembershipService.paymentsForMembership(activeMembership.getId())
        );
        BigDecimal switchCredit = MoneyUtils.normalize(
            successfulPayments.min(activeMembership.getPlanAmount())
                .multiply(BigDecimal.valueOf(remainingMonths))
                .divide(BigDecimal.valueOf(activeMembership.getDurationMonths()), MoneyUtils.SCALE, MoneyUtils.ROUNDING_MODE)
        );

        activeMembership.setEndDate(switchDate.minusDays(1));
        activeMembership.setStatus(MembershipStatus.ACTIVE);

        if (switchCredit.compareTo(BigDecimal.ZERO) > 0) {
            memberCreditLedgerService.createEntry(
                member,
                member.getOrganization(),
                activeMembership,
                switchCredit,
                MemberCreditLedgerTransactionType.CREDIT,
                MemberCreditLedgerReason.PACKAGE_SWITCH,
                request.notes()
            );
        }

        MemberMembership newMembership = new MemberMembership();
        newMembership.setMember(member);
        newMembership.setOrganization(member.getOrganization());
        newMembership.setDurationMonths(request.durationMonths());
        applyPlanSnapshot(newMembership, membershipPlanService.requireActivePlan(organizationId, request.membershipPlanId()), request.durationMonths());
        newMembership.setStartDate(switchDate);
        newMembership.setEndDate(membershipLifecycleService.calculateMembershipEndDate(switchDate, request.durationMonths()));
        membershipLifecycleService.refreshMembership(
            newMembership,
            DUE_TODAY_WINDOW_DAYS
        );
        newMembership = memberMembershipService.saveMembership(newMembership);

        BigDecimal availableWallet = memberCreditLedgerService.balanceForMember(memberId, organizationId);
        BigDecimal debitAmount = availableWallet.min(newMembership.getPlanAmount()).max(BigDecimal.ZERO);
        if (debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            memberCreditLedgerService.createEntry(
                member,
                member.getOrganization(),
                newMembership,
                debitAmount,
                MemberCreditLedgerTransactionType.DEBIT,
                MemberCreditLedgerReason.PACKAGE_SWITCH,
                request.notes()
            );
        }

        idempotencyService.record(
            organizationId,
            memberId,
            MembershipOperationType.MEMBERSHIP_SWITCH,
            requiredKey,
            IdempotencyResourceType.MEMBERSHIP,
            newMembership.getId()
        );
        auditLogService.log(
            "MEMBERSHIP_PACKAGE_SWITCHED",
            "Switched package for member id=" + memberId + " from membership id=" + activeMembership.getId()
                + " to membership id=" + newMembership.getId(),
            AuditCategory.MEMBER,
            "MEMBERSHIP"
        );
        reportCacheService.evictOrganization(organizationId);
        return memberMembershipService.membershipResponse(newMembership.getId(), organizationId);
    }

    private Long requireOrganizationId() {
        Long organizationId = com.gymflow.security.SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }

    private LocalDate resolveCreateStartDate(CreateMembershipRequest request, MemberMembership latestMembership) {
        if (latestMembership == null) {
            return request.startDate() == null ? LocalDate.now(APP_ZONE) : request.startDate();
        }

        LocalDate nextContinuationDate = latestMembership.getEndDate().plusDays(1);

        if (request.startDate() != null) {
            if (latestMembership.getStatus() == com.gymflow.domain.enums.MembershipStatus.ACTIVE
                && request.startDate().isBefore(nextContinuationDate)) {
                return nextContinuationDate;
            }
            return request.startDate();
        }

        // BUG-09/16: When the latest membership is OVERDUE and no startDate given,
        // default to today — NOT endDate+1 (which could be months in the past, causing an
        // immediately-expired membership the moment it is saved).
        if (latestMembership.getStatus() == com.gymflow.domain.enums.MembershipStatus.OVERDUE) {
            return LocalDate.now(APP_ZONE);
        }

        // For an ACTIVE latest membership, continue seamlessly from the day after it ends
        return nextContinuationDate;
    }

    private int resolveCompletedMonths(MemberMembership activeMembership, LocalDate switchDate) {
        if (switchDate.isBefore(activeMembership.getStartDate()) || switchDate.isAfter(activeMembership.getEndDate())) {
            throw new BadRequestException("Switch date must fall within the active membership period");
        }
        long monthsBetween = ChronoUnit.MONTHS.between(activeMembership.getStartDate(), switchDate);
        LocalDate expectedBoundary = activeMembership.getStartDate().plusMonths(monthsBetween);
        if (!expectedBoundary.equals(switchDate)) {
            throw new BadRequestException("Package switches are only allowed on month boundaries");
        }
        return Math.toIntExact(monthsBetween);
    }

    private LocalDate maxDate(LocalDate base, LocalDate comparison, LocalDate requested) {
        LocalDate result = comparison != null && comparison.isAfter(base) ? comparison : base;
        if (requested != null && requested.isAfter(result)) {
            return requested;
        }
        return result;
    }

    private BigDecimal scale(BigDecimal amount) {
        return MoneyUtils.normalize(amount);
    }

    private MembershipPlan resolveCreatePlan(Long organizationId, CreateMembershipRequest request, MemberMembership latestMembership) {
        if (request.membershipPlanId() != null) {
            return membershipPlanService.requireActivePlan(organizationId, request.membershipPlanId());
        }
        if (latestMembership != null && latestMembership.getMembershipPlan() != null) {
            try {
                return membershipPlanService.requireActivePlan(organizationId, latestMembership.getMembershipPlan().getId());
            } catch (BadRequestException ignored) {
                // Fall back to the current default plan when an old plan was deactivated.
            }
        }
        return membershipPlanService.requireDefaultPlan(organizationId);
    }

    private void applyPlanSnapshot(MemberMembership membership, MembershipPlan plan, int durationMonths) {
        membership.setMembershipPlan(plan);
        membership.setPlanName(plan.getName());
        membership.setMonthlyPlanPrice(scale(plan.getPrice()));
        membership.setPlanAmount(scale(plan.getPrice().multiply(BigDecimal.valueOf(durationMonths))));
    }
}
