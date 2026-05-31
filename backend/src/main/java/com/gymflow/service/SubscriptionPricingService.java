package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.MemberMembershipRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionPricingService {

    public static final SubscriptionPlan BILLMYGYM_PLAN = SubscriptionPlan.BILLMYGYM;
    public static final BigDecimal BASE_MONTHLY_AMOUNT = new BigDecimal("299.00");
    public static final int INCLUDED_ACTIVE_MEMBERS = 100;
    public static final BigDecimal OVERAGE_PER_ACTIVE_MEMBER = new BigDecimal("4.00");

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final MemberMembershipRepository memberMembershipRepository;
    private final PlanAccessService planAccessService;

    public SubscriptionPriceBreakdown currentPrice(Organization organization) {
        return priceFor(organization, planAccessService.resolvePlan(organization));
    }

    public SubscriptionPriceBreakdown priceFor(Organization organization, SubscriptionPlan selectedPlan) {
        SubscriptionPlan plan = BILLMYGYM_PLAN;
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            return new SubscriptionPriceBreakdown(plan, BigDecimal.ZERO, 0L, null, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long activeMembers = memberMembershipRepository.countBillableActiveMembers(
            organization.getId(),
            LocalDate.now(APP_ZONE)
        );
        BigDecimal baseAmount = BASE_MONTHLY_AMOUNT;
        Integer includedMembers = INCLUDED_ACTIVE_MEMBERS;
        long overageMembers = Math.max(activeMembers - includedMembers, 0);
        BigDecimal overageRate = OVERAGE_PER_ACTIVE_MEMBER;
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
            return BASE_MONTHLY_AMOUNT;
        }
    }
}
