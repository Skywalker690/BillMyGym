package com.gymflow.domain.enums;

import java.math.BigDecimal;

public enum SubscriptionPlan {
    BILLMYGYM(new BigDecimal("299.00"), 100, new BigDecimal("4.00"));

    private final BigDecimal monthlyAmount;
    private final int includedActiveMembers;
    private final BigDecimal overageRate;

    SubscriptionPlan(
        BigDecimal monthlyAmount,
        int includedActiveMembers,
        BigDecimal overageRate
    ) {
        this.monthlyAmount = monthlyAmount;
        this.includedActiveMembers = includedActiveMembers;
        this.overageRate = overageRate;
    }

    public BigDecimal monthlyAmount() {
        return monthlyAmount;
    }

    public Integer memberLimit() {
        return includedActiveMembers;
    }

    public boolean hasMemberLimit() {
        return false;
    }

    public int includedActiveMembers() {
        return includedActiveMembers;
    }

    public BigDecimal overageRate() {
        return overageRate;
    }

    public boolean advancedAnalytics() {
        return true;
    }

    public boolean monthlyReports() {
        return true;
    }

    public boolean exportReports() {
        return true;
    }

    public boolean premiumFeatures() {
        return true;
    }
}
