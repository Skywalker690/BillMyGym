package com.gymflow.api.dto;

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
}
