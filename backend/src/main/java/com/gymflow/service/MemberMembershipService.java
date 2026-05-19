package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.CreateMembershipRequest;
import com.gymflow.api.dto.MembershipDtos.MembershipHistoryResponse;
import com.gymflow.api.dto.MembershipDtos.MembershipResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberMembershipService {

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final MembershipWorkflowService membershipWorkflowService;
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
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
        Map<Long, List<MemberPayment>> paymentsByMembershipId = loadPaymentsByMembershipId(memberships);
        Map<Long, java.math.BigDecimal> appliedCreditByMembershipId = memberCreditLedgerService.appliedCreditForMemberships(
            memberships.stream().map(MemberMembership::getId).toList()
        );
        List<MembershipResponse> responses = memberships.stream()
            .map(membership -> toResponse(
                membership,
                paymentsByMembershipId.getOrDefault(membership.getId(), List.of()),
                appliedCreditByMembershipId.getOrDefault(membership.getId(), java.math.BigDecimal.ZERO),
                reminderDays
            ))
            .toList();
        return new MembershipHistoryResponse(responses);
    }

    @Transactional
    public void refreshMembershipStatuses(Long organizationId) {
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
        for (MemberMembership membership : memberMembershipRepository.findByOrganizationIdAndDeletedFalse(organizationId)) {
            membershipLifecycleService.refreshMembership(membership, reminderDays);
        }
    }

    private MembershipResponse toResponse(
        MemberMembership membership,
        List<MemberPayment> payments,
        java.math.BigDecimal appliedCreditAmount,
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
            memberCreditLedgerService.balanceForMember(membership.getMember().getId(), membership.getOrganization().getId()),
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
