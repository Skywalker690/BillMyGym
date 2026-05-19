package com.gymflow.repository;

import com.gymflow.domain.OrganizationUsageStats;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationUsageStatsRepository extends JpaRepository<OrganizationUsageStats, Long> {

    Optional<OrganizationUsageStats> findByOrganizationIdAndYearAndMonthAndDeletedFalse(
        Long organizationId,
        int year,
        int month
    );

    default OrganizationUsageStats findSnapshotOrEmpty(com.gymflow.domain.Organization organization, java.time.YearMonth yearMonth) {
        return findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            organization.getId(),
            yearMonth.getYear(),
            yearMonth.getMonthValue()
        ).orElseGet(() -> OrganizationUsageStats.empty(organization, yearMonth));
    }
}
