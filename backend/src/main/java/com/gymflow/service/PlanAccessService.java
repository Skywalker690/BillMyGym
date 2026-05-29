package com.gymflow.service;

import com.gymflow.api.error.ForbiddenException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanAccessService {

    private final OrganizationRepository organizationRepository;

    public boolean canCreateMembers() {
        return canCreateMembers(currentOrganization());
    }

    public boolean canCreateMembers(Organization organization) {
        return organization != null;
    }

    public boolean canViewAdvancedAnalytics() {
        return canViewAdvancedAnalytics(currentOrganization());
    }

    public boolean canViewAdvancedAnalytics(Organization organization) {
        return resolvePlan(organization).advancedAnalytics();
    }

    public boolean canReceiveMonthlyReports(Organization organization) {
        return resolvePlan(organization).monthlyReports();
    }

    public boolean canReceiveMonthlyReports() {
        return canReceiveMonthlyReports(currentOrganization());
    }

    public boolean canExportReports(Organization organization) {
        return resolvePlan(organization).exportReports();
    }

    public boolean canExportReports() {
        return canExportReports(currentOrganization());
    }

    public boolean canUsePremiumFeatures(Organization organization) {
        return resolvePlan(organization).premiumFeatures();
    }

    public boolean canUsePremiumFeatures() {
        return canUsePremiumFeatures(currentOrganization());
    }

    public PlanAccessSummary summary(Organization organization) {
        return new PlanAccessSummary(
            canCreateMembers(organization),
            canViewAdvancedAnalytics(organization),
            canReceiveMonthlyReports(organization),
            canExportReports(organization),
            canUsePremiumFeatures(organization)
        );
    }

    public void requireAdvancedAnalytics() {
        if (!canViewAdvancedAnalytics()) {
            throw new ForbiddenException("Advanced analytics are available on Starter, Growth, and Pro plans.");
        }
    }

    public void requireMonthlyReports(Organization organization) {
        if (!canReceiveMonthlyReports(organization)) {
            throw new ForbiddenException("Monthly reports are available on Growth and Pro plans.");
        }
    }

    public SubscriptionPlan resolvePlan(Organization organization) {
        if (organization == null) {
            return SubscriptionPlan.STARTER;
        }
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            return organization.getSubscriptionPlan() == null ? SubscriptionPlan.PRO : organization.getSubscriptionPlan();
        }
        return organization.getSubscriptionPlan() == null ? SubscriptionPlan.STARTER : organization.getSubscriptionPlan();
    }

    private Organization currentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            return null;
        }
        return organizationRepository.findByIdAndDeletedFalse(organizationId).orElse(null);
    }

    public record PlanAccessSummary(
        boolean canCreateMembers,
        boolean canViewAdvancedAnalytics,
        boolean canReceiveMonthlyReports,
        boolean canExportReports,
        boolean canUsePremiumFeatures
    ) {
    }
}
