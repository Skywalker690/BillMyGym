package com.gymflow.api.dto;

import com.gymflow.domain.enums.WhatsappReminderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ReminderDtos {

    public record ReminderRunResponse(
        int processedMembers,
        int organizationsGrouped,
        int digestsSent
    ) {
    }

    public record WhatsappReminderPreviewResponse(
        Long memberId,
        String memberName,
        String phone,
        LocalDate dueDate,
        BigDecimal pendingAmount,
        String gymName,
        String message,
        boolean canSend,
        String disabledReason
    ) {
    }

    public record WhatsappReminderSendResponse(
        Long id,
        Long memberId,
        WhatsappReminderStatus status,
        boolean sent,
        OffsetDateTime sentAt,
        String failureReason
    ) {
    }
}
