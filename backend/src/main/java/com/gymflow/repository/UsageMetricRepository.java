package com.gymflow.repository;

import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.OrganizationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageMetricRepository extends JpaRepository<UsageMetric, Long> {

    Optional<UsageMetric> findByOrganizationIdAndYearAndMonthAndDeletedFalse(Long organizationId, int year, int month);

    @Modifying
    @Query("delete from UsageMetric metric where metric.organization.id = :organizationId")
    int deleteAllByOrganizationId(@Param("organizationId") Long organizationId);

    List<UsageMetric> findByYearAndMonthAndDeletedFalse(int year, int month);

    @Query("""
        select metric
        from UsageMetric metric
        where metric.deleted = false
          and metric.year = :year
          and metric.month = :month
          and metric.organization.deleted = false
          and metric.organization.organizationType = :organizationType
        """)
    List<UsageMetric> findByYearAndMonthAndOrganizationType(
            @Param("year") int year,
            @Param("month") int month,
            @Param("organizationType") OrganizationType organizationType
    );
}
