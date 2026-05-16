package com.gymflow.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class AnalyticsDtos {

    public record PlatformAnalyticsResponse(
        long totalOrganizations,
        long activeSubscriptions
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
