package com.gymflow.service;

import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.Organization;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import com.gymflow.repository.AuditLogRepository;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.UsageMetricRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
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
    private final UsageMetricRepository usageMetricRepository;
    private final AuditLogRepository auditLogRepository;

    public MonthlyOrganizationReport buildReport(Organization organization, YearMonth reportMonth) {
        LocalDate monthStart = reportMonth.atDay(1);
        LocalDate monthEnd = reportMonth.atEndOfMonth();
        OffsetDateTime start = monthStart.atStartOfDay(REPORT_ZONE).toOffsetDateTime();
        OffsetDateTime end = monthEnd.plusDays(1).atStartOfDay(REPORT_ZONE).toOffsetDateTime();

        long membersJoined = memberMembershipRepository.countFirstMembershipsStartedBetween(
            organization.getId(),
            monthStart,
            monthEnd
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

        Map<Long, BigDecimal> paidByMembership = memberPaymentRepository
            .sumSuccessfulAmountsByMembershipForOrganization(
                organization.getId(),
                List.of(PaymentType.MEMBERSHIP, PaymentType.NEW_MEMBERSHIP, PaymentType.RENEWAL)
            )
            .stream()
            .collect(Collectors.toMap(
                MemberPaymentRepository.MembershipAmountProjection::getMembershipId,
                MemberPaymentRepository.MembershipAmountProjection::getAmount
            ));
        BigDecimal pendingRevenue = latestMemberships.values().stream()
            .filter(membership -> membership.getEndDate().isBefore(snapshotDate))
            .map(membership -> {
                BigDecimal paid = paidByMembership.getOrDefault(membership.getId(), BigDecimal.ZERO);
                return membership.getPlanAmount().subtract(paid).max(BigDecimal.ZERO);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long deactivatedMembers = auditLogRepository.countByOrganizationIdAndEventBetween(
            organization.getId(),
            "MEMBER_DEACTIVATED",
            start.toInstant(),
            end.toInstant()
        );

        UsageMetric usageStats = usageMetricRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            organization.getId(),
            reportMonth.getYear(),
            reportMonth.getMonthValue()
        ).orElseGet(() -> UsageMetric.empty(organization, reportMonth));
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
            usageStats.getMonthlyWhatsappCount(),
            usageStats.getMonthlyEmailCount()
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
