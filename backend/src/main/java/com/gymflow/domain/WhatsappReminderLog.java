package com.gymflow.domain;

import com.gymflow.domain.enums.WhatsappReminderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "whatsapp_reminder_logs", indexes = {
    @Index(name = "idx_whatsapp_reminder_logs_org_sent", columnList = "organization_id, sent_at DESC, deleted"),
    @Index(name = "idx_whatsapp_reminder_logs_org_status_sent", columnList = "organization_id, status, sent_at DESC, deleted"),
    @Index(name = "idx_whatsapp_reminder_logs_member_created", columnList = "member_id, created_at DESC, deleted")
})
public class WhatsappReminderLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    private OffsetDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WhatsappReminderStatus status = WhatsappReminderStatus.PENDING;
}
