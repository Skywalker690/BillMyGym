package com.gymflow.domain;

import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
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
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_org_deleted", columnList = "organization_id, deleted"),
    @Index(name = "idx_notifications_org_type_created", columnList = "organization_id, type, created_at DESC, deleted"),
    @Index(name = "idx_notifications_org_status_created", columnList = "organization_id, status, created_at DESC, deleted"),
    @Index(name = "idx_notifications_org_channel_status_created", columnList = "organization_id, channel, status, created_at DESC, deleted"),
    @Index(name = "idx_notifications_org_date_type_channel", columnList = "organization_id, notification_date, type, channel, deleted")
})
public class NotificationLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private int memberCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attemptCount;

    @Column(columnDefinition = "text")
    private String failureReason;

    @Column(name = "notification_date")
    private LocalDate notificationDate;

    private OffsetDateTime sentAt;

    @Column(columnDefinition = "text")
    private String message;

}
