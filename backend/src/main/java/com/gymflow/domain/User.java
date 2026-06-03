package com.gymflow.domain;

import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
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
@Table(name = "users", indexes = {
    @Index(name = "idx_users_org_deleted", columnList = "organization_id, deleted"),
    @Index(name = "idx_users_org_role_deleted", columnList = "organization_id, role, deleted"),
    @Index(name = "idx_users_status_deleted", columnList = "status, deleted")
})
public class User extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "google_id", unique = true, length = 255)
    private String googleId;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "terms_accepted")
    private Boolean termsAccepted = false;

    @Column(name = "privacy_accepted")
    private Boolean privacyAccepted = false;

    @Column(name = "policy_version", length = 32)
    private String policyVersion;

    @Column(name = "policy_accepted_at")
    private OffsetDateTime policyAcceptedAt;

    @Column(name = "policy_ip_address", length = 64)
    private String policyIpAddress;

    @Column(name = "policy_device_info", length = 255)
    private String policyDeviceInfo;
}
