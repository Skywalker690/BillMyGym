package com.gymflow.repository;

import com.gymflow.domain.MembershipPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    List<MembershipPlan> findByOrganizationIdAndDeletedFalseOrderByDefaultPlanDescActiveDescNameAsc(Long organizationId);

    List<MembershipPlan> findByOrganizationIdAndActiveTrueAndDeletedFalseOrderByDefaultPlanDescNameAsc(Long organizationId);

    Optional<MembershipPlan> findByIdAndOrganizationIdAndDeletedFalse(Long id, Long organizationId);

    Optional<MembershipPlan> findByIdAndOrganizationIdAndActiveTrueAndDeletedFalse(Long id, Long organizationId);

    Optional<MembershipPlan> findTopByOrganizationIdAndDefaultPlanTrueAndDeletedFalseOrderByIdAsc(Long organizationId);

    Optional<MembershipPlan> findTopByOrganizationIdAndActiveTrueAndDeletedFalseOrderByDefaultPlanDescNameAsc(Long organizationId);

    long countByOrganizationIdAndActiveTrueAndDeletedFalse(Long organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndDeletedFalse(Long organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(Long organizationId, String name, Long id);

    @Modifying
    @Query("""
        update MembershipPlan plan
        set plan.defaultPlan = false
        where plan.organization.id = :organizationId
          and plan.deleted = false
          and plan.id <> :planId
        """)
    void clearOtherDefaults(@Param("organizationId") Long organizationId, @Param("planId") Long planId);
}
