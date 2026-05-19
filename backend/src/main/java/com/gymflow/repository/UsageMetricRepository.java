package com.gymflow.repository;

import com.gymflow.domain.UsageMetric;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageMetricRepository extends JpaRepository<UsageMetric, Long> {

    Optional<UsageMetric> findByOrganizationIdAndYearAndMonthAndDeletedFalse(Long organizationId, int year, int month);

    List<UsageMetric> findByYearAndMonthAndDeletedFalse(int year, int month);
}
