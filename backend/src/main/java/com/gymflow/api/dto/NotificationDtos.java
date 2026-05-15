package com.gymflow.api.dto;

import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import java.time.OffsetDateTime;

public class NotificationDtos {

    public record NotificationResponse(
        Long id,
        Long memberId,
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        NotificationStatus status,
        int attemptCount,
        String failureReason,
        OffsetDateTime sentAt,
        String message,
        OffsetDateTime createdAt
    ) {
    }
}
