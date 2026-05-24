package com.gymflow.domain;

import com.gymflow.domain.enums.SubscriptionStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "organization_subscriptions", indexes = {
    @Index(name = "idx_org_sub_org_deleted", columnList = "organization_id, deleted"),
    @Index(name = "idx_org_sub_org_end_deleted", columnList = "organization_id, end_date DESC, deleted"),
    @Index(name = "idx_org_sub_status_end_deleted", columnList = "status, end_date DESC, deleted")
})
public class OrganizationSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(nullable = false)
    private BigDecimal amount;

    private String razorpaySubscriptionId;
}
