package com.gymflow.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class AnalyticsDtos {

    public record PlatformAnalyticsResponse(
        long totalOrganizations,
        long activeOrganizations,
        long trialOrganizations,
        long pilotOrganizations,
        long pausedOrganizations,
        long deactivatedOrganizations,
        long expiredOrganizations,
        long activeSubscriptionRecords,
        BigDecimal monthlySaasRevenue,
        BigDecimal previousMonthlySaasRevenue,
        int revenueGrowthPercent,
        BigDecimal lifetimeSaasRevenue,
        BigDecimal projectedMonthlyRevenue,
        long paymentFailures,
        long pendingPayments,
        long whatsappUsage,
        long emailUsage,
        long totalCommunicationUsage,
        long renewalsDueInSevenDays,
        long pilotsEndingInThirtyDays,
        long currentMonthNewOrganizations,
        long previousMonthNewOrganizations,
        int organizationGrowthPercent,
        List<PlatformBreakdownResponse> organizationsByStatus,
        List<PlatformBreakdownResponse> organizationsBySubscriptionPlan,
        List<PlatformBreakdownResponse> paymentsByStatus,
        List<PlatformBreakdownResponse> lifecycleAlerts
    ) {
    }

    public record PlatformBreakdownResponse(
        String key,
        String label,
        long count,
        BigDecimal amount,
        int percentage
    ) {
    }

    public record OrganizationAnalyticsResponse(
        long activeMembers,
        long remindersSent
    ) {
    }

    public record RevenueSummaryResponse(
        BigDecimal currentRevenue,
        BigDecimal pendingRevenue,
        BigDecimal expectedRevenue,
        BigDecimal overdueRevenue,
        long activeMembers,
        long prepaidMembers
    ) {
    }

    public record MonthlyRevenuePoint(
        int year,
        int month,
        BigDecimal currentRevenue,
        BigDecimal pendingRevenue,
        BigDecimal expectedRevenue,
        BigDecimal overdueRevenue
    ) {
    }

    public record MonthlyRevenueResponse(
        List<MonthlyRevenuePoint> months
    ) {
    }

    public record PlatformYearlyReportMonth(
        int month,
        String label,
        long organizationsJoined,
        BigDecimal revenue
    ) {
    }

    public record PlatformYearlyReportResponse(
        int year,
        List<Integer> availableYears,
        long totalOrganizationsJoined,
        BigDecimal totalRevenue,
        List<PlatformYearlyReportMonth> months
    ) {
    }
}
