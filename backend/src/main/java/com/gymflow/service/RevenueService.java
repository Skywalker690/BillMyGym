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
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevenueService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<PaymentType> COLLECTED_REVENUE_TYPES = List.of(
        PaymentType.ADMISSION,
        PaymentType.MEMBERSHIP,
        PaymentType.NEW_MEMBERSHIP,
        PaymentType.RENEWAL
    );
    private static final List<PaymentType> MEMBERSHIP_COVERAGE_TYPES = List.of(
        PaymentType.MEMBERSHIP,
        PaymentType.NEW_MEMBERSHIP,
        PaymentType.RENEWAL
    );

    private final OrganizationRepository organizationRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final OrganizationSettingService organizationSettingService;

    @Cacheable(value = "revenueReports", key = "T(com.gymflow.security.SecurityUtils).currentUser().organizationId()")
    public RevenueSummaryResponse revenueSummary() {
        Organization organization = currentOrganization();
        int reminderDays = 0;
        YearMonth currentMonth = YearMonth.now(APP_ZONE);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        List<MemberMembership> memberships = memberMembershipRepository.findByOrganizationIdAndDeletedFalse(organization.getId()).stream()
            .filter(membership -> !membership.getMember().isDeleted())
            .peek(membership -> membershipLifecycleService.refreshMembership(membership, reminderDays))
            .toList();
        Map<Long, BigDecimal> paidAmountByMembershipId = memberPaymentRepository
            .sumSuccessfulAmountsByMembershipForOrganization(organization.getId(), MEMBERSHIP_COVERAGE_TYPES)
            .stream()
            .collect(Collectors.toMap(
                MemberPaymentRepository.MembershipAmountProjection::getMembershipId,
                MemberPaymentRepository.MembershipAmountProjection::getAmount
            ));

        BigDecimal currentRevenue = memberPaymentRepository.sumAmountByOrganizationAndStatusAndTypesBetween(
            organization.getId(),
            PaymentStatus.PAID,
            COLLECTED_REVENUE_TYPES,
            monthStart,
            monthEnd
        );

        BigDecimal pendingRevenue = BigDecimal.ZERO;
        BigDecimal overdueRevenue = BigDecimal.ZERO;
        long prepaidMembers = 0L;
        long activeMembersPendingCollection = 0L;
        BigDecimal monthlyMembershipFee = organization.getMonthlyMembershipFee() == null
            ? BigDecimal.ZERO
            : organization.getMonthlyMembershipFee();

        for (MemberMembership membership : memberships) {
            if (membership.getMember().getStatus() != MemberStatus.ACTIVE) {
                continue;
            }
            PaymentCoverageSummary coverage = membershipLifecycleService.buildCoverageSummary(
                membership,
                paidAmountByMembershipId.getOrDefault(membership.getId(), BigDecimal.ZERO),
                BigDecimal.ZERO,
                reminderDays
            );
            boolean isCurrentObligation = membership.getStatus() == MembershipStatus.ACTIVE
                && !membership.getStartDate().isAfter(monthEnd)
                && !membership.getEndDate().isBefore(monthStart);
            if (isCurrentObligation && membership.getStatus() == MembershipStatus.ACTIVE) {
                pendingRevenue = pendingRevenue.add(
                    coverage.pendingAmount().compareTo(BigDecimal.ZERO) > 0 ? monthlyMembershipFee : BigDecimal.ZERO
                );
                if (coverage.pendingAmount().compareTo(BigDecimal.ZERO) > 0) {
                    activeMembersPendingCollection++;
                }
            }
            if (membership.getStatus() == MembershipStatus.OVERDUE) {
                overdueRevenue = overdueRevenue.add(monthlyMembershipFee);
                activeMembersPendingCollection++;
            }
            if (membership.getDurationMonths() > 1 && coverage.pendingAmount().compareTo(BigDecimal.ZERO) == 0
                && membership.getStatus() == MembershipStatus.ACTIVE) {
                prepaidMembers++;
            }
        }

        BigDecimal expectedRevenue = currentRevenue.add(overdueRevenue);
        long activeMembers = memberMembershipRepository.countBillableActiveMembers(organization.getId(), LocalDate.now(APP_ZONE));
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
        YearMonth currentMonth = YearMonth.now(APP_ZONE);
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
        admissionRevenue = memberPaymentRepository.sumAmountByOrganizationAndStatusAndTypesBetween(
            organization.getId(),
            PaymentStatus.PAID,
            List.of(PaymentType.ADMISSION),
            monthStart,
            monthEnd
        );
        recurringRevenue = memberPaymentRepository.sumAmountByOrganizationAndStatusAndTypesBetween(
            organization.getId(),
            PaymentStatus.PAID,
            MEMBERSHIP_COVERAGE_TYPES,
            monthStart,
            monthEnd
        );
        long admittedMembers = memberPaymentRepository.countDistinctMembersByOrganizationAndStatusAndTypeBetween(
            organization.getId(),
            PaymentStatus.PAID,
            PaymentType.ADMISSION,
            monthStart,
            monthEnd
        );

        return new RevenueSegmentationResponse(
            admittedMembers,
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
