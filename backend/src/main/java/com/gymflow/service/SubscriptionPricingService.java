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

    public static final int PRO_INCLUDED_ACTIVE_MEMBERS = 600;
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
        long overageMembers = plan == SubscriptionPlan.PRO
            ? Math.max(activeMembers - PRO_INCLUDED_ACTIVE_MEMBERS, 0)
            : 0L;
        BigDecimal overageAmount = PRO_OVERAGE_PER_ACTIVE_MEMBER.multiply(BigDecimal.valueOf(overageMembers));
        return new SubscriptionPriceBreakdown(
            plan,
            baseAmount.add(overageAmount),
            activeMembers,
            includedMembers,
            overageMembers,
            plan == SubscriptionPlan.PRO ? PRO_OVERAGE_PER_ACTIVE_MEMBER : BigDecimal.ZERO,
            overageAmount
        );
    }

    private Integer includedMembers(SubscriptionPlan plan) {
        if (plan == SubscriptionPlan.PRO) {
            return PRO_INCLUDED_ACTIVE_MEMBERS;
        }
        return plan.memberLimit();
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
