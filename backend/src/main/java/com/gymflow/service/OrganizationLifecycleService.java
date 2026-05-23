package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.repository.OrganizationRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationLifecycleService {

    private final OrganizationRepository organizationRepository;
    private final BillingLifecycleService billingLifecycleService;

    @Transactional
    public Organization normalizeForAccess(Long organizationId) {
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId).orElse(null);
        if (organization == null) {
            return null;
        }
        normalize(organization, LocalDate.now());
        return organization;
    }

    public void normalize(Organization organization, LocalDate today) {
        if (organization == null || organization.getStatus() == OrganizationStatus.DEACTIVATED) {
            return;
        }

        if (organization.getOrganizationType() == OrganizationType.PILOT
            && organization.getPilotEndDate() != null
            && !organization.getPilotEndDate().isAfter(today)) {
            billingLifecycleService.convertExpiredPilotToRegularBilling(organization, today);
            return;
        }

        if (organization.getOrganizationType() == OrganizationType.REGULAR
            && (organization.getStatus() == OrganizationStatus.ACTIVE || organization.getStatus() == OrganizationStatus.TRIAL)
            && organization.getSubscriptionDueDate() != null
            && !organization.getSubscriptionDueDate().isAfter(today)) {
            billingLifecycleService.markPaused(organization);
        }
    }
}
