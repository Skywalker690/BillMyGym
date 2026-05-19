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
        BigDecimal monthlySaasRevenue,
        long paymentFailures,
        long whatsappUsage,
        long emailUsage
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
}
