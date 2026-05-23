package com.gymflow.service;

import com.gymflow.api.dto.AnalyticsDtos.MonthlyRevenuePoint;
import com.gymflow.api.dto.AnalyticsDtos.MonthlyRevenueResponse;
import com.gymflow.api.dto.AnalyticsDtos.RevenueSegmentationResponse;
import com.gymflow.api.dto.AnalyticsDtos.RevenueSummaryResponse;
import com.gymflow.api.dto.MembershipDtos.PaymentCoverageSummary;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevenueService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final OrganizationSettingService organizationSettingService;

    @Cacheable(value = "revenueReports", key = "T(com.gymflow.security.SecurityUtils).currentUser().organizationId()")
    public RevenueSummaryResponse revenueSummary() {
        Organization organization = currentOrganization();
        int reminderDays = organizationSettingService.getOrCreateSettings(organization.getId()).getReminderDaysBeforeExpiry();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        List<MemberMembership> memberships = memberMembershipRepository.findByOrganizationIdAndDeletedFalse(organization.getId()).stream()
            .filter(membership -> !membership.getMember().isDeleted())
            .peek(membership -> membershipLifecycleService.refreshMembership(membership, reminderDays))
            .toList();
        List<MemberPayment> payments = memberPaymentRepository.findByOrganizationIdAndDeletedFalseOrderByPaymentDateDescCreatedAtDesc(
            organization.getId()
        );
        Map<Long, List<MemberPayment>> paymentsByMembershipId = payments.stream()
            .collect(Collectors.groupingBy(payment -> payment.getMembership().getId()));

        BigDecimal currentRevenue = payments.stream()
            .filter(payment -> payment.getPaymentDate() != null)
            .filter(payment -> !payment.getPaymentDate().isBefore(monthStart) && !payment.getPaymentDate().isAfter(monthEnd))
            .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PAID)
            .map(MemberPayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedRevenue = BigDecimal.ZERO;
        BigDecimal pendingRevenue = BigDecimal.ZERO;
        BigDecimal overdueRevenue = BigDecimal.ZERO;
        long prepaidMembers = 0L;
        long activeMembersPendingCollection = 0L;

        for (MemberMembership membership : memberships) {
            if (membership.getMember().getStatus() != MemberStatus.ACTIVE) {
                continue;
            }
            PaymentCoverageSummary coverage = membershipLifecycleService.buildCoverageSummary(
                membership,
                paymentsByMembershipId.getOrDefault(membership.getId(), List.of()),
                reminderDays
            );
            boolean isCurrentObligation = membership.getStatus() == MembershipStatus.ACTIVE
                && !membership.getStartDate().isAfter(monthEnd)
                && !membership.getEndDate().isBefore(monthStart);
            if (isCurrentObligation) {
                expectedRevenue = expectedRevenue.add(membership.getPlanAmount());
            }
            if (isCurrentObligation && membership.getStatus() == MembershipStatus.ACTIVE) {
                pendingRevenue = pendingRevenue.add(coverage.pendingAmount());
                if (coverage.pendingAmount().compareTo(BigDecimal.ZERO) > 0) {
                    activeMembersPendingCollection++;
                }
            }
            if (membership.getStatus() == MembershipStatus.OVERDUE) {
                overdueRevenue = overdueRevenue.add(coverage.pendingAmount());
            }
            if (membership.getDurationMonths() > 1 && coverage.pendingAmount().compareTo(BigDecimal.ZERO) == 0
                && membership.getStatus() == MembershipStatus.ACTIVE) {
                prepaidMembers++;
            }
        }

        long activeMembers = memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(organization.getId(), MemberStatus.ACTIVE);
        long activeMembersCollected = Math.max(activeMembers - activeMembersPendingCollection, 0);

        return new RevenueSummaryResponse(
            currentRevenue,
            pendingRevenue,
            expectedRevenue,
            overdueRevenue,
            activeMembers,
            prepaidMembers,
            activeMembersCollected,
            activeMembersPendingCollection
        );
    }

    public MonthlyRevenueResponse monthlyRevenue() {
        RevenueSummaryResponse summary = revenueSummary();
        YearMonth currentMonth = YearMonth.now();
        return new MonthlyRevenueResponse(List.of(
            new MonthlyRevenuePoint(
                currentMonth.getYear(),
                currentMonth.getMonthValue(),
                summary.currentRevenue(),
                summary.pendingRevenue(),
                summary.expectedRevenue(),
                summary.overdueRevenue()
            )
        ));
    }

    @Cacheable(value = "revenueReports", key = "'segmentation:' + T(com.gymflow.security.SecurityUtils).currentUser().organizationId()")
    public RevenueSegmentationResponse revenueSegmentationSummary() {
        Organization organization = currentOrganization();
        YearMonth currentMonth = YearMonth.now(APP_ZONE);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        OffsetDateTime monthStartAt = monthStart.atStartOfDay(APP_ZONE).toOffsetDateTime();
        OffsetDateTime monthEndAt = currentMonth.plusMonths(1).atDay(1).atStartOfDay(APP_ZONE).toOffsetDateTime().minusNanos(1);

        BigDecimal admissionRevenue = BigDecimal.ZERO;
        BigDecimal recurringRevenue = BigDecimal.ZERO;
        Set<Long> admittedMemberIds = new java.util.HashSet<>();
        List<MemberPayment> payments = memberPaymentRepository.findByOrganizationIdAndDeletedFalseOrderByPaymentDateDescCreatedAtDesc(
            organization.getId()
        );

        for (MemberPayment payment : payments) {
            if (payment.getPaymentStatus() != PaymentStatus.PAID || payment.getPaymentDate() == null) {
                continue;
            }
            LocalDate paymentDate = payment.getPaymentDate();
            if (paymentDate.isBefore(monthStart) || paymentDate.isAfter(monthEnd)) {
                continue;
            }
            if (payment.getPaymentType() == PaymentType.ADMISSION) {
                admissionRevenue = admissionRevenue.add(payment.getAmount());
                if (payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    admittedMemberIds.add(payment.getMember().getId());
                }
            } else if (payment.getPaymentType() == PaymentType.MEMBERSHIP
                || payment.getPaymentType() == PaymentType.NEW_MEMBERSHIP
                || payment.getPaymentType() == PaymentType.RENEWAL) {
                recurringRevenue = recurringRevenue.add(payment.getAmount());
            }
        }

        return new RevenueSegmentationResponse(
            admittedMemberIds.size(),
            admissionRevenue,
            recurringRevenue,
            admissionRevenue.add(recurringRevenue)
        );
    }

    private Organization currentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
    }
}
