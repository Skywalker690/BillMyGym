package com.gymflow.api.dto;

import com.gymflow.api.dto.AnalyticsDtos.RevenueSummaryResponse;
import com.gymflow.api.dto.MemberDtos.MemberDirectoryItem;
import com.gymflow.api.dto.MemberDtos.MemberDirectorySummary;
import com.gymflow.api.dto.NotificationDtos.NotificationResponse;
import com.gymflow.api.dto.SettingDtos.SettingsResponse;
import java.util.List;

public class ReportDtos {

    public record DashboardReportResponse(
        RevenueSummaryResponse revenue,
        MemberDirectorySummary memberSummary,
        List<MemberDirectoryItem> expiringSoon,
        List<MemberDirectoryItem> overdue,
        List<NotificationResponse> recentReminderLogs
    ) {
    }

    public record ReminderReportResponse(
        SettingsResponse settings,
        List<NotificationResponse> recentReminderLogs
    ) {
    }
}
