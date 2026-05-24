package com.gymflow.domain;

import com.gymflow.domain.enums.MemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "members", indexes = {
    @Index(name = "idx_members_org_deleted", columnList = "organization_id, deleted"),
    @Index(name = "idx_members_org_status_deleted", columnList = "organization_id, status, deleted"),
    @Index(name = "idx_members_org_created_deleted", columnList = "organization_id, created_at DESC, deleted"),
    @Index(name = "idx_members_org_phone_deleted_btree", columnList = "organization_id, phone, deleted"),
    @Index(name = "idx_members_org_name_deleted_btree", columnList = "organization_id, full_name, deleted")
})
public class Member extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    private OffsetDateTime deletedAt;

    private OffsetDateTime deactivatedAt;

    @Column(length = 1000)
    private String deactivationReason;

    @Version
    @Column(nullable = false)
    private Long version = 0L;
}
