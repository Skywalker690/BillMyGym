package com.gymflow.service;

import com.gymflow.api.dto.MemberDtos.MemberDirectoryItem;
import com.gymflow.api.dto.MemberDtos.MemberDirectorySummary;
import com.gymflow.api.dto.AnalyticsDtos.RevenueSegmentationResponse;
import com.gymflow.api.dto.AnalyticsDtos.RevenueSummaryResponse;
import com.gymflow.api.dto.ReportDtos.DashboardReportResponse;
import com.gymflow.api.dto.ReportDtos.MonthlyMemberJoinResponse;
import com.gymflow.api.dto.ReportDtos.ReminderReportResponse;
import com.gymflow.api.dto.SubscriptionDtos.PlanAccessResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final RevenueService revenueService;
    private final MemberService memberService;
    private final MemberMembershipService memberMembershipService;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberRepository memberRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final OrganizationSettingService organizationSettingService;
    private final NotificationQueryService notificationQueryService;
    private final com.gymflow.repository.OrganizationRepository organizationRepository;
    private final PlanAccessService planAccessService;

    @Transactional
    @Cacheable(value = "dashboardReports", key = "T(com.gymflow.security.SecurityUtils).currentUser().organizationId()")
    public DashboardReportResponse dashboardReport() {
        Long organizationId = requireOrganizationId();
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
        LocalDate today = LocalDate.now();
        memberMembershipService.refreshMembershipStatuses(organizationId);
        List<MemberMembership> expiringSoon = memberMembershipRepository.findExpiringSoonMemberships(
            organizationId,
            today,
            today.plusDays(reminderDays),
            PageRequest.of(0, 5)
        );
        List<MemberMembership> overdue = memberMembershipRepository.findOverdueOrExpiredMemberships(
            organizationId,
            today,
            PageRequest.of(0, 5)
        );
        List<MemberDirectoryItem> expiringItems = toDirectoryItems(expiringSoon, reminderDays);
        List<MemberDirectoryItem> overdueItems = toDirectoryItems(overdue, reminderDays);
        var organization = organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new BadRequestException("Organization not found"));
        RevenueSummaryResponse revenueSummary = planAccessService.canViewAdvancedAnalytics(organization)
            ? revenueService.revenueSummary()
            : null;
        RevenueSegmentationResponse revenueSegmentation = planAccessService.canViewAdvancedAnalytics(organization)
            ? revenueService.revenueSegmentationSummary()
            : null;
        MemberDirectorySummary memberSummary = memberService.memberDirectorySummary(organizationId, reminderDays);

        return new DashboardReportResponse(
            revenueSummary,
            revenueSegmentation,
            memberSummary,
            monthlyMemberJoins(organizationId),
            toPlanAccessResponse(planAccessService.summary(organization)),
            expiringItems,
            overdueItems,
            notificationQueryService.listRecentReminderLogs(organizationId, 5)
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardReports", key = "'reminders:' + T(com.gymflow.security.SecurityUtils).currentUser().organizationId()")
    public ReminderReportResponse reminderReport() {
        Long organizationId = requireOrganizationId();
        return new ReminderReportResponse(
            organizationSettingService.getCurrentSettings(),
            notificationQueryService.listRecentReminderLogs(organizationId, 25)
        );
    }

    private List<MemberDirectoryItem> toDirectoryItems(List<MemberMembership> memberships, int reminderDays) {
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<Long> membershipIds = memberships.stream().map(MemberMembership::getId).toList();
        Map<Long, List<MemberPayment>> paymentsByMembershipId = memberPaymentRepository
            .findByMembershipIdInAndDeletedFalse(membershipIds)
            .stream()
            .collect(Collectors.groupingBy(payment -> payment.getMembership().getId()));
        Map<Long, BigDecimal> walletBalanceByMemberId = memberService.toDirectoryItems(
            memberships.stream().map(MemberMembership::getMember).toList(),
            reminderDays
        ).stream().collect(Collectors.toMap(
            MemberDirectoryItem::id,
            item -> item.currentMembership() == null ? BigDecimal.ZERO : item.currentMembership().walletBalance()
        ));
        Map<Long, BigDecimal> appliedCreditByMembershipId = memberships.stream()
            .collect(Collectors.toMap(MemberMembership::getId, membership -> BigDecimal.ZERO));
        return memberships.stream()
            .map(membership -> memberService.toDirectoryItem(
                membership.getMember(),
                membership,
                walletBalanceByMemberId.getOrDefault(membership.getMember().getId(), BigDecimal.ZERO),
                appliedCreditByMembershipId,
                paymentsByMembershipId,
                reminderDays
            ))
            .toList();
    }

    private PlanAccessResponse toPlanAccessResponse(PlanAccessService.PlanAccessSummary summary) {
        return new PlanAccessResponse(
            summary.canCreateMembers(),
            summary.canViewAdvancedAnalytics(),
            summary.canReceiveMonthlyReports(),
            summary.canExportReports(),
            summary.canUsePremiumFeatures()
        );
    }

    private List<MonthlyMemberJoinResponse> monthlyMemberJoins(Long organizationId) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(5);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM");

        Map<YearMonth, Long> joinedMembersByMonth = memberRepository.findByOrganizationIdAndDeletedFalse(organizationId).stream()
            .map(Member::getCreatedAt)
            .filter(createdAt -> createdAt != null)
            .map(YearMonth::from)
            .filter(month -> !month.isBefore(startMonth) && !month.isAfter(currentMonth))
            .collect(Collectors.groupingBy(month -> month, Collectors.counting()));

        return IntStream.rangeClosed(0, 5)
            .mapToObj(offset -> {
                YearMonth month = startMonth.plusMonths(offset);
                return new MonthlyMemberJoinResponse(
                    month.toString(),
                    month.format(labelFormatter),
                    joinedMembersByMonth.getOrDefault(month, 0L)
                );
            })
            .toList();
    }

    private Long requireOrganizationId() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }
}
