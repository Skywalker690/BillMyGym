package com.gymflow.service;

import com.gymflow.api.dto.AnalyticsDtos.OrganizationAnalyticsResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformBreakdownResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformAnalyticsResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformSubscriptionMonthResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformYearlyReportMonth;
import com.gymflow.api.dto.AnalyticsDtos.PlatformYearlyReportResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionPaymentRepository;
import com.gymflow.repository.UsageMetricRepository;
import com.gymflow.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final SubscriptionPaymentStatus PAID_STATUS = SubscriptionPaymentStatus.PAID;

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UsageMetricRepository usageMetricRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SubscriptionPricingService subscriptionPricingService;

    public PlatformAnalyticsResponse platformAnalytics() {
        ensureSuperAdmin();
        YearMonth currentMonth = YearMonth.now(APP_ZONE);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate previousMonthStart = previousMonth.atDay(1);
        LocalDate previousMonthEnd = previousMonth.atEndOfMonth();
        LocalDate today = LocalDate.now(APP_ZONE);
        long totalOrganizations = organizationRepository.countByDeletedFalse();
        long activeOrganizations = organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.ACTIVE);
        long trialOrganizations = organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.TRIAL);
        long demoOrganizations = organizationRepository.countByDeletedFalseAndOrganizationType(OrganizationType.DEMO);
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
        BigDecimal monthlySaasRevenue = safe(subscriptionPaymentRepository.sumPaidBetween(monthStart, monthEnd));
        BigDecimal previousMonthlySaasRevenue = safe(subscriptionPaymentRepository.sumPaidBetween(previousMonthStart, previousMonthEnd));
        BigDecimal lifetimeSaasRevenue = safe(subscriptionPaymentRepository.sumPaidAll());
        List<Organization> activeRegularOrganizations = organizationRepository.findAllByDeletedFalseAndOrganizationTypeAndStatus(
            OrganizationType.REGULAR,
            OrganizationStatus.ACTIVE
        );
        BigDecimal projectedMonthlyRevenue = subscriptionPricingService.projectedRevenue(activeRegularOrganizations);
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
            demoOrganizations,
            pausedOrganizations,
            overdueRegularOrganizations,
            deactivatedOrganizations,
            expiredOrganizations,
            subscriptionPaymentRepository.countByPaymentStatusAndSubscriptionEndGreaterThanEqualAndDeletedFalse(
                PAID_STATUS,
                today
            ),
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
            0,
            currentMonthNewOrganizations,
            previousMonthNewOrganizations,
            growthPercent(previousMonthNewOrganizations, currentMonthNewOrganizations),
            monthlySubscriptionActivity(currentMonth),
            organizationsByStatus(totalOrganizations),
            organizationsByPlan(totalOrganizations, activeRegularOrganizations),
            paymentsByStatus(),
            List.of(
                breakdown("RENEWALS_DUE", "Renewals due in 7 days", renewalsDueInSevenDays, BigDecimal.ZERO, totalOrganizations),
                breakdown("EXPIRED", "Expired or overdue organizations", expiredOrganizations, BigDecimal.ZERO, totalOrganizations),
                breakdown("PAYMENT_ATTENTION", "Failed or pending payments", paymentFailures + pendingPayments, BigDecimal.ZERO, Math.max(paymentFailures + pendingPayments, totalOrganizations)),
                breakdown("TRIAL", "Free trial gyms", trialOrganizations, BigDecimal.ZERO, totalOrganizations)
            )
        );
    }

    public PlatformYearlyReportResponse platformYearlyReport(Integer year) {
        ensureSuperAdmin();
        int currentYear = YearMonth.now(APP_ZONE).getYear();
        List<Integer> availableYears = resolveAvailableYears(currentYear);
        int targetYear = year == null ? currentYear : year;
        if (!availableYears.contains(targetYear)) {
            throw new BadRequestException("No platform report data exists for the selected year");
        }

        List<PlatformYearlyReportMonth> months = IntStream.rangeClosed(1, 12)
            .mapToObj(month -> {
                LocalDate startDate = LocalDate.of(targetYear, month, 1);
                LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                long organizationsJoined = organizationRepository.countByDeletedFalseAndCreatedAtBetween(
                    startOfDay(startDate),
                    endOfDay(endDate)
                );
                return new PlatformYearlyReportMonth(
                    month,
                    startDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    organizationsJoined,
                    safe(subscriptionPaymentRepository.sumPaidBetween(startDate, endDate))
                );
            })
            .toList();

        long totalOrganizationsJoined = months.stream().mapToLong(PlatformYearlyReportMonth::organizationsJoined).sum();
        BigDecimal totalRevenue = months.stream()
            .map(PlatformYearlyReportMonth::revenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PlatformYearlyReportResponse(targetYear, availableYears, totalOrganizationsJoined, totalRevenue, months);
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

    private List<PlatformBreakdownResponse> organizationsByPlan(long totalOrganizations, List<Organization> activeRegularOrganizations) {
        Map<SubscriptionPlan, Long> activeRegularCounts = new EnumMap<>(SubscriptionPlan.class);
        organizationRepository.countBySubscriptionPlanForOrganizationTypeAndStatus(
                OrganizationType.REGULAR,
                OrganizationStatus.ACTIVE
            )
            .forEach(row -> activeRegularCounts.put(row.getSubscriptionPlan(), row.getCount() == null ? 0L : row.getCount()));
        Map<SubscriptionPlan, BigDecimal> activeRegularAmounts = subscriptionPricingService.projectedRevenueByPlan(activeRegularOrganizations);
        List<PlatformBreakdownResponse> items = new ArrayList<>(Arrays.stream(SubscriptionPlan.values())
            .map(plan -> breakdown(
                plan.name(),
                label(plan.name()),
                activeRegularCounts.getOrDefault(plan, 0L),
                activeRegularAmounts.getOrDefault(plan, BigDecimal.ZERO),
                totalOrganizations
            ))
            .toList());
        long demoOrganizations = organizationRepository.countByDeletedFalseAndOrganizationType(OrganizationType.DEMO);
        if (demoOrganizations > 0) {
            items.add(breakdown(
                "DEMO",
                label("DEMO"),
                demoOrganizations,
                BigDecimal.ZERO,
                totalOrganizations
            ));
        }
        long renewalRequiredOrganizations = organizationRepository.countByDeletedFalseAndStatusIn(
            List.of(OrganizationStatus.PAUSED, OrganizationStatus.DEACTIVATED)
        );
        if (renewalRequiredOrganizations > 0) {
            items.add(breakdown(
                "RENEWAL_REQUIRED",
                "Renewal Required",
                renewalRequiredOrganizations,
                BigDecimal.ZERO,
                totalOrganizations
            ));
        }
        items.add(breakdown(
            "TRIAL",
            "Free Trial Gyms",
            organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.TRIAL),
            BigDecimal.ZERO,
            totalOrganizations
        ));
        return items;
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

    private List<PlatformSubscriptionMonthResponse> monthlySubscriptionActivity(YearMonth currentMonth) {
        YearMonth startMonth = currentMonth.minusMonths(5);
        return IntStream.rangeClosed(0, 5)
            .mapToObj(offset -> {
                YearMonth month = startMonth.plusMonths(offset);
                OffsetDateTime start = startOfDay(month.atDay(1));
                OffsetDateTime end = endOfDay(month.atEndOfMonth());
                LocalDate startDate = month.atDay(1);
                LocalDate endDate = month.atEndOfMonth();
                return new PlatformSubscriptionMonthResponse(
                    month.toString(),
                    month.atDay(1).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    subscriptionPaymentRepository.countByPaymentStatusAndDeletedFalseAndPaymentDateBetween(
                        SubscriptionPaymentStatus.PAID,
                        startDate,
                        endDate
                    ),
                    subscriptionPaymentRepository.countByPaymentStatusAndDeletedFalseAndCreatedAtBetween(
                        SubscriptionPaymentStatus.PENDING,
                        start,
                        end
                    ),
                    safe(subscriptionPaymentRepository.sumByPaymentStatusAndPaymentDateBetween(
                        SubscriptionPaymentStatus.PAID,
                        startDate,
                        endDate
                    )),
                    safe(subscriptionPaymentRepository.sumByPaymentStatusAndCreatedAtBetween(
                        SubscriptionPaymentStatus.PENDING,
                        start,
                        end
                    ))
                );
            })
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

    private void ensureSuperAdmin() {
        if (SecurityUtils.currentUser().role() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only SUPER_ADMIN can access platform analytics");
        }
    }

    private List<Integer> resolveAvailableYears(int currentYear) {
        OffsetDateTime earliestOrganizationCreatedAt = organizationRepository.findEarliestCreatedAt();
        LocalDate earliestPaidPaymentDate = subscriptionPaymentRepository.findEarliestPaidPaymentDate();
        Integer earliestOrganizationYear = earliestOrganizationCreatedAt == null
            ? null
            : earliestOrganizationCreatedAt.getYear();
        Integer earliestRevenueYear = earliestPaidPaymentDate == null
            ? null
            : earliestPaidPaymentDate.getYear();
        Integer earliestYear = Stream.of(earliestOrganizationYear, earliestRevenueYear)
            .filter(java.util.Objects::nonNull)
            .min(Integer::compareTo)
            .orElse(currentYear);
        return IntStream.rangeClosed(earliestYear, currentYear)
            .boxed()
            .sorted((left, right) -> Integer.compare(right, left))
            .toList();
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(APP_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(APP_ZONE).toOffsetDateTime().minusNanos(1);
    }

    private String label(String value) {
        return switch (value) {
            case "BILLMYGYM" -> "BillMyGym Plan";
            case "DEMO" -> "Demo Workspace";
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
            memberMembershipRepository.countBillableActiveMembers(organizationId, LocalDate.now(APP_ZONE)),
            notificationLogRepository.countByOrganizationIdAndTypeAndDeletedFalse(
                organizationId,
                NotificationType.ORGANIZATION_RENEWAL_DIGEST
            )
        );
    }
}
