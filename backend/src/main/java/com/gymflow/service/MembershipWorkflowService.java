package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.CreateMembershipRequest;
import com.gymflow.api.dto.MembershipDtos.MembershipResponse;
import com.gymflow.api.dto.MembershipDtos.SwitchMembershipRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.IdempotencyResourceType;
import com.gymflow.domain.enums.MemberCreditLedgerReason;
import com.gymflow.domain.enums.MemberCreditLedgerTransactionType;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipOperationType;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipWorkflowService {

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final MemberCreditLedgerService memberCreditLedgerService;
    private final MembershipOperationIdempotencyService idempotencyService;
    private final OrganizationSettingService organizationSettingService;
    private final AuditLogService auditLogService;

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public MembershipResponse createMembership(Long memberId, CreateMembershipRequest request, String idempotencyKey) {
        Member member = requireActiveMember(memberId);
        Long organizationId = member.getOrganization().getId();
        String requiredKey = idempotencyService.requireKey(idempotencyKey);
        var existing = idempotencyService.findExisting(
            organizationId,
            memberId,
            MembershipOperationType.MEMBERSHIP_CREATE,
            requiredKey
        ).orElse(null);
        if (existing != null) {
            return membershipResponse(existing.getResourceId(), organizationId);
        }

        MemberMembership activeMembership = memberMembershipRepository
            .findByMemberIdAndOrganizationIdAndStatusAndDeletedFalse(memberId, organizationId, MembershipStatus.ACTIVE)
            .orElse(null);
        if (activeMembership != null && isPackageSwitch(activeMembership, request)) {
            throw new BadRequestException("Use the package switch flow when changing an active membership package");
        }

        MemberMembership membership = new MemberMembership();
        membership.setMember(member);
        membership.setOrganization(member.getOrganization());
        membership.setPlanName(resolvePlanName(request.planName(), request.durationMonths()));
        membership.setDurationMonths(request.durationMonths());
        membership.setPlanAmount(scale(request.planAmount()));

        LocalDate startDate = resolveCreateStartDate(request, activeMembership);
        membership.setStartDate(startDate);
        membership.setEndDate(membershipLifecycleService.calculateMembershipEndDate(startDate, request.durationMonths()));
        membershipLifecycleService.refreshMembership(
            membership,
            organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry()
        );
        membership = memberMembershipRepository.save(membership);

        idempotencyService.record(
            organizationId,
            memberId,
            MembershipOperationType.MEMBERSHIP_CREATE,
            requiredKey,
            IdempotencyResourceType.MEMBERSHIP,
            membership.getId()
        );
        auditLogService.log(
            "MEMBERSHIP_CREATED",
            "Created membership id=" + membership.getId() + " for member id=" + memberId,
            AuditCategory.MEMBER,
            "MEMBERSHIP"
        );
        return membershipResponse(membership.getId(), organizationId);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public MembershipResponse switchMembership(Long memberId, SwitchMembershipRequest request, String idempotencyKey) {
        Member member = requireActiveMember(memberId);
        Long organizationId = member.getOrganization().getId();
        String requiredKey = idempotencyService.requireKey(idempotencyKey);
        var existing = idempotencyService.findExisting(
            organizationId,
            memberId,
            MembershipOperationType.MEMBERSHIP_SWITCH,
            requiredKey
        ).orElse(null);
        if (existing != null) {
            return membershipResponse(existing.getResourceId(), organizationId);
        }

        MemberMembership activeMembership = memberMembershipRepository.findActiveMembershipForUpdate(memberId, organizationId)
            .orElseThrow(() -> new BadRequestException("An active membership is required for package switch"));

        LocalDate switchDate = request.switchDate();
        int completedMonths = resolveCompletedMonths(activeMembership, switchDate);
        int remainingMonths = activeMembership.getDurationMonths() - completedMonths;
        if (remainingMonths <= 0) {
            throw new BadRequestException("No unused membership months remain for package switch");
        }

        BigDecimal successfulPayments = membershipLifecycleService.successfulPaymentTotal(
            memberPaymentRepository.findByMembershipIdAndDeletedFalseOrderByPaymentDateAscCreatedAtAsc(activeMembership.getId())
        );
        BigDecimal switchCredit = scale(
            successfulPayments.min(activeMembership.getPlanAmount())
                .multiply(BigDecimal.valueOf(remainingMonths))
                .divide(BigDecimal.valueOf(activeMembership.getDurationMonths()), 2, RoundingMode.HALF_UP)
        );

        activeMembership.setStatus(MembershipStatus.COMPLETED);

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
        newMembership.setPlanName(request.planName());
        newMembership.setDurationMonths(request.durationMonths());
        newMembership.setPlanAmount(scale(request.planAmount()));
        newMembership.setStartDate(switchDate);
        newMembership.setEndDate(membershipLifecycleService.calculateMembershipEndDate(switchDate, request.durationMonths()));
        membershipLifecycleService.refreshMembership(
            newMembership,
            organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry()
        );
        newMembership = memberMembershipRepository.save(newMembership);

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
        return membershipResponse(newMembership.getId(), organizationId);
    }

    @Transactional(readOnly = true)
    public MembershipResponse membershipResponse(Long membershipId, Long organizationId) {
        MemberMembership membership = memberMembershipRepository.findByIdAndOrganizationIdAndDeletedFalse(membershipId, organizationId)
            .orElseThrow(() -> new NotFoundException("Membership not found"));
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
        List<MemberPayment> payments = memberPaymentRepository.findByMembershipIdAndDeletedFalseOrderByPaymentDateAscCreatedAtAsc(membershipId);
        return new MembershipResponse(
            membership.getId(),
            membership.getMember().getId(),
            membership.getOrganization().getId(),
            membership.getPlanName(),
            membership.getDurationMonths(),
            membership.getPlanAmount(),
            membership.getStartDate(),
            membership.getEndDate(),
            membership.getStatus(),
            memberCreditLedgerService.balanceForMember(membership.getMember().getId(), organizationId),
            membership.getLastSentAt(),
            membership.getCreatedAt(),
            membership.getUpdatedAt(),
            membershipLifecycleService.buildCoverageSummary(
                membership,
                payments,
                memberCreditLedgerService.appliedCreditForMembership(membershipId),
                reminderDays
            )
        );
    }

    private Member requireActiveMember(Long memberId) {
        Long organizationId = requireOrganizationId();
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, organizationId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new BadRequestException("Re-activate the member before creating a new membership");
        }
        return member;
    }

    private Long requireOrganizationId() {
        Long organizationId = com.gymflow.security.SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }

    private LocalDate resolveCreateStartDate(CreateMembershipRequest request, MemberMembership activeMembership) {
        if (activeMembership == null) {
            return request.startDate() == null ? LocalDate.now() : request.startDate();
        }
        return maxDate(LocalDate.now(), activeMembership.getEndDate().plusDays(1), request.startDate());
    }

    private boolean isPackageSwitch(MemberMembership activeMembership, CreateMembershipRequest request) {
        String requestedPlan = resolvePlanName(request.planName(), request.durationMonths());
        return !activeMembership.getPlanName().equalsIgnoreCase(requestedPlan)
            || activeMembership.getDurationMonths() != request.durationMonths()
            || activeMembership.getPlanAmount().compareTo(scale(request.planAmount())) != 0;
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

    private String resolvePlanName(String requestPlanName, int durationMonths) {
        if (requestPlanName != null && !requestPlanName.isBlank()) {
            return requestPlanName.trim();
        }
        return durationMonths == 1 ? "Monthly Membership" : durationMonths + "-Month Membership";
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
