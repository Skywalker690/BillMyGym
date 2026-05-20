package com.gymflow.api.dto;

import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
import java.time.Instant;
import java.util.List;

public class AuditDtos {

    public record AuditPageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
    ) {
    }

    public record GlobalLoginAuditResponse(
        AuditPageResponse<GlobalLoginAuditRow> page,
        long successfulCount,
        long failedCount,
        long suspiciousCount
    ) {
    }

    public record GlobalLoginAuditRow(
        Instant time,
        Long organizationId,
        String organizationName,
        String adminName,
        String email,
        String device,
        String browser,
        String ip,
        String status,
        String description
    ) {
    }

    public record MemberActivityAuditRow(
        Instant time,
        String admin,
        String action,
        String member,
        String description
    ) {
    }

    public record MemberStatusAuditRow(
        Instant time,
        String admin,
        String member,
        String action,
        String description
    ) {
    }

    public record AdminActivityAuditRow(
        Instant time,
        String admin,
        String action,
        String description
    ) {
    }

    public record NotificationAuditResponse(
        AuditPageResponse<NotificationAuditRow> page,
        NotificationAuditSummary summary
    ) {
    }

    public record NotificationAuditSummary(
        long whatsappSent,
        long emailSent,
        long failedCount,
        long todaySentCount,
        long monthlySentCount
    ) {
    }

    public record NotificationAuditRow(
        Instant time,
        NotificationChannel channel,
        String receiver,
        NotificationStatus status,
        String description
    ) {
    }
}

