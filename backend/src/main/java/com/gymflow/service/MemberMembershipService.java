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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberMembershipService {

    private static final Set<MembershipStatus> ACTIVE_MEMBERSHIP_STATUSES = Set.of(
        MembershipStatus.ACTIVE
    );

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final OrganizationSettingService organizationSettingService;
    private final AuditLogService auditLogService;

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public MembershipResponse createMembership(Long memberId, CreateMembershipRequest request) {
        Long organizationId = requireOrganizationId();
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, organizationId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new BadRequestException("Re-activate the member before creating a new membership");
        }

        List<MemberMembership> history = memberMembershipRepository
            .findByMemberIdAndOrganizationIdAndDeletedFalseOrderByStartDateDescCreatedAtDesc(memberId, organizationId);
        List<MemberMembership> activeMemberships = history.stream()
            .filter(membership -> ACTIVE_MEMBERSHIP_STATUSES.contains(membership.getStatus()))
            .toList();
        if (activeMemberships.size() > 1) {
            throw new BadRequestException("Member has multiple active memberships. Resolve data integrity issue before continuing");
        }
        MemberMembership activeMembership = activeMemberships.isEmpty() ? null : activeMemberships.get(0);

        MemberMembership membership = new MemberMembership();
        membership.setMember(member);
        membership.setOrganization(member.getOrganization());
        membership.setPlanName(request.planName());
        membership.setDurationMonths(request.durationMonths());
        membership.setPlanAmount(request.planAmount());

        LocalDate startDate = resolveStartDate(request, history, activeMembership);
        membership.setStartDate(startDate);
        membership.setEndDate(membershipLifecycleService.calculateMembershipEndDate(startDate, request.durationMonths()));
        membershipLifecycleService.refreshMembership(
            membership,
            organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry()
        );

        if (activeMembership != null) {
            activeMembership.setStatus(MembershipStatus.COMPLETED);
            if (!samePlan(activeMembership, request)) {
                auditLogService.log(
                    "MEMBERSHIP_PACKAGE_SWITCHED",
                    "Switched package for member id=" + memberId + " from membership id=" + activeMembership.getId(),
                    AuditCategory.MEMBER,
                    "MEMBERSHIP"
                );
            }
        }

        membership = memberMembershipRepository.save(membership);
        auditLogService.log(
            "MEMBERSHIP_CREATED",
            "Created membership id=" + membership.getId() + " for member id=" + memberId,
            AuditCategory.MEMBER,
            "MEMBERSHIP"
        );
        return toResponse(
            membership,
            List.of(),
            organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry()
        );
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
        List<MembershipResponse> responses = memberships.stream()
            .map(membership -> toResponse(
                membership,
                paymentsByMembershipId.getOrDefault(membership.getId(), List.of()),
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

    private LocalDate resolveStartDate(
        CreateMembershipRequest request,
        List<MemberMembership> history,
        MemberMembership activeMembership
    ) {
        if (activeMembership != null) {
            boolean packageSwitch = !samePlan(activeMembership, request);
            if (packageSwitch) {
                return request.startDate() == null ? LocalDate.now() : request.startDate();
            }
            return maxDate(LocalDate.now(), activeMembership.getEndDate().plusDays(1), request.startDate());
        }
        if (!history.isEmpty()) {
            return maxDate(LocalDate.now(), history.get(0).getEndDate().plusDays(1), request.startDate());
        }
        return request.startDate() == null ? LocalDate.now() : request.startDate();
    }

    private boolean samePlan(MemberMembership activeMembership, CreateMembershipRequest request) {
        return activeMembership.getPlanName().equals(request.planName())
            && activeMembership.getDurationMonths() == request.durationMonths()
            && activeMembership.getPlanAmount().compareTo(request.planAmount()) == 0;
    }

    private LocalDate maxDate(LocalDate base, LocalDate comparison, LocalDate requested) {
        LocalDate result = comparison != null && comparison.isAfter(base) ? comparison : base;
        if (requested != null && requested.isAfter(result)) {
            return requested;
        }
        return result;
    }

    private MembershipResponse toResponse(
        MemberMembership membership,
        List<MemberPayment> payments,
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
            membership.getLastSentAt(),
            membership.getCreatedAt(),
            membership.getUpdatedAt(),
            membershipLifecycleService.buildCoverageSummary(membership, payments, reminderDays)
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
