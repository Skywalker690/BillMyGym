package com.gymflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "api_request_log", indexes = {
    @Index(name = "idx_api_request_org_request_endpoint", columnList = "organization_id, request_id, endpoint"),
    @Index(name = "idx_api_request_org_created", columnList = "organization_id, created_at DESC")
})
public class ApiRequestLog {

    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "request_id", nullable = false, length = 255)
    private String requestId;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "response_hash", nullable = false, length = 128)
    private String responseHash;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
