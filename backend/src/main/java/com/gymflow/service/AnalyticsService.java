package com.gymflow.service;

import com.gymflow.api.dto.AnalyticsDtos.OrganizationAnalyticsResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformBreakdownResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformAnalyticsResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionPaymentRepository;
import com.gymflow.repository.OrganizationSubscriptionRepository;
import com.gymflow.repository.UsageMetricRepository;
import com.gymflow.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UsageMetricRepository usageMetricRepository;
    private final MemberRepository memberRepository;
    private final NotificationLogRepository notificationLogRepository;

    public PlatformAnalyticsResponse platformAnalytics() {
        if (SecurityUtils.currentUser().role() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only SUPER_ADMIN can access platform analytics");
        }
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate previousMonthStart = previousMonth.atDay(1);
        LocalDate previousMonthEnd = previousMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();
        List<Organization> organizations = organizationRepository.findAllByDeletedFalse();
        long totalOrganizations = organizations.size();
        long activeOrganizations = organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.ACTIVE);
        long trialOrganizations = organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.TRIAL);
        long pilotOrganizations = organizationRepository.countByDeletedFalseAndOrganizationType(OrganizationType.PILOT);
        long pausedOrganizations = organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.PAUSED);
        long deactivatedOrganizations = organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.DEACTIVATED);
        long overdueRegularOrganizations = organizationRepository.countByDeletedFalseAndOrganizationTypeAndStatusInAndSubscriptionDueDateBefore(
            OrganizationType.REGULAR,
            List.of(OrganizationStatus.ACTIVE, OrganizationStatus.TRIAL),
            today
        );
        long expiredOrganizations = pausedOrganizations + overdueRegularOrganizations;
        long renewalsDueInSevenDays = organizationRepository.countByDeletedFalseAndOrganizationTypeAndStatusInAndSubscriptionDueDateBetween(
            OrganizationType.REGULAR,
            List.of(OrganizationStatus.ACTIVE, OrganizationStatus.TRIAL),
            today,
            today.plusDays(7)
        );
        long pilotsEndingInThirtyDays = organizationRepository.countByDeletedFalseAndOrganizationTypeAndPilotEndDateBetween(
            OrganizationType.PILOT,
            today,
            today.plusDays(30)
        );
        BigDecimal monthlySaasRevenue = safe(subscriptionPaymentRepository.sumPaidBetween(monthStart, monthEnd));
        BigDecimal previousMonthlySaasRevenue = safe(subscriptionPaymentRepository.sumPaidBetween(previousMonthStart, previousMonthEnd));
        BigDecimal lifetimeSaasRevenue = safe(subscriptionPaymentRepository.sumPaidAll());
        BigDecimal projectedMonthlyRevenue = organizations.stream()
            .filter(organization -> !organization.isDeleted())
            .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
            .filter(organization -> organization.getOrganizationType() == OrganizationType.REGULAR)
            .map(organization -> organization.getSubscriptionPlan() == null
                ? BigDecimal.ZERO
                : organization.getSubscriptionPlan().monthlyAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        var usage = usageMetricRepository.findByYearAndMonthAndDeletedFalse(
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        );
        long whatsappUsage = usage.stream().mapToLong(metric -> metric.getMonthlyWhatsappCount()).sum();
        long emailUsage = usage.stream().mapToLong(metric -> metric.getMonthlyEmailCount()).sum();
        long paymentFailures = subscriptionPaymentRepository.countByPaymentStatusAndDeletedFalse(SubscriptionPaymentStatus.FAILED);
        long pendingPayments = subscriptionPaymentRepository.countByPaymentStatusAndDeletedFalse(SubscriptionPaymentStatus.PENDING);
        long currentMonthNewOrganizations = organizationRepository.countByDeletedFalseAndCreatedAtBetween(
            startOfDay(monthStart),
            endOfDay(monthEnd)
        );
        long previousMonthNewOrganizations = organizationRepository.countByDeletedFalseAndCreatedAtBetween(
            startOfDay(previousMonthStart),
            endOfDay(previousMonthEnd)
        );
        return new PlatformAnalyticsResponse(
            totalOrganizations,
            activeOrganizations,
            trialOrganizations,
            pilotOrganizations,
            pausedOrganizations,
            deactivatedOrganizations,
            expiredOrganizations,
            subscriptionRepository.countByDeletedFalseAndEndDateGreaterThanEqual(today),
            monthlySaasRevenue,
            previousMonthlySaasRevenue,
            growthPercent(previousMonthlySaasRevenue, monthlySaasRevenue),
            lifetimeSaasRevenue,
            projectedMonthlyRevenue,
            paymentFailures,
            pendingPayments,
            whatsappUsage,
            emailUsage,
            whatsappUsage + emailUsage,
            renewalsDueInSevenDays,
            pilotsEndingInThirtyDays,
            currentMonthNewOrganizations,
            previousMonthNewOrganizations,
            growthPercent(previousMonthNewOrganizations, currentMonthNewOrganizations),
            organizationsByStatus(totalOrganizations),
            organizationsByPlan(totalOrganizations),
            paymentsByStatus(),
            List.of(
                breakdown("RENEWALS_DUE", "Renewals due in 7 days", renewalsDueInSevenDays, BigDecimal.ZERO, totalOrganizations),
                breakdown("EXPIRED", "Expired or overdue organizations", expiredOrganizations, BigDecimal.ZERO, totalOrganizations),
                breakdown("PAYMENT_ATTENTION", "Failed or pending payments", paymentFailures + pendingPayments, BigDecimal.ZERO, Math.max(paymentFailures + pendingPayments, totalOrganizations)),
                breakdown("PILOT_ENDING", "Founding partner access ending in 30 days", pilotsEndingInThirtyDays, BigDecimal.ZERO, totalOrganizations)
            )
        );
    }

    private List<PlatformBreakdownResponse> organizationsByStatus(long totalOrganizations) {
        return Arrays.stream(OrganizationStatus.values())
            .map(status -> breakdown(
                status.name(),
                label(status.name()),
                organizationRepository.countByDeletedFalseAndStatus(status),
                BigDecimal.ZERO,
                totalOrganizations
            ))
            .toList();
    }

    private List<PlatformBreakdownResponse> organizationsByPlan(long totalOrganizations) {
        return Arrays.stream(SubscriptionPlan.values())
            .map(plan -> {
                long count = organizationRepository.countByDeletedFalseAndSubscriptionPlan(plan);
                return breakdown(
                    plan.name(),
                    label(plan.name()),
                    count,
                    plan.monthlyAmount().multiply(BigDecimal.valueOf(count)),
                    totalOrganizations
                );
            })
            .toList();
    }

    private List<PlatformBreakdownResponse> paymentsByStatus() {
        long totalPayments = Arrays.stream(SubscriptionPaymentStatus.values())
            .mapToLong(subscriptionPaymentRepository::countByPaymentStatusAndDeletedFalse)
            .sum();
        return Arrays.stream(SubscriptionPaymentStatus.values())
            .map(status -> breakdown(
                status.name(),
                label(status.name()),
                subscriptionPaymentRepository.countByPaymentStatusAndDeletedFalse(status),
                BigDecimal.ZERO,
                totalPayments
            ))
            .toList();
    }

    private PlatformBreakdownResponse breakdown(String key, String label, long count, BigDecimal amount, long total) {
        int percentage = total <= 0 ? 0 : (int) Math.round((count * 100.0) / total);
        return new PlatformBreakdownResponse(key, label, count, safe(amount), percentage);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int growthPercent(BigDecimal previous, BigDecimal current) {
        BigDecimal safePrevious = safe(previous);
        BigDecimal safeCurrent = safe(current);
        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        return safeCurrent.subtract(safePrevious)
            .multiply(BigDecimal.valueOf(100))
            .divide(safePrevious, 0, java.math.RoundingMode.HALF_UP)
            .intValue();
    }

    private int growthPercent(long previous, long current) {
        if (previous == 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) Math.round(((current - previous) * 100.0) / previous);
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
    }

    private OffsetDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).minusNanos(1);
    }

    private String label(String value) {
        return switch (value) {
            case "PILOT" -> "Founding Partner Access";
            case "TRIAL" -> "Free Trial Access";
            case "ACTIVE" -> "Active Subscription";
            case "PAUSED" -> "Subscription Renewal Required";
            case "DEACTIVATED" -> "Account Inactive";
            default -> Arrays.stream(value.toLowerCase().split("_"))
                .map(part -> part.isBlank() ? part : part.substring(0, 1).toUpperCase() + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(value);
        };
    }

    public OrganizationAnalyticsResponse organizationAnalytics() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return new OrganizationAnalyticsResponse(
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(organizationId, MemberStatus.ACTIVE),
            notificationLogRepository.countByOrganizationIdAndTypeAndDeletedFalse(
                organizationId,
                NotificationType.ORGANIZATION_RENEWAL_DIGEST
            )
        );
    }
}
