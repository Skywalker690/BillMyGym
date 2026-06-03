package com.gymflow.service;

import com.gymflow.domain.Organization;
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
        return true;
    }

    public boolean canReceiveMonthlyReports(Organization organization) {
        return true;
    }

    public boolean canReceiveMonthlyReports() {
        return canReceiveMonthlyReports(currentOrganization());
    }

    public boolean canExportReports(Organization organization) {
        return true;
    }

    public boolean canExportReports() {
        return canExportReports(currentOrganization());
    }

    public boolean canUsePremiumFeatures(Organization organization) {
        return true;
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
    }

    public void requireMonthlyReports(Organization organization) {
    }

    public SubscriptionPlan resolvePlan(Organization organization) {
        return SubscriptionPlan.BILLMYGYM;
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
