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
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import java.math.BigDecimal;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.security.SecurityUtils;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DUE_TODAY_WINDOW_DAYS = 0;
    private static final String AUTO_DEACTIVATION_REASON =
        "Automatically deactivated after membership expired without renewal for 30 days";

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final MemberCreditLedgerService memberCreditLedgerService;
    private final OrganizationSettingService organizationSettingService;
    private final AuditLogService auditLogService;

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
        int reminderDays = DUE_TODAY_WINDOW_DAYS;
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
        int reminderDays = DUE_TODAY_WINDOW_DAYS;
        List<MemberMembership> memberships = memberMembershipRepository.findByOrganizationIdAndDeletedFalse(organizationId);
        for (MemberMembership membership : memberships) {
            membershipLifecycleService.refreshMembership(membership, reminderDays);
        }
        reconcileMemberStatuses(memberships, reminderDays);
    }

    @Transactional
    public Member requireMemberForWrite(Long memberId, Long organizationId) {
        return memberRepository.findByIdAndOrganizationIdAndDeletedFalseForUpdate(memberId, organizationId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
    }

    @Transactional
    public Member requireActiveMember(Long memberId, Long organizationId) {
        Member member = requireMemberForWrite(memberId, organizationId);
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

    @Transactional(readOnly = true)
    public MemberMembership findLatestMembership(Long memberId, Long organizationId) {
        return memberMembershipRepository
            .findTopByMemberIdAndOrganizationIdAndDeletedFalseOrderByEndDateDescStartDateDescCreatedAtDesc(memberId, organizationId)
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

    @Transactional
    public void deactivateLatestMembershipForMember(Member member) {
        if (member == null || member.isDeleted() || member.getOrganization() == null || member.getId() == null) {
            return;
        }
        MemberMembership latestMembership = findLatestMembership(member.getId(), member.getOrganization().getId());
        if (latestMembership != null && latestMembership.getStatus() != MembershipStatus.OVERDUE) {
            latestMembership.setStatus(MembershipStatus.OVERDUE);
            latestMembership.setLastSentAt(null);
        }
    }

    @Transactional(readOnly = true)
    public MembershipResponse membershipResponse(Long membershipId, Long organizationId) {
        MemberMembership membership = memberMembershipRepository.findByIdAndOrganizationIdAndDeletedFalse(membershipId, organizationId)
            .orElseThrow(() -> new NotFoundException("Membership not found"));
        int reminderDays = DUE_TODAY_WINDOW_DAYS;
        return new MembershipResponse(
            membership.getId(),
            membership.getMember().getId(),
            membership.getOrganization().getId(),
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
        for (List<MemberMembership> memberMemberships : membershipsByMemberId.values()) {
            Member member = memberMemberships.getFirst().getMember();
            MemberMembership latestMembership = latestMembership(memberMemberships);
            if (latestMembership == null) {
                continue;
            }

            boolean membershipExpiredWithoutRenewal =
                latestMembership.getStatus() == MembershipStatus.OVERDUE;
            boolean isOverdueBy30Days = !latestMembership.getEndDate().plusDays(30).isAfter(LocalDate.now(APP_ZONE));

            if (member.getStatus() == MemberStatus.ACTIVE && membershipExpiredWithoutRenewal && isOverdueBy30Days) {
                member.setStatus(MemberStatus.DEACTIVATED);
                member.setDeactivatedAt(OffsetDateTime.now(APP_ZONE));
                member.setDeactivationReason(AUTO_DEACTIVATION_REASON);
                latestMembership.setStatus(MembershipStatus.OVERDUE);
                latestMembership.setLastSentAt(null);
                auditLogService.logOrganizationActivity(
                    "MEMBER_DEACTIVATED",
                    "Member auto-deactivated after 30 days overdue: " + member.getFullName(),
                    AuditActivityScope.MEMBER_STATUS_ACTIVITY,
                    AuditEventStatus.INFO,
                    "MEMBER",
                    member.getOrganization().getId(),
                    member.getFullName()
                );
                continue;
            }

            if (member.getStatus() == MemberStatus.DEACTIVATED) {
                if (latestMembership.getStatus() != MembershipStatus.OVERDUE) {
                    latestMembership.setStatus(MembershipStatus.OVERDUE);
                    latestMembership.setLastSentAt(null);
                }
                continue;
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
