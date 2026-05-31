package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BillingLifecycleServiceTest {

    private final BillingLifecycleService billingLifecycleService = new BillingLifecycleService();

    @Test
    void paidRenewalStartsFromActualRenewalDateForExpiredOrganization() {
        Organization organization = new Organization();
        organization.setOrganizationType(OrganizationType.REGULAR);
        organization.setSubscriptionPlan(SubscriptionPlan.BILLMYGYM);
        organization.setStatus(OrganizationStatus.PAUSED);
        organization.setSubscriptionDueDate(LocalDate.of(2026, 5, 20));

        LocalDate renewalDate = LocalDate.of(2026, 5, 30);
        LocalDate periodEnd = billingLifecycleService.billingPeriodEnd(renewalDate);

        billingLifecycleService.activatePaidPeriod(organization, renewalDate, periodEnd);

        assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
        assertEquals(LocalDate.of(2026, 6, 29), periodEnd);
        assertEquals(LocalDate.of(2026, 6, 30), organization.getSubscriptionDueDate());
        assertNull(organization.getPausedAt());
        assertNull(organization.getDeactivatedAt());
    }
}
