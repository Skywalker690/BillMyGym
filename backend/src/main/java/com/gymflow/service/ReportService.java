package com.gymflow.service;

import com.gymflow.api.dto.MemberDtos.MemberDirectoryItem;
import com.gymflow.api.dto.ReportDtos.DashboardReportResponse;
import com.gymflow.api.dto.ReportDtos.ReminderReportResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final OrganizationSettingService organizationSettingService;
    private final NotificationQueryService notificationQueryService;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardReports", key = "T(com.gymflow.security.SecurityUtils).currentUser().organizationId()")
    public DashboardReportResponse dashboardReport() {
        Long organizationId = requireOrganizationId();
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
        LocalDate today = LocalDate.now();
        List<MemberMembership> expiringSoon = memberMembershipRepository.findExpiringSoonMemberships(
            organizationId,
            today,
            today.plusDays(reminderDays),
            PageRequest.of(0, 5)
        );
        List<MemberMembership> overdue = memberMembershipRepository.findOverdueOrExpiredMemberships(
            organizationId,
            PageRequest.of(0, 5)
        );
        List<MemberDirectoryItem> expiringItems = toDirectoryItems(expiringSoon, reminderDays);
        List<MemberDirectoryItem> overdueItems = toDirectoryItems(overdue, reminderDays);

        return new DashboardReportResponse(
            revenueService.revenueSummary(),
            memberService.memberDirectorySummary(organizationId, reminderDays),
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

    private Long requireOrganizationId() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }
}
