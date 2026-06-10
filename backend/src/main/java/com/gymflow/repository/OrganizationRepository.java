package com.gymflow.repository;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    interface OrganizationPlanCountProjection {
        SubscriptionPlan getSubscriptionPlan();
        Long getCount();
    }

    Optional<Organization> findByIdAndDeletedFalse(Long id);

    Optional<Organization> findByReferralCode(String referralCode);

    List<Organization> findAllByDeletedFalse();

    long countByDeletedFalse();

    long countByDeletedFalseAndOrganizationTypeNot(OrganizationType organizationType);

    List<Organization> findAllByDeletedFalseAndStatus(OrganizationStatus status);

    List<Organization> findAllByDeletedFalseAndOrganizationTypeAndStatus(
            OrganizationType organizationType,
            OrganizationStatus status
    );

    long countByDeletedFalseAndStatus(OrganizationStatus status);

    long countByDeletedFalseAndOrganizationTypeAndStatus(
            OrganizationType organizationType,
            OrganizationStatus status
    );

    long countByDeletedFalseAndStatusIn(Collection<OrganizationStatus> statuses);

    long countByDeletedFalseAndOrganizationTypeAndStatusIn(
            OrganizationType organizationType,
            Collection<OrganizationStatus> statuses
    );

    long countByDeletedFalseAndOrganizationType(OrganizationType organizationType);

    long countByDeletedFalseAndSubscriptionPlan(SubscriptionPlan subscriptionPlan);

    @Query("""
        select organization.subscriptionPlan as subscriptionPlan,
               count(organization) as count
        from Organization organization
        where organization.deleted = false
          and organization.organizationType = :organizationType
          and organization.status = :status
        group by organization.subscriptionPlan
        """)
    List<OrganizationPlanCountProjection> countBySubscriptionPlanForOrganizationTypeAndStatus(
            @Param("organizationType") OrganizationType organizationType,
            @Param("status") OrganizationStatus status
    );

    @Query("""
        select organization.subscriptionPlan as subscriptionPlan,
               count(organization) as count
        from Organization organization
        where organization.deleted = false
          and organization.organizationType = :organizationType
          and organization.status <> :excludedStatus
        group by organization.subscriptionPlan
        """)
    List<OrganizationPlanCountProjection> countBySubscriptionPlanForOrganizationTypeExcludingStatus(
            @Param("organizationType") OrganizationType organizationType,
            @Param("excludedStatus") OrganizationStatus excludedStatus
    );

    long countByDeletedFalseAndCreatedAtBetween(
            OffsetDateTime start,
            OffsetDateTime end
    );

    long countByDeletedFalseAndOrganizationTypeAndCreatedAtBetween(
            OrganizationType organizationType,
            OffsetDateTime start,
            OffsetDateTime end
    );

    @Query("""
        select min(organization.createdAt)
        from Organization organization
        where organization.deleted = false
        """)
    OffsetDateTime findEarliestCreatedAt();

    @Query("""
        select min(organization.createdAt)
        from Organization organization
        where organization.deleted = false
          and organization.organizationType = :organizationType
        """)
    OffsetDateTime findEarliestCreatedAtForOrganizationType(@Param("organizationType") OrganizationType organizationType);

    List<Organization> findAllByDeletedFalseAndStatusIn(
            List<OrganizationStatus> statuses
    );

    @Query(
            value = """
        select o from Organization o
        where o.deleted = false
          and (
            :search is null
            or lower(o.name) like :pattern
            or lower(o.place) like :pattern
            or lower(o.district) like :pattern
            or lower(o.adminEmail) like :pattern
            or lower(o.adminFullName) like :pattern
          )
          and (
            (:includeAllTypes = true)
            or (:organizationType is not null and o.organizationType = :organizationType)
            or (
              :includeAllTypes = false and :organizationType is null and (
                (:pilotOnly = true and o.organizationType = com.gymflow.domain.enums.OrganizationType.DEMO)
                or (:pilotOnly = false and o.organizationType <> com.gymflow.domain.enums.OrganizationType.DEMO)
              )
            )
          )
          and (:status is null or o.status = :status)
          and (:plan is null or o.subscriptionPlan = :plan)
        order by
          case
            when o.status = com.gymflow.domain.enums.OrganizationStatus.PAUSED
            then 0
            else 1
          end,
          o.createdAt desc
        """,
            countQuery = """
        select count(o) from Organization o
        where o.deleted = false
          and (
            :search is null
            or lower(o.name) like :pattern
            or lower(o.place) like :pattern
            or lower(o.district) like :pattern
            or lower(o.adminEmail) like :pattern
            or lower(o.adminFullName) like :pattern
          )
          and (
            (:includeAllTypes = true)
            or (:organizationType is not null and o.organizationType = :organizationType)
            or (
              :includeAllTypes = false and :organizationType is null and (
                (:pilotOnly = true and o.organizationType = com.gymflow.domain.enums.OrganizationType.DEMO)
                or (:pilotOnly = false and o.organizationType <> com.gymflow.domain.enums.OrganizationType.DEMO)
              )
            )
          )
          and (:status is null or o.status = :status)
          and (:plan is null or o.subscriptionPlan = :plan)
        """
    )
    Page<Organization> findDirectoryPage(
            @Param("search") String search,
            @Param("pattern") String pattern,
            @Param("includeAllTypes") boolean includeAllTypes,
            @Param("pilotOnly") boolean pilotOnly,
            @Param("organizationType") OrganizationType organizationType,
            @Param("status") OrganizationStatus status,
            @Param("plan") SubscriptionPlan plan,
            Pageable pageable
    );

    List<Organization> findAllByDeletedFalseAndOrganizationTypeAndSubscriptionDueDateIn(
            OrganizationType organizationType,
            List<LocalDate> dueDates
    );

    List<Organization> findAllByDeletedFalseAndOrganizationTypeInAndSubscriptionDueDateIn(
            Collection<OrganizationType> organizationTypes,
            List<LocalDate> dueDates
    );

    List<Organization> findAllByDeletedFalseAndOrganizationTypeAndStatusAndSubscriptionDueDateBefore(
            OrganizationType organizationType,
            OrganizationStatus status,
            LocalDate date
    );

    List<Organization> findAllByDeletedFalseAndOrganizationTypeInAndStatusAndSubscriptionDueDateBefore(
            Collection<OrganizationType> organizationTypes,
            OrganizationStatus status,
            LocalDate date
    );

    long countByDeletedFalseAndOrganizationTypeAndStatusInAndSubscriptionDueDateBefore(
            OrganizationType organizationType,
            Collection<OrganizationStatus> statuses,
            LocalDate date
    );

    long countByDeletedFalseAndOrganizationTypeAndStatusInAndSubscriptionDueDateBetween(
            OrganizationType organizationType,
            Collection<OrganizationStatus> statuses,
            LocalDate startDate,
            LocalDate endDate
    );

    boolean existsByNameAndDeletedFalse(String name);

    boolean existsByAdminEmailAndDeletedFalse(String adminEmail);

    boolean existsByAdminEmailAndDeletedFalseAndIdNot(
            String adminEmail,
            Long id
    );

    boolean existsByAdminPhoneNumberAndDeletedFalse(
            String adminPhoneNumber
    );

    boolean existsByAdminPhoneNumberAndDeletedFalseAndIdNot(
            String adminPhoneNumber,
            Long id
    );
}
