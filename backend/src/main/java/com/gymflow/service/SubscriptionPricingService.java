package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.MemberRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionPricingService {

    public static final int STARTER_INCLUDED_ACTIVE_MEMBERS = 100;
    public static final int GROWTH_INCLUDED_ACTIVE_MEMBERS = 200;
    public static final int PRO_INCLUDED_ACTIVE_MEMBERS = 600;
    public static final BigDecimal STARTER_GROWTH_OVERAGE_PER_ACTIVE_MEMBER = new BigDecimal("5.00");
    public static final BigDecimal PRO_OVERAGE_PER_ACTIVE_MEMBER = new BigDecimal("3.00");

    private final MemberRepository memberRepository;
    private final PlanAccessService planAccessService;

    public SubscriptionPriceBreakdown currentPrice(Organization organization) {
        return priceFor(organization, planAccessService.resolvePlan(organization));
    }

    public SubscriptionPriceBreakdown priceFor(Organization organization, SubscriptionPlan selectedPlan) {
        SubscriptionPlan plan = selectedPlan == null ? SubscriptionPlan.STARTER : selectedPlan;
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            return new SubscriptionPriceBreakdown(plan, BigDecimal.ZERO, 0L, null, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long activeMembers = memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(
            organization.getId(),
            MemberStatus.ACTIVE
        );
        BigDecimal baseAmount = plan.monthlyAmount();
        Integer includedMembers = includedMembers(plan);
        long overageMembers = includedMembers == null ? 0L : Math.max(activeMembers - includedMembers, 0);
        BigDecimal overageRate = switch (plan) {
            case STARTER, GROWTH -> STARTER_GROWTH_OVERAGE_PER_ACTIVE_MEMBER;
            case PRO -> PRO_OVERAGE_PER_ACTIVE_MEMBER;
            case PILOT -> BigDecimal.ZERO;
        };
        BigDecimal overageAmount = overageRate.multiply(BigDecimal.valueOf(overageMembers));
        return new SubscriptionPriceBreakdown(
            plan,
            baseAmount.add(overageAmount),
            activeMembers,
            includedMembers,
            overageMembers,
            overageRate,
            overageAmount
        );
    }

    private Integer includedMembers(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> STARTER_INCLUDED_ACTIVE_MEMBERS;
            case GROWTH -> GROWTH_INCLUDED_ACTIVE_MEMBERS;
            case PRO -> PRO_INCLUDED_ACTIVE_MEMBERS;
            case PILOT -> plan.memberLimit();
        };
    }

    public record SubscriptionPriceBreakdown(
        SubscriptionPlan plan,
        BigDecimal totalAmount,
        long activeMembers,
        Integer includedMembers,
        long overageMembers,
        BigDecimal overageRate,
        BigDecimal overageAmount
    ) {
        public BigDecimal baseAmount() {
            return plan.monthlyAmount();
        }
    }
}
