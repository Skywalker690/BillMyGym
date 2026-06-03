package com.gymflow.domain;

import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "organizations", indexes = {
    @Index(name = "idx_org_deleted", columnList = "deleted"),
    @Index(name = "idx_org_status_deleted", columnList = "status, deleted"),
    @Index(name = "idx_org_type_status_due", columnList = "organization_type, status, subscription_due_date, deleted"),
    @Index(name = "idx_org_subscription_plan", columnList = "subscription_plan, deleted"),
    @Index(name = "idx_org_created_deleted", columnList = "created_at DESC, deleted"),
    @Index(name = "idx_org_admin_email_deleted", columnList = "admin_email, deleted"),
    @Index(name = "idx_org_admin_phone_deleted", columnList = "admin_phone_number, deleted")
})
public class Organization extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String district;

    private String place;

    private String logoUrl;
    private String mapLocationLink;
    private String address;

    @Column(nullable = false)
    private String adminFullName;

    @Column(nullable = false)
    private String adminPhoneNumber;

    @Column(nullable = false)
    private String adminEmail;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyMembershipFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal admissionFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false)
    private OrganizationType organizationType = OrganizationType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.BILLMYGYM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status = OrganizationStatus.TRIAL;

    private LocalDate trialStartDate;
    private LocalDate trialEndDate;
    private LocalDate subscriptionDueDate;
    private OffsetDateTime pausedAt;
    private OffsetDateTime deactivatedAt;
    private OffsetDateTime deletedAt;

    @Column(unique = true, length = 50)
    private String referralCode;

    @Column(precision = 12, scale = 2)
    private BigDecimal gymCoinBalance = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalCoinsEarned = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalCoinsUsed = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean sixMonthRewardClaimed = false;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

}
