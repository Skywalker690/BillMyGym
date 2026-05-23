package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.repository.OrganizationRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PilotConversionService {

    private final OrganizationRepository organizationRepository;
    private final UsageMetricService usageMetricService;
    private final BillingLifecycleService billingLifecycleService;

    @Transactional
    public void convertEligiblePilots() {
        LocalDate today = LocalDate.now();
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndOrganizationTypeAndPilotEndDateBefore(
            OrganizationType.PILOT,
            today.plusDays(1)
        )) {
            if (organization.getOrganizationType() != OrganizationType.PILOT) {
                continue;
            }
            billingLifecycleService.convertExpiredPilotToRegularBilling(organization, today);
            usageMetricService.refreshMonthlySnapshot(organization);
        }
    }
}
