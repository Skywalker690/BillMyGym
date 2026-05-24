package com.gymflow.service;

import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationUsageStats;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.repository.AuditLogRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    private final AuditLogRepository auditLogRepository;

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
        long expiredMembers = memberMembershipRepository.countExpiredBetween(
            organization.getId(),
            monthStart,
            monthEnd
        );

        List<MemberPayment> payments = memberPaymentRepository.findByOrganizationIdAndDeletedFalseAndPaymentDateBetweenOrderByPaymentDateAscCreatedAtAsc(
            organization.getId(),
            monthStart,
            monthEnd
        );
        BigDecimal collectedRevenue = payments.stream()
            .filter(payment -> payment.getPaymentDate() != null)
            .filter(payment -> !payment.getPaymentDate().isBefore(monthStart) && !payment.getPaymentDate().isAfter(monthEnd))
            .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PAID)
            .map(MemberPayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate snapshotDate = monthEnd;
        var latestMemberships = latestMembershipsAsOf(
            memberMembershipRepository.findReportSnapshotMemberships(organization.getId(), snapshotDate),
            snapshotDate
        );
        long activeMembers = latestMemberships.values().stream()
            .filter(membership -> !membership.getEndDate().isBefore(snapshotDate))
            .count();
        long overdueMembers = latestMemberships.values().stream()
            .filter(membership -> membership.getEndDate().isBefore(snapshotDate))
            .count();
        BigDecimal monthlyFee = organization.getMonthlyMembershipFee() == null
            ? BigDecimal.ZERO
            : organization.getMonthlyMembershipFee();
        BigDecimal pendingRevenue = monthlyFee.multiply(BigDecimal.valueOf(overdueMembers));
        long deactivatedMembers = auditLogRepository.countByOrganizationIdAndEventBetween(
            organization.getId(),
            "MEMBER_DEACTIVATED",
            start.toInstant(),
            end.toInstant()
        );

        OrganizationUsageStats usageStats = organizationUsageStatsRepository.findSnapshotOrEmpty(organization, reportMonth);
        return new MonthlyOrganizationReport(
            reportMonth,
            reportMonth.format(LABEL_FORMATTER),
            membersJoined,
            activeMembers,
            expiredMembers,
            overdueMembers,
            deactivatedMembers,
            collectedRevenue,
            pendingRevenue,
            usageStats.getWhatsappSentCount(),
            usageStats.getEmailSentCount()
        );
    }

    private HashMap<Long, MemberMembership> latestMembershipsAsOf(List<MemberMembership> memberships, LocalDate snapshotDate) {
        HashMap<Long, MemberMembership> latestByMember = new HashMap<>();
        for (MemberMembership membership : memberships) {
            if (membership.getMember().isDeleted() || !isMemberActiveAtSnapshot(membership.getMember(), snapshotDate)) {
                continue;
            }
            if (membership.getStartDate().isAfter(snapshotDate)) {
                continue;
            }
            latestByMember.merge(
                membership.getMember().getId(),
                membership,
                (left, right) -> latestMembershipComparator().compare(left, right) >= 0 ? left : right
            );
        }
        return latestByMember;
    }

    private Comparator<MemberMembership> latestMembershipComparator() {
        return Comparator.comparing(MemberMembership::getEndDate)
            .thenComparing(MemberMembership::getStartDate)
            .thenComparing(MemberMembership::getCreatedAt);
    }

    private boolean isMemberActiveAtSnapshot(com.gymflow.domain.Member member, LocalDate snapshotDate) {
        if (member.getCreatedAt() != null && member.getCreatedAt().toLocalDate().isAfter(snapshotDate)) {
            return false;
        }
        return member.getDeactivatedAt() == null || member.getDeactivatedAt().toLocalDate().isAfter(snapshotDate);
    }

    public record MonthlyOrganizationReport(
        YearMonth month,
        String label,
        long membersJoined,
        long activeMembers,
        long expiredMembers,
        long overdueMembers,
        long deactivatedMembers,
        BigDecimal collectedRevenue,
        BigDecimal pendingRevenue,
        long whatsappSentCount,
        long emailSentCount
    ) {
    }
}
