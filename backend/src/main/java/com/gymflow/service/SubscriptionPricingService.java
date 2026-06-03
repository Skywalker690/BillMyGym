package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.MemberMembershipRepository;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        SubscriptionPlan plan = planAccessService.resolvePlan(organization);
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            return priceFor(organization, plan, 0L);
        }
        long activeMembers = memberMembershipRepository.countBillableActiveMembers(
            organization.getId(),
            LocalDate.now(APP_ZONE)
        );
        return priceFor(organization, plan, activeMembers);
    }

    public SubscriptionPriceBreakdown priceFor(Organization organization, SubscriptionPlan selectedPlan) {
        return currentPrice(organization);
    }

    public Map<Long, SubscriptionPriceBreakdown> currentPrices(List<Organization> organizations) {
        if (organizations == null || organizations.isEmpty()) {
            return Map.of();
        }
        LocalDate today = LocalDate.now(APP_ZONE);
        List<Long> billableOrganizationIds = organizations.stream()
            .filter(organization -> organization.getOrganizationType() != OrganizationType.DEMO)
            .map(Organization::getId)
            .toList();
        Map<Long, Long> activeMembersByOrganizationId = billableOrganizationIds.isEmpty()
            ? Map.of()
            : memberMembershipRepository.countBillableActiveMembersByOrganizationIds(billableOrganizationIds, today)
                .stream()
                .collect(Collectors.toMap(
                    MemberMembershipRepository.OrganizationActiveMemberCountProjection::getOrganizationId,
                    projection -> projection.getActiveMembers() == null ? 0L : projection.getActiveMembers()
                ));
        return organizations.stream()
            .collect(Collectors.toMap(
                Organization::getId,
                organization -> priceFor(
                    organization,
                    planAccessService.resolvePlan(organization),
                    activeMembersByOrganizationId.getOrDefault(organization.getId(), 0L)
                )
            ));
    }

    public BigDecimal projectedRevenue(Collection<Organization> organizations) {
        if (organizations == null || organizations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return currentPrices(List.copyOf(organizations)).values().stream()
            .map(SubscriptionPriceBreakdown::totalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<SubscriptionPlan, BigDecimal> projectedRevenueByPlan(List<Organization> organizations) {
        if (organizations == null || organizations.isEmpty()) {
            return Map.of();
        }
        Map<Long, SubscriptionPriceBreakdown> pricingByOrganizationId = currentPrices(organizations);
        return organizations.stream()
            .collect(Collectors.groupingBy(
                organization -> planAccessService.resolvePlan(organization),
                Collectors.mapping(
                    organization -> pricingByOrganizationId.get(organization.getId()).totalAmount(),
                    Collectors.reducing(BigDecimal.ZERO, Function.identity(), BigDecimal::add)
                )
            ));
    }

    public SubscriptionPriceBreakdown priceFor(Organization organization, SubscriptionPlan selectedPlan, long activeMembers) {
        BigDecimal baseAmount = BASE_MONTHLY_AMOUNT;
        Integer includedMembers = INCLUDED_ACTIVE_MEMBERS;
        SubscriptionPlan plan = selectedPlan == null ? BILLMYGYM_PLAN : selectedPlan;
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            return new SubscriptionPriceBreakdown(plan, BigDecimal.ZERO, 0L, null, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        }
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
