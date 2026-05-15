package com.gymflow.domain;

import com.gymflow.domain.enums.MembershipDuration;
import com.gymflow.domain.enums.MembershipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "members")
public class Member extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phoneNumber;

    private String email;

    @Column(nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false)
    private LocalDate membershipStartDate;

    @Column(nullable = false)
    private LocalDate membershipEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipDuration membershipDuration;

    private Integer customDurationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus membershipStatus = MembershipStatus.ACTIVE;

    private OffsetDateTime overdueMarkedAt;

    private OffsetDateTime deletedAt;

    @Column(length = 1000)
    private String notes;

}
