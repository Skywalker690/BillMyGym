package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class BillingLifecycleService {

    private static final int TRIAL_ACCESS_DAYS = 14;
    private static final int DEMO_ACCESS_DAYS = 90;
    private static final int PILOT_BILLING_DAYS = 30;

    public void startRegularTrial(Organization organization, LocalDate startDate) {
        LocalDate trialEndDate = trialEndDate(startDate);
        organization.setOrganizationType(OrganizationType.REGULAR);
        if (organization.getSubscriptionPlan() == null || organization.getSubscriptionPlan() == SubscriptionPlan.PILOT) {
            organization.setSubscriptionPlan(SubscriptionPlan.STARTER);
        }
        organization.setStatus(OrganizationStatus.TRIAL);
        organization.setTrialStartDate(startDate);
        organization.setTrialEndDate(trialEndDate);
        organization.setSubscriptionDueDate(trialEndDate);
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public void startPilotAccess(Organization organization, LocalDate startDate) {
        LocalDate pilotEndDate = startDate.plusDays(PILOT_BILLING_DAYS);
        organization.setOrganizationType(OrganizationType.PILOT);
        organization.setSubscriptionPlan(SubscriptionPlan.PILOT);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setPilotStartDate(startDate);
        organization.setPilotEndDate(pilotEndDate);
        organization.setSubscriptionDueDate(pilotEndDate);
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public void startDemoAccess(Organization organization, LocalDate startDate) {
        LocalDate demoEndDate = pilotEndDate(startDate);
        organization.setOrganizationType(OrganizationType.DEMO);
        organization.setSubscriptionPlan(SubscriptionPlan.PILOT);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setPilotStartDate(startDate);
        organization.setPilotEndDate(demoEndDate);
        organization.setSubscriptionDueDate(demoEndDate);
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public void activatePaidPeriod(Organization organization, LocalDate periodStart, LocalDate periodEnd) {
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            startDemoAccess(organization, periodStart);
            return;
        }
        if (organization.getOrganizationType() != OrganizationType.PILOT) {
            organization.setOrganizationType(OrganizationType.REGULAR);
        }
        if (organization.getSubscriptionPlan() == SubscriptionPlan.PILOT && organization.getOrganizationType() != OrganizationType.PILOT) {
            organization.setSubscriptionPlan(SubscriptionPlan.STARTER);
        }
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setSubscriptionDueDate(nextDueDate(periodEnd));
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public boolean hasCurrentPaidAccess(Organization organization, LocalDate today) {
        return organization.getStatus() == OrganizationStatus.ACTIVE
            && organization.getOrganizationType() != OrganizationType.DEMO
            && organization.getSubscriptionDueDate() != null
            && organization.getSubscriptionDueDate().isAfter(today);
    }

    public boolean isDeactivated(Organization organization) {
        return organization.getStatus() == OrganizationStatus.DEACTIVATED
            || organization.getDeactivatedAt() != null;
    }

    public LocalDate trialEndDate(LocalDate startDate) {
        return startDate.plusDays(TRIAL_ACCESS_DAYS - 1L);
    }

    public LocalDate pilotEndDate(LocalDate startDate) {
        return startDate.plusDays(DEMO_ACCESS_DAYS);
    }

    public LocalDate billingPeriodEnd(LocalDate periodStart) {
        return periodStart.plusMonths(1).minusDays(1);
    }

    public LocalDate nextDueDate(LocalDate periodEnd) {
        return periodEnd.plusDays(1);
    }

    public void markPaused(Organization organization) {
        organization.setStatus(OrganizationStatus.PAUSED);
        if (organization.getPausedAt() == null) {
            organization.setPausedAt(OffsetDateTime.now());
        }
    }
}
