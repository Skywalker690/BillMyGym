package com.gymflow.domain;

import com.gymflow.domain.enums.IdempotencyResourceType;
import com.gymflow.domain.enums.MembershipOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "membership_operation_idempotency", indexes = {
    @Index(name = "idx_membership_idempotency_org_key", columnList = "organization_id, idempotency_key, deleted"),
    @Index(name = "idx_membership_idempotency_member_operation", columnList = "organization_id, member_id, operation_type, deleted"),
    @Index(name = "idx_membership_idempotency_resource", columnList = "resource_type, resource_id, deleted")
})
public class MembershipOperationIdempotency extends BaseEntity {

    @Column(nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 255)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyResourceType resourceType;

    @Column(nullable = false)
    private Long resourceId;
}
