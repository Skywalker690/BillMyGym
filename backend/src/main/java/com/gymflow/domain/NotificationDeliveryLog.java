package com.gymflow.domain;

import com.gymflow.domain.enums.NotificationDeliveryStatus;
import com.gymflow.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "notification_delivery_logs", indexes = {
    @Index(name = "idx_notification_delivery_org_type_attempt", columnList = "organization_id, type, last_attempt, status"),
    @Index(name = "idx_notification_delivery_provider_retry", columnList = "provider, status, next_retry_at"),
    @Index(name = "idx_notification_delivery_provider_message", columnList = "provider_message_id, deleted"),
    @Index(name = "idx_notification_delivery_notification", columnList = "notification_id")
})
public class NotificationDeliveryLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private NotificationLog notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt")
    private Instant lastAttempt;

    @Column(nullable = false)
    private String provider = "SYSTEM";

    private String templateName;

    private String recipient;

    private String providerMessageId;

    @Column(columnDefinition = "text")
    private String errorMessage;

    private Instant nextRetryAt;

    @Column(columnDefinition = "text")
    private String requestPayload;
}
