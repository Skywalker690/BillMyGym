package com.gymflow.repository;

import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.enums.MembershipStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberMembershipRepository extends JpaRepository<MemberMembership, Long> {

    interface OrganizationActiveMemberCountProjection {
        Long getOrganizationId();
        Long getActiveMembers();
    }

    @EntityGraph(attributePaths = {"member", "organization"})
    List<MemberMembership> findByOrganizationIdAndDeletedFalse(Long organizationId);

    @EntityGraph(attributePaths = {"member", "organization"})
    List<MemberMembership> findByMemberIdAndOrganizationIdAndDeletedFalseOrderByStartDateDescCreatedAtDesc(Long memberId, Long organizationId);

    @EntityGraph(attributePaths = {"member", "organization"})
    Optional<MemberMembership> findTopByMemberIdAndOrganizationIdAndDeletedFalseOrderByEndDateDescStartDateDescCreatedAtDesc(
        Long memberId,
        Long organizationId
    );

    @EntityGraph(attributePaths = {"member", "organization"})
    Optional<MemberMembership> findByIdAndOrganizationIdAndDeletedFalse(Long id, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"member", "organization"})
    @Query("""
        select membership
        from MemberMembership membership
        where membership.id = :id
          and membership.organization.id = :organizationId
          and membership.deleted = false
        """)
    Optional<MemberMembership> findByIdAndOrganizationIdAndDeletedFalseForUpdate(
        @Param("id") Long id,
        @Param("organizationId") Long organizationId
    );

    @EntityGraph(attributePaths = {"member", "organization"})
    List<MemberMembership> findByOrganizationIdAndMemberIdInAndDeletedFalseOrderByMemberIdAscStartDateDescCreatedAtDesc(Long organizationId, Collection<Long> memberIds);

    @EntityGraph(attributePaths = {"member", "organization"})
    @Query("""
        select membership
        from MemberMembership membership
        where membership.deleted = false
          and membership.organization.id = :organizationId
          and membership.startDate <= :snapshotThrough
        """)
    List<MemberMembership> findReportSnapshotMemberships(
        @Param("organizationId") Long organizationId,
        @Param("snapshotThrough") java.time.LocalDate snapshotThrough
    );

    @EntityGraph(attributePaths = {"member", "organization"})
    Optional<MemberMembership> findByMemberIdAndOrganizationIdAndStatusAndDeletedFalse(Long memberId, Long organizationId, MembershipStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"member", "organization"})
    @Query("""
        select membership
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.id = :memberId
          and membership.organization.id = :organizationId
          and membership.status = 'ACTIVE'
          and membership.endDate >= CURRENT_DATE
        """)
    Optional<MemberMembership> findActiveMembershipForUpdate(
        @Param("memberId") Long memberId,
        @Param("organizationId") Long organizationId
    );

    @Query("""
        select membership
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.organization.id = :organizationId
          and membership.member.id = :memberId
          and membership.status in :statuses
        order by membership.startDate desc, membership.createdAt desc
        """)
    List<MemberMembership> findActiveMembershipsForMember(
        @Param("organizationId") Long organizationId,
        @Param("memberId") Long memberId,
        @Param("statuses") Collection<MembershipStatus> statuses
    );

    @EntityGraph(attributePaths = {"member", "organization"})
    @Query("""
        select membership
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.status in ('ACTIVE', 'TRIAL')
          and membership.organization.id = :organizationId
          and membership.status = 'OVERDUE'
          and not exists (
              select newer.id
              from MemberMembership newer
              where newer.deleted = false
                and newer.member = membership.member
                and (
                    newer.endDate > membership.endDate
                    or (newer.endDate = membership.endDate and newer.createdAt > membership.createdAt)
                )
          )
        order by membership.organization.id, membership.endDate asc
        """)
    List<MemberMembership> findReminderCandidates(@Param("organizationId") Long organizationId);

    @EntityGraph(attributePaths = {"member", "organization"})
    @Query("""
        select membership
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.deleted = false
          and membership.organization.status in ('ACTIVE', 'TRIAL')
          and membership.organization.id = :organizationId
          and (
              membership.status = 'OVERDUE'
              or (
                  membership.status = 'ACTIVE'
                  and membership.endDate = :today
              )
          )
          and (membership.lastSentAt is null or membership.lastSentAt < :dayStart)
          and not exists (
              select newer.id
              from MemberMembership newer
              where newer.deleted = false
                and newer.member = membership.member
                and (
                    newer.endDate > membership.endDate
                    or (newer.endDate = membership.endDate and newer.createdAt > membership.createdAt)
                )
          )
        order by membership.organization.id, membership.endDate asc
        """)
    List<MemberMembership> findReminderCandidatesForDay(
        @Param("organizationId") Long organizationId,
        @Param("dayStart") java.time.OffsetDateTime dayStart,
        @Param("today") java.time.LocalDate today
    );

    @EntityGraph(attributePaths = {"member", "organization"})
    @Query("""
        select membership
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.deleted = false
          and membership.organization.status in ('ACTIVE', 'TRIAL')
          and membership.organization.id = :organizationId
          and membership.status = 'OVERDUE'
          and membership.endDate < :today
          and (membership.lastSentAt is null or membership.lastSentAt < :dayStart)
          and not exists (
              select newer.id
              from MemberMembership newer
              where newer.deleted = false
                and newer.member = membership.member
                and (
                    newer.endDate > membership.endDate
                    or (newer.endDate = membership.endDate and newer.createdAt > membership.createdAt)
                )
          )
        order by membership.organization.id, membership.endDate asc
        """)
    List<MemberMembership> findExpiredReminderCandidatesForDay(
        @Param("organizationId") Long organizationId,
        @Param("dayStart") java.time.OffsetDateTime dayStart,
        @Param("today") java.time.LocalDate today
    );

    @EntityGraph(attributePaths = {"member", "organization"})
    @Query("""
        select membership
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.id = :organizationId
          and membership.status = 'ACTIVE'
          and membership.endDate = :today
        order by membership.endDate asc
        """)
    List<MemberMembership> findExpiringSoonMemberships(
        @Param("organizationId") Long organizationId,
        @Param("today") java.time.LocalDate today,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"member", "organization"})
    @Query("""
        select membership
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.id = :organizationId
          and membership.endDate <= :today
          and membership.status = 'OVERDUE'
          and not exists (
              select newer.id
              from MemberMembership newer
              where newer.deleted = false
                and newer.member = membership.member
                and (
                    newer.endDate > membership.endDate
                    or (newer.endDate = membership.endDate and newer.createdAt > membership.createdAt)
                )
          )
        order by membership.endDate asc
        """)
    List<MemberMembership> findOverdueOrExpiredMemberships(
        @Param("organizationId") Long organizationId,
        @Param("today") java.time.LocalDate today,
        Pageable pageable
    );

    long countByOrganizationIdAndDeletedFalseAndStatus(Long organizationId, MembershipStatus status);

    @Modifying
    @Query("delete from MemberMembership membership where membership.organization.id = :organizationId")
    int deleteAllByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
        select count(distinct membership.member.id)
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.id = :organizationId
          and membership.status = 'ACTIVE'
          and membership.startDate <= :today
          and membership.endDate >= :today
        """)
    long countBillableActiveMembers(
        @Param("organizationId") Long organizationId,
        @Param("today") java.time.LocalDate today
    );

    @Query("""
        select membership.organization.id as organizationId,
               count(distinct membership.member.id) as activeMembers
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.id in :organizationIds
          and membership.status = 'ACTIVE'
          and membership.startDate <= :today
          and membership.endDate >= :today
        group by membership.organization.id
        """)
    List<OrganizationActiveMemberCountProjection> countBillableActiveMembersByOrganizationIds(
        @Param("organizationIds") Collection<Long> organizationIds,
        @Param("today") java.time.LocalDate today
    );

    @Query("""
        select count(membership)
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.id = :organizationId
          and membership.endDate between :startDate and :endDate
        """)
    long countExpiredBetween(
        @Param("organizationId") Long organizationId,
        @Param("startDate") java.time.LocalDate startDate,
        @Param("endDate") java.time.LocalDate endDate
    );

    @Query("""
        select count(membership)
        from MemberMembership membership
        where membership.deleted = false
          and membership.organization.id = :organizationId
          and membership.status = 'ACTIVE'
          and membership.endDate = :today
        """)
    long countExpiringSoon(
        @Param("organizationId") Long organizationId,
        @Param("today") java.time.LocalDate today
    );

    @Query("""
        select count(membership)
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.id = :organizationId
          and membership.endDate = :today
        """)
    long countExpiredOn(
        @Param("organizationId") Long organizationId,
        @Param("today") java.time.LocalDate today
    );

    @Query("""
        select count(membership)
        from MemberMembership membership
        where membership.deleted = false
          and membership.member.deleted = false
          and membership.member.status = 'ACTIVE'
          and membership.organization.id = :organizationId
          and membership.endDate < :today
          and membership.status = 'OVERDUE'
        """)
    long countOverdueBefore(
        @Param("organizationId") Long organizationId,
        @Param("today") java.time.LocalDate today
    );
}
