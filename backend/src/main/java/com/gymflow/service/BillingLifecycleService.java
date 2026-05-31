package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class BillingLifecycleService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int TRIAL_ACCESS_DAYS = 60;

    public void startRegularTrial(Organization organization, LocalDate startDate) {
        LocalDate trialEndDate = trialEndDate(startDate);
        organization.setOrganizationType(OrganizationType.REGULAR);
        organization.setSubscriptionPlan(SubscriptionPlan.BILLMYGYM);
        organization.setStatus(OrganizationStatus.TRIAL);
        organization.setTrialStartDate(startDate);
        organization.setTrialEndDate(trialEndDate);
        organization.setSubscriptionDueDate(trialEndDate);
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public void startDemoAccess(Organization organization, LocalDate startDate) {
        organization.setOrganizationType(OrganizationType.DEMO);
        organization.setSubscriptionPlan(SubscriptionPlan.BILLMYGYM);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTrialStartDate(null);
        organization.setTrialEndDate(null);
        organization.setSubscriptionDueDate(null);
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public void activatePaidPeriod(Organization organization, LocalDate periodStart, LocalDate periodEnd) {
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            startDemoAccess(organization, periodStart);
            return;
        }
        organization.setOrganizationType(OrganizationType.REGULAR);
        organization.setSubscriptionPlan(SubscriptionPlan.BILLMYGYM);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTrialStartDate(null);
        organization.setTrialEndDate(null);
        organization.setSubscriptionDueDate(nextDueDate(periodEnd));
        organization.setPausedAt(null);
        organization.setDeactivatedAt(null);
    }

    public void expireRegularTrial(Organization organization, LocalDate today) {
        organization.setOrganizationType(OrganizationType.REGULAR);
        organization.setSubscriptionPlan(SubscriptionPlan.BILLMYGYM);
        organization.setStatus(OrganizationStatus.PAUSED);
        organization.setSubscriptionDueDate(today);
        if (organization.getPausedAt() == null) {
            organization.setPausedAt(OffsetDateTime.now(APP_ZONE));
        }
        organization.setDeactivatedAt(null);
    }

    public boolean hasCurrentPaidAccess(Organization organization, LocalDate today) {
        return organization.getStatus() == OrganizationStatus.ACTIVE
            && organization.getOrganizationType() != OrganizationType.DEMO
            && organization.getSubscriptionDueDate() != null
            && organization.getSubscriptionDueDate().isAfter(today);
    }

    public boolean hasCurrentAccessWindow(Organization organization, LocalDate today) {
        if (organization == null || organization.getOrganizationType() == OrganizationType.DEMO) {
            return true;
        }
        if (organization.getStatus() != OrganizationStatus.ACTIVE && organization.getStatus() != OrganizationStatus.TRIAL) {
            return false;
        }
        return organization.getSubscriptionDueDate() != null && organization.getSubscriptionDueDate().isAfter(today);
    }

    public boolean isDeactivated(Organization organization) {
        return organization.getStatus() == OrganizationStatus.DEACTIVATED
            || organization.getDeactivatedAt() != null;
    }

    public LocalDate trialEndDate(LocalDate startDate) {
        return startDate.plusDays(TRIAL_ACCESS_DAYS - 1L);
    }

    public LocalDate billingPeriodEnd(LocalDate periodStart) {
        return periodStart.plusMonths(1).minusDays(1);
    }

    public LocalDate nextDueDate(LocalDate periodEnd) {
        return periodEnd.plusDays(1);
    }

    public void markPaused(Organization organization) {
        if (organization.getSubscriptionDueDate() == null) {
            organization.setSubscriptionDueDate(LocalDate.now(APP_ZONE));
        }
        organization.setStatus(OrganizationStatus.PAUSED);
        if (organization.getPausedAt() == null) {
            organization.setPausedAt(OffsetDateTime.now(APP_ZONE));
        }
        organization.setDeactivatedAt(null);
    }
}
