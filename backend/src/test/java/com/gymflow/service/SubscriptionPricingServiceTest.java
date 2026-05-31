package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.MemberMembershipRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionPricingServiceTest {

    @Mock
    private MemberMembershipRepository memberMembershipRepository;

    @Mock
    private PlanAccessService planAccessService;

    @Test
    void usesBasePriceUntilIncludedActiveMembersAreExceeded() {
        Organization organization = regularOrganization(11L);
        when(memberMembershipRepository.countBillableActiveMembers(eq(11L), any())).thenReturn(100L);
        SubscriptionPricingService service = new SubscriptionPricingService(memberMembershipRepository, planAccessService);

        var price = service.priceFor(organization, SubscriptionPlan.BILLMYGYM);

        assertEquals(SubscriptionPlan.BILLMYGYM, price.plan());
        assertEquals(new BigDecimal("299.00"), price.baseAmount());
        assertEquals(new BigDecimal("299.00"), price.totalAmount());
        assertEquals(100L, price.activeMembers());
        assertEquals(100, price.includedMembers());
        assertEquals(0L, price.overageMembers());
        assertEquals(new BigDecimal("4.00"), price.overageRate());
        assertEquals(new BigDecimal("0.00"), price.overageAmount());
    }

    @Test
    void chargesFourRupeesForEachActiveMemberAboveOneHundred() {
        Organization organization = regularOrganization(12L);
        when(memberMembershipRepository.countBillableActiveMembers(eq(12L), any())).thenReturn(150L);
        SubscriptionPricingService service = new SubscriptionPricingService(memberMembershipRepository, planAccessService);

        var price = service.priceFor(organization, SubscriptionPlan.BILLMYGYM);

        assertEquals(new BigDecimal("499.00"), price.totalAmount());
        assertEquals(150L, price.activeMembers());
        assertEquals(50L, price.overageMembers());
        assertEquals(new BigDecimal("200.00"), price.overageAmount());
    }

    @Test
    void demoOrganizationsRemainFreeShowcaseWorkspaces() {
        Organization organization = new Organization();
        organization.setOrganizationType(OrganizationType.DEMO);
        setId(organization, 13L);
        SubscriptionPricingService service = new SubscriptionPricingService(memberMembershipRepository, planAccessService);

        var price = service.priceFor(organization, SubscriptionPlan.BILLMYGYM);

        assertEquals(SubscriptionPlan.BILLMYGYM, price.plan());
        assertEquals(BigDecimal.ZERO, price.totalAmount());
        assertEquals(0L, price.activeMembers());
        assertEquals(0L, price.overageMembers());
        verifyNoInteractions(memberMembershipRepository);
    }

    private Organization regularOrganization(Long id) {
        Organization organization = new Organization();
        organization.setOrganizationType(OrganizationType.REGULAR);
        organization.setSubscriptionPlan(SubscriptionPlan.BILLMYGYM);
        setId(organization, id);
        return organization;
    }

    private void setId(Object target, Long id) {
        try {
            var field = target.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
