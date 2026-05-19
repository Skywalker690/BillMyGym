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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberMembershipRepository extends JpaRepository<MemberMembership, Long> {

    @EntityGraph(attributePaths = {"member", "organization"})
    List<MemberMembership> findByOrganizationIdAndDeletedFalse(Long organizationId);

    @EntityGraph(attributePaths = {"member", "organization"})
    List<MemberMembership> findByMemberIdAndOrganizationIdAndDeletedFalseOrderByStartDateDescCreatedAtDesc(Long memberId, Long organizationId);

    @EntityGraph(attributePaths = {"member", "organization"})
    Optional<MemberMembership> findByIdAndOrganizationIdAndDeletedFalse(Long id, Long organizationId);

    @EntityGraph(attributePaths = {"member", "organization"})
    List<MemberMembership> findByOrganizationIdAndMemberIdInAndDeletedFalseOrderByMemberIdAscStartDateDescCreatedAtDesc(Long organizationId, Collection<Long> memberIds);

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
          and membership.organization.status = 'ACTIVE'
          and membership.organization.id = :organizationId
          and membership.status in ('EXPIRED', 'OVERDUE')
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
          and membership.organization.status = 'ACTIVE'
          and membership.organization.id = :organizationId
          and membership.status in ('EXPIRED', 'OVERDUE')
          and (membership.lastSentAt is null or membership.lastSentAt < :dayStart)
        order by membership.organization.id, membership.endDate asc
        """)
    List<MemberMembership> findReminderCandidatesForDay(
        @Param("organizationId") Long organizationId,
        @Param("dayStart") java.time.OffsetDateTime dayStart
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
          and membership.endDate between :today and :expiringThrough
        order by membership.endDate asc
        """)
    List<MemberMembership> findExpiringSoonMemberships(
        @Param("organizationId") Long organizationId,
        @Param("today") java.time.LocalDate today,
        @Param("expiringThrough") java.time.LocalDate expiringThrough,
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
          and membership.status in ('EXPIRED', 'OVERDUE')
        order by membership.endDate asc
        """)
    List<MemberMembership> findOverdueOrExpiredMemberships(@Param("organizationId") Long organizationId, Pageable pageable);

    long countByOrganizationIdAndDeletedFalseAndStatus(Long organizationId, MembershipStatus status);

    @Query("""
        select count(membership)
        from MemberMembership membership
        where membership.deleted = false
          and membership.organization.id = :organizationId
          and membership.status = 'ACTIVE'
          and membership.endDate between :today and :expiringThrough
        """)
    long countExpiringSoon(
        @Param("organizationId") Long organizationId,
        @Param("today") java.time.LocalDate today,
        @Param("expiringThrough") java.time.LocalDate expiringThrough
    );
}
