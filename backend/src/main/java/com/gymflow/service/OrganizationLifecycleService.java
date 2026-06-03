package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.repository.OrganizationRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationLifecycleService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final OrganizationRepository organizationRepository;
    private final BillingLifecycleService billingLifecycleService;

    @Transactional
    public Organization normalizeForAccess(Long organizationId) {
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId).orElse(null);
        if (organization == null) {
            return null;
        }
        normalize(organization, LocalDate.now(APP_ZONE));
        return organization;
    }

    public void normalize(Organization organization, LocalDate today) {
        if (organization == null || organization.getStatus() == OrganizationStatus.DEACTIVATED) {
            return;
        }

        if (organization.getOrganizationType() == com.gymflow.domain.enums.OrganizationType.REGULAR
            && (organization.getStatus() == OrganizationStatus.ACTIVE || organization.getStatus() == OrganizationStatus.TRIAL)
            && !billingLifecycleService.hasCurrentAccessWindow(organization, today)) {
            if (organization.getStatus() == OrganizationStatus.TRIAL) {
                billingLifecycleService.expireRegularTrial(organization, today);
            } else {
                billingLifecycleService.markPaused(organization);
            }
        }
    }
}
