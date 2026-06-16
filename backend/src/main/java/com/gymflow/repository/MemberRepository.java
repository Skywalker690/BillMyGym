package com.gymflow.repository;

import com.gymflow.domain.Member;
import com.gymflow.domain.enums.MemberStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @EntityGraph(attributePaths = "organization")
    List<Member> findByOrganizationIdAndDeletedFalse(Long organizationId);

    Optional<Member> findByIdAndOrganizationIdAndDeletedFalse(Long id, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "organization")
    @Query("""
        select member
        from Member member
        where member.id = :id
          and member.organization.id = :organizationId
          and member.deleted = false
        """)
    Optional<Member> findByIdAndOrganizationIdAndDeletedFalseForUpdate(@Param("id") Long id, @Param("organizationId") Long organizationId);

    long countByOrganizationIdAndDeletedFalseAndStatus(Long organizationId, MemberStatus status);

    long countByOrganizationIdAndDeletedFalseAndStatusIn(Long organizationId, Collection<MemberStatus> statuses);

    long countByOrganizationIdAndDeletedFalseAndCreatedAtBetween(
        Long organizationId,
        OffsetDateTime start,
        OffsetDateTime end
    );

    @Query(
        value = """
            select extract(year from created_at)::int as year,
                   extract(month from created_at)::int as month,
                   count(*) as total
            from members
            where organization_id = :organizationId
              and deleted = false
              and created_at >= :start
              and created_at < :end
            group by extract(year from created_at), extract(month from created_at)
            """,
        nativeQuery = true
    )
    List<MonthlyMemberJoinProjection> countJoinedMembersByMonth(
        @Param("organizationId") Long organizationId,
        @Param("start") OffsetDateTime start,
        @Param("end") OffsetDateTime end
    );

    @EntityGraph(attributePaths = "organization")
    @Query("""
    select member
    from Member member
    left join MemberMembership latestMembership on latestMembership.member = member
      and latestMembership.deleted = false
      and not exists (
          select newer.id from MemberMembership newer
          where newer.member = member and newer.deleted = false
            and (newer.endDate > latestMembership.endDate or (newer.endDate = latestMembership.endDate and newer.createdAt > latestMembership.createdAt))
      )
    where member.organization.id = :organizationId
      and member.deleted = false
      and (
          :pattern is null
          or lower(member.fullName) like :pattern
          or lower(member.phone) like :pattern
      )
      and (:memberStatus is null or member.status = :memberStatus)
      and (
          :membershipStatus is null
          or (
              :membershipStatus = 'ACTIVE'
              and latestMembership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE
              and latestMembership.endDate > :today
          )
          or (
              :membershipStatus = 'EXPIRING_SOON'
              and latestMembership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE
              and latestMembership.endDate = :today
          )
          or (
              :membershipStatus = 'OVERDUE'
              and latestMembership.status = com.gymflow.domain.enums.MembershipStatus.OVERDUE
          )
      )
    order by 
      case 
        when latestMembership.status = com.gymflow.domain.enums.MembershipStatus.OVERDUE then 1
        when latestMembership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE and latestMembership.endDate = :today then 2
        when latestMembership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE then 3
        else 4
      end asc,
      member.createdAt desc
    """)
    Page<Member> searchDirectory(
        @Param("organizationId") Long organizationId,
        @Param("pattern") String pattern,
        @Param("memberStatus") MemberStatus memberStatus,
        @Param("membershipStatus") String membershipStatus,
        @Param("today") java.time.LocalDate today,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "organization")
    @Query("""
    select member
    from Member member
    left join MemberMembership latestMembership on latestMembership.member = member
      and latestMembership.deleted = false
      and not exists (
          select newer.id from MemberMembership newer
          where newer.member = member and newer.deleted = false
            and (newer.endDate > latestMembership.endDate or (newer.endDate = latestMembership.endDate and newer.createdAt > latestMembership.createdAt))
      )
    where member.organization.id = :organizationId
      and member.deleted = false
      and (
          :pattern is null
          or lower(member.fullName) like :pattern
          or lower(member.phone) like :pattern
      )
      and member.status in :memberStatuses
      and (
          :membershipStatus is null
          or (
              :membershipStatus = 'ACTIVE'
              and latestMembership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE
              and latestMembership.endDate > :today
          )
          or (
              :membershipStatus = 'EXPIRING_SOON'
              and latestMembership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE
              and latestMembership.endDate = :today
          )
          or (
              :membershipStatus = 'OVERDUE'
              and latestMembership.status = com.gymflow.domain.enums.MembershipStatus.OVERDUE
          )
      )
    order by 
      case 
        when latestMembership.status = com.gymflow.domain.enums.MembershipStatus.OVERDUE then 1
        when latestMembership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE and latestMembership.endDate = :today then 2
        when latestMembership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE then 3
        else 4
      end asc,
      member.createdAt desc
    """)
    Page<Member> searchDirectoryByStatuses(
        @Param("organizationId") Long organizationId,
        @Param("pattern") String pattern,
        @Param("memberStatuses") Collection<MemberStatus> memberStatuses,
        @Param("membershipStatus") String membershipStatus,
        @Param("today") java.time.LocalDate today,
        Pageable pageable
    );

    long countByOrganizationIdAndDeletedFalse(Long organizationId);

    @Modifying
    @Query("delete from Member member where member.organization.id = :organizationId")
    int deleteAllByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
        select member.phone
        from Member member
        where member.organization.id = :organizationId
          and member.deleted = false
          and member.phone is not null
        """)
    List<String> findPhonesByOrganizationIdAndDeletedFalse(@Param("organizationId") Long organizationId);

    interface MonthlyMemberJoinProjection {
        Integer getYear();

        Integer getMonth();

        Long getTotal();
    }
}
