package com.gymflow.api.dto;

import com.gymflow.api.dto.AnalyticsDtos.RevenueSummaryResponse;
import com.gymflow.api.dto.AnalyticsDtos.RevenueSegmentationResponse;
import com.gymflow.api.dto.MemberDtos.MemberDirectoryItem;
import com.gymflow.api.dto.MemberDtos.MemberDirectorySummary;
import com.gymflow.api.dto.NotificationDtos.NotificationResponse;
import com.gymflow.api.dto.SubscriptionDtos.PlanAccessResponse;
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
        RevenueSummaryResponse revenue,
        RevenueSegmentationResponse revenueSegmentation,
        MemberDirectorySummary memberSummary,
        List<MonthlyMemberJoinResponse> monthlyMemberJoins,
        PlanAccessResponse planAccess,
        SubscriptionValiditySummary subscriptionValidity,
        List<MemberDirectoryItem> expiringSoon,
        List<MemberDirectoryItem> overdue,
        List<NotificationResponse> recentReminderLogs
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
        List<NotificationResponse> recentReminderLogs
    ) {
    }

    public record FinancialReportResponse(
        String period,
        String range,
        LocalDate startDate,
        LocalDate endDate,
        String organizationName,
        String logoUrl,
        String planName,
        PlanAccessResponse planAccess,
        FinancialReportSummary summary,
        List<FinancialReportMonthResponse> months
    ) {
    }

    public record FinancialReportSummary(
        BigDecimal totalRevenue,
        BigDecimal admissionRevenue,
        BigDecimal membershipRevenue,
        BigDecimal duePayments,
        BigDecimal expenses,
        long admissions,
        long activeMembers,
        long overdueMembers
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
        BigDecimal expenses,
        long admissions,
        long activeMembers,
        long overdueMembers,
        String planDetails
    ) {
    }
}
