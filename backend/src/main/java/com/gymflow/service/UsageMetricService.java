package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationUsageStats;
import com.gymflow.domain.UsageMetric;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.OrganizationUsageStatsRepository;
import com.gymflow.repository.UsageMetricRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageMetricService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final UsageMetricRepository usageMetricRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationUsageStatsRepository organizationUsageStatsRepository;

    @Transactional
    public UsageMetric recordEmailSent(Organization organization) {
        UsageMetric metric = resolveMetric(organization);
        metric.incrementMonthlyEmailCount();
        refreshSnapshot(metric);
        recordOrganizationUsage(organization, false, true);
        return usageMetricRepository.save(metric);
    }

    @Transactional
    public UsageMetric recordWhatsappDelivered(Organization organization) {
        UsageMetric metric = resolveMetric(organization);
        metric.incrementMonthlyWhatsappCount();
        refreshSnapshot(metric);
        recordOrganizationUsage(organization, true, false);
        return usageMetricRepository.save(metric);
    }

    @Transactional
    public UsageMetric recordDigestSent(Organization organization) {
        UsageMetric metric = resolveMetric(organization);
        metric.incrementMonthlyDigestCount();
        refreshSnapshot(metric);
        return usageMetricRepository.save(metric);
    }

    @Transactional
    public UsageMetric refreshMonthlySnapshot(Organization organization) {
        UsageMetric metric = resolveMetric(organization);
        refreshSnapshot(metric);
        return usageMetricRepository.save(metric);
    }

    public UsageMetric resolveMetric(Organization organization) {
        YearMonth currentMonth = YearMonth.now(APP_ZONE);
        return usageMetricRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            organization.getId(),
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        ).orElseGet(() -> UsageMetric.empty(organization, currentMonth));
    }

    private void refreshSnapshot(UsageMetric metric) {
        Organization organization = metric.getOrganization();
        metric.setActiveMembers((int) memberMembershipRepository.countBillableActiveMembers(
            organization.getId(),
            LocalDate.now(APP_ZONE)
        ));
        metric.setActiveAdmins((int) appUserRepository.countByOrganizationIdAndRoleAndDeletedFalse(
            organization.getId(),
            com.gymflow.domain.enums.Role.ORG_ADMIN
        ));
        metric.setSubscriptionStatus(organization.getStatus().name());
    }

    private void recordOrganizationUsage(Organization organization, boolean whatsappSent, boolean emailSent) {
        YearMonth currentMonth = YearMonth.now(APP_ZONE);
        OrganizationUsageStats stats = organizationUsageStatsRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            organization.getId(),
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        ).orElseGet(() -> OrganizationUsageStats.empty(organization, currentMonth));
        if (whatsappSent) {
            stats.incrementWhatsappSentCount();
        }
        if (emailSent) {
            stats.incrementEmailSentCount();
        }
        organizationUsageStatsRepository.save(stats);
    }
}
