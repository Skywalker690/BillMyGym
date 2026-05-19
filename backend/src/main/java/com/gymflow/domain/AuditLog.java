package com.gymflow.domain;

import com.gymflow.domain.enums.AuditCategory;
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
        @Index(name = "idx_audit_category", columnList = "category")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditCategory category;

    @Column(length = 20)
    private String zone;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @PrePersist
    public void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}

