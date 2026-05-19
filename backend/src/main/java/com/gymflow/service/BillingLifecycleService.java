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
    private static final int PILOT_ACCESS_DAYS = 90;

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
        LocalDate pilotEndDate = pilotEndDate(startDate);
        organization.setOrganizationType(OrganizationType.PILOT);
        organization.setSubscriptionPlan(SubscriptionPlan.PILOT);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setPilotStartDate(startDate);
        organization.setPilotEndDate(pilotEndDate);
        organization.setSubscriptionDueDate(pilotEndDate);
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public void activatePaidPeriod(Organization organization, LocalDate periodStart, LocalDate periodEnd) {
        organization.setOrganizationType(OrganizationType.REGULAR);
        if (organization.getSubscriptionPlan() == SubscriptionPlan.PILOT) {
            organization.setSubscriptionPlan(SubscriptionPlan.STARTER);
        }
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setSubscriptionDueDate(nextDueDate(periodEnd));
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public boolean hasCurrentPaidAccess(Organization organization, LocalDate today) {
        return organization.getStatus() == OrganizationStatus.ACTIVE
            && organization.getOrganizationType() == OrganizationType.REGULAR
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
        return startDate.plusDays(PILOT_ACCESS_DAYS);
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
