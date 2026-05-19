package com.gymflow.service;

import com.gymflow.api.dto.AnalyticsDtos.MonthlyRevenuePoint;
import com.gymflow.api.dto.AnalyticsDtos.MonthlyRevenueResponse;
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
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RevenueService {

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

        BigDecimal currentRevenue = payments.stream()
            .filter(payment -> payment.getPaymentDate() != null)
            .filter(payment -> !payment.getPaymentDate().isBefore(monthStart) && !payment.getPaymentDate().isAfter(monthEnd))
            .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PAID || payment.getPaymentStatus() == PaymentStatus.PARTIAL)
            .map(MemberPayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedRevenue = BigDecimal.ZERO;
        BigDecimal pendingRevenue = BigDecimal.ZERO;
        BigDecimal overdueRevenue = BigDecimal.ZERO;
        long prepaidMembers = 0L;

        for (MemberMembership membership : memberships) {
            if (membership.getMember().getStatus() != MemberStatus.ACTIVE) {
                continue;
            }
            PaymentCoverageSummary coverage = membershipLifecycleService.buildCoverageSummary(
                membership,
                payments.stream().filter(payment -> payment.getMembership().getId().equals(membership.getId())).toList(),
                reminderDays
            );
            boolean isCurrentObligation = membership.getStatus() == MembershipStatus.ACTIVE
                && !membership.getStartDate().isAfter(monthEnd)
                && !membership.getEndDate().isBefore(monthStart);
            if (isCurrentObligation && membership.getStatus() != MembershipStatus.COMPLETED) {
                expectedRevenue = expectedRevenue.add(membership.getPlanAmount());
            }
            if (isCurrentObligation && membership.getStatus() == MembershipStatus.ACTIVE) {
                pendingRevenue = pendingRevenue.add(coverage.pendingAmount());
            }
            if (membership.getStatus() == MembershipStatus.EXPIRED || membership.getStatus() == MembershipStatus.OVERDUE) {
                overdueRevenue = overdueRevenue.add(coverage.pendingAmount());
            }
            if (membership.getDurationMonths() > 1 && coverage.pendingAmount().compareTo(BigDecimal.ZERO) == 0
                && membership.getStatus() == MembershipStatus.ACTIVE) {
                prepaidMembers++;
            }
        }

        return new RevenueSummaryResponse(
            currentRevenue,
            pendingRevenue,
            expectedRevenue,
            overdueRevenue,
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(organization.getId(), MemberStatus.ACTIVE),
            prepaidMembers
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

    private Organization currentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
    }
}
