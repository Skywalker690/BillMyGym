package com.gymflow.domain;

import com.gymflow.domain.enums.ReferralStatus;
import com.gymflow.domain.enums.RewardType;
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

@Entity
@Getter
@Setter
@Table(name = "referrals")
public class Referral extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_org_id", nullable = false)
    private Organization referrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_org_id", nullable = false, unique = true)
    private Organization referred;

    @Column(nullable = false, length = 50)
    private String referralCodeUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReferralStatus status = ReferralStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RewardType rewardType;

}
