package com.gymflow.domain;

import com.gymflow.domain.enums.MembershipStatus;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "member_memberships", indexes = {
    @Index(name = "idx_member_memberships_member_deleted", columnList = "member_id, deleted"),
    @Index(name = "idx_member_memberships_member_end_created", columnList = "member_id, end_date DESC, created_at DESC, deleted"),
    @Index(name = "idx_member_memberships_org_status_deleted", columnList = "organization_id, status, deleted"),
    @Index(name = "idx_member_memberships_org_status_end", columnList = "organization_id, status, end_date, deleted"),
    @Index(name = "idx_member_memberships_org_member_start", columnList = "organization_id, member_id, start_date DESC, created_at DESC, deleted"),
    @Index(name = "idx_member_memberships_org_last_sent", columnList = "organization_id, last_sent_at, status, deleted")
})
public class MemberMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_plan_id", nullable = false)
    private MembershipPlan membershipPlan;

    @Column(nullable = false)
    private int durationMonths;

    @Column(nullable = false, length = 120)
    private String planName = "Regular";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPlanPrice = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal planAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "last_sent_at")
    private OffsetDateTime lastSentAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;
}
