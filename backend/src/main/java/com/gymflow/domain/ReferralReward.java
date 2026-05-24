package com.gymflow.domain;

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

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "referral_rewards")
public class ReferralReward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RewardType rewardType;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private int monthsRewarded = 0;

    @Column(nullable = false)
    private boolean rewardUsed = false;

}
