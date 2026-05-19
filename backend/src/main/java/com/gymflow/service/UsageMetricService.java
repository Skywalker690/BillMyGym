package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.UsageMetricRepository;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageMetricService {

    private final UsageMetricRepository usageMetricRepository;
    private final MemberRepository memberRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public UsageMetric recordEmailSent(Organization organization) {
        UsageMetric metric = resolveMetric(organization);
        metric.incrementMonthlyEmailCount();
        refreshSnapshot(metric);
        return usageMetricRepository.save(metric);
    }

    @Transactional
    public UsageMetric recordWhatsappDelivered(Organization organization) {
        UsageMetric metric = resolveMetric(organization);
        metric.incrementMonthlyWhatsappCount();
        refreshSnapshot(metric);
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
        YearMonth currentMonth = YearMonth.now();
        return usageMetricRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            organization.getId(),
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        ).orElseGet(() -> UsageMetric.empty(organization, currentMonth));
    }

    private void refreshSnapshot(UsageMetric metric) {
        Organization organization = metric.getOrganization();
        metric.setActiveMembers((int) memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(
            organization.getId(),
            MemberStatus.ACTIVE
        ));
        metric.setActiveAdmins((int) appUserRepository.countByOrganizationIdAndRoleAndDeletedFalse(
            organization.getId(),
            com.gymflow.domain.enums.Role.ORG_ADMIN
        ));
        metric.setSubscriptionStatus(organization.getStatus().name());
    }
}
