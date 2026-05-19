package com.gymflow.domain;

import com.gymflow.domain.enums.IdempotencyResourceType;
import com.gymflow.domain.enums.MembershipOperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "membership_operation_idempotency")
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
