package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.PaymentCoverageSummary;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationUsageStats;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationUsageStatsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthlyOrganizationReportService {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM");

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final OrganizationUsageStatsRepository organizationUsageStatsRepository;
    private final MembershipLifecycleService membershipLifecycleService;

    public MonthlyOrganizationReport buildReport(Organization organization, YearMonth reportMonth) {
        LocalDate monthStart = reportMonth.atDay(1);
        LocalDate monthEnd = reportMonth.atEndOfMonth();
        OffsetDateTime start = monthStart.atStartOfDay(REPORT_ZONE).toOffsetDateTime();
        OffsetDateTime end = monthEnd.plusDays(1).atStartOfDay(REPORT_ZONE).toOffsetDateTime();

        long membersJoined = memberRepository.countByOrganizationIdAndDeletedFalseAndCreatedAtBetween(
            organization.getId(),
            start,
            end
        );
        long activeMembers = memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(
            organization.getId(),
            MemberStatus.ACTIVE
        );
        long expiredMembers = memberMembershipRepository.countExpiredBetween(
            organization.getId(),
            monthStart,
            monthEnd
        );

        List<MemberPayment> payments = memberPaymentRepository.findByOrganizationIdAndDeletedFalseOrderByPaymentDateDescCreatedAtDesc(
            organization.getId()
        );
        Map<Long, List<MemberPayment>> paymentsByMembershipId = payments.stream()
            .collect(Collectors.groupingBy(payment -> payment.getMembership().getId()));
        BigDecimal collectedRevenue = payments.stream()
            .filter(payment -> payment.getPaymentDate() != null)
            .filter(payment -> !payment.getPaymentDate().isBefore(monthStart) && !payment.getPaymentDate().isAfter(monthEnd))
            .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PAID)
            .map(MemberPayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingRevenue = BigDecimal.ZERO;
        for (MemberMembership membership : memberMembershipRepository.findByOrganizationIdAndDeletedFalse(organization.getId())) {
            if (membership.getMember().getStatus() != MemberStatus.ACTIVE) {
                continue;
            }
            boolean overlapsMonth = !membership.getStartDate().isAfter(monthEnd)
                && !membership.getEndDate().isBefore(monthStart);
            if (!overlapsMonth) {
                continue;
            }
            PaymentCoverageSummary coverage = membershipLifecycleService.buildCoverageSummary(
                membership,
                paymentsByMembershipId.getOrDefault(membership.getId(), List.of()),
                0
            );
            pendingRevenue = pendingRevenue.add(coverage.pendingAmount());
        }

        OrganizationUsageStats usageStats = organizationUsageStatsRepository.findSnapshotOrEmpty(organization, reportMonth);
        return new MonthlyOrganizationReport(
            reportMonth,
            reportMonth.format(LABEL_FORMATTER),
            membersJoined,
            activeMembers,
            expiredMembers,
            collectedRevenue,
            pendingRevenue,
            usageStats.getWhatsappSentCount(),
            usageStats.getEmailSentCount()
        );
    }

    public record MonthlyOrganizationReport(
        YearMonth month,
        String label,
        long membersJoined,
        long activeMembers,
        long expiredMembers,
        BigDecimal collectedRevenue,
        BigDecimal pendingRevenue,
        long whatsappSentCount,
        long emailSentCount
    ) {
    }
}
