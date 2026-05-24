package com.gymflow.domain;

import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_ts", columnList = "timestamp DESC"),
        @Index(name = "idx_audit_zone", columnList = "zone"),
        @Index(name = "idx_audit_category", columnList = "category"),
        @Index(name = "idx_audit_org_scope_ts", columnList = "organization_id, activity_scope, timestamp DESC"),
        @Index(name = "idx_audit_org_event_ts", columnList = "organization_id, event, timestamp DESC"),
        @Index(name = "idx_audit_org_scope_status_ts", columnList = "organization_id, activity_scope, event_status, timestamp DESC"),
        @Index(name = "idx_audit_login_status_ts", columnList = "activity_scope, event_status, timestamp DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String event;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "actor_name", nullable = false, length = 255)
    private String actorName;

    @Column(name = "actor_email", nullable = false, length = 255)
    private String actorEmail;

    @Column(name = "actor_role", length = 40)
    private String actorRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_scope", length = 40)
    private AuditActivityScope activityScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", length = 20)
    private AuditEventStatus eventStatus;

    @Column(name = "target_name", length = 255)
    private String targetName;

    @Column(length = 20)
    private String zone;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(length = 120)
    private String device;

    @Column(length = 120)
    private String browser;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @PrePersist
    public void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (activityScope == null) {
            activityScope = AuditActivityScope.ADMIN_ACTIVITY;
        }
        if (eventStatus == null) {
            eventStatus = AuditEventStatus.INFO;
        }
    }
}

