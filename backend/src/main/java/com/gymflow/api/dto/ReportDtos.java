package com.gymflow.api.dto;

import com.gymflow.api.dto.AnalyticsDtos.RevenueSummaryResponse;
import com.gymflow.api.dto.AnalyticsDtos.RevenueSegmentationResponse;
import com.gymflow.api.dto.MemberDtos.MemberDirectoryItem;
import com.gymflow.api.dto.MemberDtos.MemberDirectorySummary;
import com.gymflow.api.dto.NotificationDtos.NotificationResponse;
import com.gymflow.api.dto.ReminderDtos.ManualReminderQuotaResponse;
import com.gymflow.api.dto.SubscriptionDtos.PlanAccessResponse;
import com.gymflow.api.dto.SubscriptionDtos.SubscriptionPricingResponse;
import com.gymflow.api.dto.SettingDtos.SettingsResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportDtos {

    public record MonthlyMemberJoinResponse(
        String month,
        String label,
        long joinedMembers
    ) {
    }

    public record DashboardReportResponse(
        String planName,
        RevenueSummaryResponse revenue,
        RevenueSegmentationResponse revenueSegmentation,
        MemberDirectorySummary memberSummary,
        List<MonthlyMemberJoinResponse> monthlyMemberJoins,
        SubscriptionValiditySummary subscriptionValidity,
        List<MemberDirectoryItem> overdue
    ) {
    }

    public record SubscriptionValiditySummary(
        String label,
        long daysRemaining,
        String tooltipLabel,
        java.time.LocalDate tooltipDate
    ) {
    }

    public record ReminderReportResponse(
        SettingsResponse settings,
        List<NotificationResponse> recentReminderLogs,
        ManualReminderQuotaResponse manualQuota
    ) {
    }

    public record FinancialReportResponse(
        String period,
        String range,
        LocalDate startDate,
        LocalDate endDate,
        String planName,
        FinancialReportSummary summary,
        List<FinancialReportMonthResponse> months
    ) {
    }

    public record FinancialReportSummary(
        BigDecimal totalRevenue,
        BigDecimal admissionRevenue,
        BigDecimal membershipRevenue,
        BigDecimal duePayments,
        long admissions,
        long activeMembers,
        long overdueMembers,
        long deactivatedMembers
    ) {
    }

    public record FinancialReportMonthResponse(
        int year,
        int month,
        String label,
        BigDecimal totalRevenue,
        BigDecimal admissionRevenue,
        BigDecimal membershipRevenue,
        BigDecimal duePayments,
        long admissions,
        long activeMembers,
        long overdueMembers,
        long deactivatedMembers
    ) {
    }
}
