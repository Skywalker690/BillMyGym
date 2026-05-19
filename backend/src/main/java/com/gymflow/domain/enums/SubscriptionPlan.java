package com.gymflow.domain.enums;

import java.math.BigDecimal;

public enum SubscriptionPlan {
    STARTER(new BigDecimal("299.00"), 100, false, false, false, false),
    GROWTH(new BigDecimal("699.00"), 500, true, true, true, false),
    PRO(new BigDecimal("1499.00"), null, true, true, true, true),
    PILOT(BigDecimal.ZERO, 100, true, true, true, true);

    private final BigDecimal monthlyAmount;
    private final Integer memberLimit;
    private final boolean advancedAnalytics;
    private final boolean monthlyReports;
    private final boolean exportReports;
    private final boolean premiumFeatures;

    SubscriptionPlan(
        BigDecimal monthlyAmount,
        Integer memberLimit,
        boolean advancedAnalytics,
        boolean monthlyReports,
        boolean exportReports,
        boolean premiumFeatures
    ) {
        this.monthlyAmount = monthlyAmount;
        this.memberLimit = memberLimit;
        this.advancedAnalytics = advancedAnalytics;
        this.monthlyReports = monthlyReports;
        this.exportReports = exportReports;
        this.premiumFeatures = premiumFeatures;
    }

    public BigDecimal monthlyAmount() {
        return monthlyAmount;
    }

    public Integer memberLimit() {
        return memberLimit;
    }

    public boolean hasMemberLimit() {
        return memberLimit != null;
    }

    public boolean advancedAnalytics() {
        return advancedAnalytics;
    }

    public boolean monthlyReports() {
        return monthlyReports;
    }

    public boolean exportReports() {
        return exportReports;
    }

    public boolean premiumFeatures() {
        return premiumFeatures;
    }
}
