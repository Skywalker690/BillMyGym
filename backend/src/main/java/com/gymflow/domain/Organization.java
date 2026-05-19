package com.gymflow.domain;

import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String district;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false)
    private OrganizationType organizationType = OrganizationType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.STARTER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status = OrganizationStatus.TRIAL;

    private LocalDate trialStartDate;
    private LocalDate trialEndDate;
    private LocalDate pilotStartDate;
    private LocalDate pilotEndDate;
    private LocalDate subscriptionDueDate;
    private OffsetDateTime pausedAt;
    private OffsetDateTime deactivatedAt;
    private OffsetDateTime deletedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

}
