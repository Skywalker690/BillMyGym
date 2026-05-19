package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
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
            organization.setOrganizationType(OrganizationType.REGULAR);
            organization.setStatus(OrganizationStatus.TRIAL);
            organization.setTrialStartDate(today);
            organization.setTrialEndDate(today.plusDays(14));
            organization.setSubscriptionDueDate(today.plusDays(14));
            usageMetricService.refreshMonthlySnapshot(organization);
        }
    }
}
