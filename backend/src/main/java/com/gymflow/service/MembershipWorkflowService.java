package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.CreateMembershipRequest;
import com.gymflow.api.dto.MembershipDtos.MembershipResponse;
import com.gymflow.api.dto.MembershipDtos.SwitchMembershipRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
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
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipWorkflowService {

    private final MemberMembershipService memberMembershipService;
    private final MembershipLifecycleService membershipLifecycleService;
    private final MemberCreditLedgerService memberCreditLedgerService;
    private final MembershipOperationIdempotencyService idempotencyService;
    private final OrganizationSettingService organizationSettingService;
    private final AuditLogService auditLogService;

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public MembershipResponse createMembership(Long memberId, CreateMembershipRequest request, String idempotencyKey) {
        Long organizationId = requireOrganizationId();
        Member member = memberMembershipService.requireActiveMember(memberId, organizationId);
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
        boolean renewalFlow = latestMembership != null;

        MemberMembership membership = new MemberMembership();
        membership.setMember(member);
        membership.setOrganization(member.getOrganization());
        membership.setDurationMonths(request.durationMonths());
        membership.setPlanAmount(resolveMembershipAmount(member, request.durationMonths()));

        LocalDate startDate = resolveCreateStartDate(request, latestMembership);
        membership.setStartDate(startDate);
        membership.setEndDate(membershipLifecycleService.calculateMembershipEndDate(startDate, request.durationMonths()));
        membershipLifecycleService.refreshMembership(
            membership,
            organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry()
        );
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
        return memberMembershipService.membershipResponse(membership.getId(), organizationId);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
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

        activeMembership.setStatus(MembershipStatus.DEACTIVATED);

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
        newMembership.setPlanAmount(scale(request.planAmount()));
        newMembership.setStartDate(switchDate);
        newMembership.setEndDate(membershipLifecycleService.calculateMembershipEndDate(switchDate, request.durationMonths()));
        membershipLifecycleService.refreshMembership(
            newMembership,
            organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry()
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
            return request.startDate() == null ? LocalDate.now() : request.startDate();
        }
        
        LocalDate nextContinuationDate = latestMembership.getEndDate().plusDays(1);
        
        if (request.startDate() != null) {
            if (latestMembership.getStatus() == com.gymflow.domain.enums.MembershipStatus.ACTIVE && request.startDate().isBefore(nextContinuationDate)) {
                return nextContinuationDate;
            }
            return request.startDate();
        }
        
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

    private BigDecimal resolveMembershipAmount(Member member, int durationMonths) {
        BigDecimal recurringFee = member.getOrganization().getMonthlyMembershipFee() == null
            ? BigDecimal.ZERO
            : member.getOrganization().getMonthlyMembershipFee();
        return MoneyUtils.normalize(recurringFee.multiply(BigDecimal.valueOf(durationMonths)));
    }
}
