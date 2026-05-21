package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.PaymentCoverageSummary;
import com.gymflow.api.dto.MembershipDtos.CreateMembershipRequest;
import com.gymflow.api.dto.MembershipDtos.MembershipHistoryResponse;
import com.gymflow.api.dto.MembershipDtos.MembershipResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import java.math.BigDecimal;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.security.SecurityUtils;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberMembershipService {

    private static final String AUTO_DEACTIVATION_REASON =
        "Automatically deactivated after fully paid membership expired without renewal";

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final MemberCreditLedgerService memberCreditLedgerService;
    private final OrganizationSettingService organizationSettingService;

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public MembershipResponse createMembership(Long memberId, CreateMembershipRequest request) {
        throw new BadRequestException("Use the idempotent membership workflow endpoint");
    }

    @Transactional
    public MembershipHistoryResponse listMemberships(Long memberId) {
        Long organizationId = requireOrganizationId();
        memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, organizationId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        List<MemberMembership> memberships = memberMembershipRepository
            .findByMemberIdAndOrganizationIdAndDeletedFalseOrderByStartDateDescCreatedAtDesc(memberId, organizationId);
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
        java.math.BigDecimal walletBalance = memberCreditLedgerService.balanceForMember(memberId, organizationId);
        Map<Long, List<MemberPayment>> paymentsByMembershipId = loadPaymentsByMembershipId(memberships);
        Map<Long, java.math.BigDecimal> appliedCreditByMembershipId = memberCreditLedgerService.appliedCreditForMemberships(
            memberships.stream().map(MemberMembership::getId).toList()
        );
        List<MembershipResponse> responses = memberships.stream()
            .map(membership -> toResponse(
                membership,
                paymentsByMembershipId.getOrDefault(membership.getId(), List.of()),
                appliedCreditByMembershipId.getOrDefault(membership.getId(), java.math.BigDecimal.ZERO),
                walletBalance,
                reminderDays
            ))
            .toList();
        return new MembershipHistoryResponse(responses);
    }

    @Transactional
    public void refreshMembershipStatuses(Long organizationId) {
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
        List<MemberMembership> memberships = memberMembershipRepository.findByOrganizationIdAndDeletedFalse(organizationId);
        for (MemberMembership membership : memberships) {
            membershipLifecycleService.refreshMembership(membership, reminderDays);
        }
        reconcileMemberStatuses(memberships, reminderDays);
    }

    @Transactional
    public Member requireActiveMember(Long memberId, Long organizationId) {
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalseForUpdate(memberId, organizationId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new BadRequestException("Re-activate the member before creating a new membership");
        }
        return member;
    }

    @Transactional
    public MemberMembership findActiveMembership(Long memberId, Long organizationId) {
        return memberMembershipRepository
            .findByMemberIdAndOrganizationIdAndStatusAndDeletedFalse(memberId, organizationId, MembershipStatus.ACTIVE)
            .orElse(null);
    }

    @Transactional
    public MemberMembership findActiveMembershipForUpdate(Long memberId, Long organizationId) {
        return memberMembershipRepository.findActiveMembershipForUpdate(memberId, organizationId)
            .orElseThrow(() -> new BadRequestException("An active membership is required for package switch"));
    }

    @Transactional
    public MemberMembership saveMembership(MemberMembership membership) {
        return memberMembershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public MembershipResponse membershipResponse(Long membershipId, Long organizationId) {
        MemberMembership membership = memberMembershipRepository.findByIdAndOrganizationIdAndDeletedFalse(membershipId, organizationId)
            .orElseThrow(() -> new NotFoundException("Membership not found"));
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
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
                paymentsForMembership(membershipId),
                memberCreditLedgerService.appliedCreditForMembership(membershipId),
                reminderDays
            )
        );
    }

    @Transactional(readOnly = true)
    public List<MemberPayment> paymentsForMembership(Long membershipId) {
        return memberPaymentRepository.findByMembershipIdAndDeletedFalseOrderByPaymentDateAscCreatedAtAsc(membershipId);
    }

    private MembershipResponse toResponse(
        MemberMembership membership,
        List<MemberPayment> payments,
        java.math.BigDecimal appliedCreditAmount,
        java.math.BigDecimal walletBalance,
        int reminderDays
    ) {
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
            walletBalance,
            membership.getLastSentAt(),
            membership.getCreatedAt(),
            membership.getUpdatedAt(),
            membershipLifecycleService.buildCoverageSummary(membership, payments, appliedCreditAmount, reminderDays)
        );
    }

    private Map<Long, List<MemberPayment>> loadPaymentsByMembershipId(Collection<MemberMembership> memberships) {
        List<Long> membershipIds = memberships.stream().map(MemberMembership::getId).toList();
        if (membershipIds.isEmpty()) {
            return Map.of();
        }
        return memberPaymentRepository.findByMembershipIdInAndDeletedFalse(membershipIds).stream()
            .collect(Collectors.groupingBy(
                payment -> payment.getMembership().getId(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    payments -> payments.stream()
                        .sorted((left, right) -> {
                            int paymentDateComparison = left.getPaymentDate().compareTo(right.getPaymentDate());
                            if (paymentDateComparison != 0) {
                                return paymentDateComparison;
                            }
                            return left.getCreatedAt().compareTo(right.getCreatedAt());
                        })
                        .toList()
                )
            ));
    }

    private void reconcileMemberStatuses(List<MemberMembership> memberships, int reminderDays) {
        if (memberships.isEmpty()) {
            return;
        }

        Map<Long, List<MemberMembership>> membershipsByMemberId = memberships.stream()
            .filter(membership -> !membership.getMember().isDeleted())
            .collect(Collectors.groupingBy(membership -> membership.getMember().getId()));
        Map<Long, List<MemberPayment>> paymentsByMembershipId = loadPaymentsByMembershipId(memberships);

        for (List<MemberMembership> memberMemberships : membershipsByMemberId.values()) {
            Member member = memberMemberships.getFirst().getMember();
            MemberMembership latestMembership = latestMembership(memberMemberships);
            if (latestMembership == null) {
                continue;
            }

            PaymentCoverageSummary coverage = membershipLifecycleService.buildCoverageSummary(
                latestMembership,
                paymentsByMembershipId.getOrDefault(latestMembership.getId(), List.of()),
                reminderDays
            );
            boolean fullyPaid = coverage.pendingAmount().compareTo(BigDecimal.ZERO) == 0;
            boolean membershipExpiredWithoutRenewal =
                latestMembership.getStatus() == MembershipStatus.EXPIRED
                    || latestMembership.getStatus() == MembershipStatus.OVERDUE;

            if (member.getStatus() == MemberStatus.ACTIVE && fullyPaid && membershipExpiredWithoutRenewal) {
                member.setStatus(MemberStatus.DEACTIVATED);
                if (member.getDeactivatedAt() == null) {
                    member.setDeactivatedAt(OffsetDateTime.now());
                }
                member.setDeactivationReason(AUTO_DEACTIVATION_REASON);
                continue;
            }

            if (member.getStatus() == MemberStatus.DEACTIVATED
                && AUTO_DEACTIVATION_REASON.equals(member.getDeactivationReason())
                && latestMembership.getStatus() == MembershipStatus.ACTIVE) {
                member.setStatus(MemberStatus.ACTIVE);
                member.setDeactivatedAt(null);
                member.setDeactivationReason(null);
            }
        }
    }

    private MemberMembership latestMembership(List<MemberMembership> memberships) {
        return memberships.stream()
            .max(
                Comparator.comparing(MemberMembership::getEndDate)
                    .thenComparing(MemberMembership::getStartDate)
                    .thenComparing(MemberMembership::getCreatedAt)
            )
            .orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public void markReminderSent(List<MemberMembership> memberships, OffsetDateTime sentAt) {
        for (MemberMembership membership : memberships) {
            membership.setLastSentAt(sentAt);
        }
    }

    private Long requireOrganizationId() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }
}
