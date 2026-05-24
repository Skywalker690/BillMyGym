package com.gymflow.api.dto;

import com.gymflow.domain.enums.ReferralStatus;
import com.gymflow.domain.enums.RewardType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ReferralDtos {

    public record ReferralDashboardResponse(
        String referralCode,
        long totalReferrals,
        long successfulReferrals,
        long pendingReferrals,
        BigDecimal gymCoinBalance,
        BigDecimal totalCoinsEarned,
        boolean sixMonthRewardClaimed,
        long nextMilestoneRemaining,
        List<ReferralEntry> recentReferrals,
        List<RewardEntry> recentRewards
    ) {
    }

    public record ReferralEntry(
        String organizationName,
        ReferralStatus status,
        RewardType rewardType,
        Instant date
    ) {
    }

    public record RewardEntry(
        RewardType rewardType,
        BigDecimal amount,
        int monthsRewarded,
        Instant date
    ) {
    }

    public record AdminReferralAnalyticsResponse(
        long totalReferralSignups,
        long totalSuccessfulReferrals,
        BigDecimal totalCoinsRewarded,
        long totalSixMonthRewards
    ) {
    }
}
