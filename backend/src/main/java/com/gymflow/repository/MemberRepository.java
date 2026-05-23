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

    Optional<Member> findByOrganizationIdAndPhoneAndDeletedFalse(Long organizationId, String phone);

    long countByOrganizationIdAndDeletedFalseAndStatus(Long organizationId, MemberStatus status);

    long countByOrganizationIdAndDeletedFalseAndStatusIn(Long organizationId, Collection<MemberStatus> statuses);

    long countByOrganizationIdAndDeletedFalseAndCreatedAtBetween(
        Long organizationId,
        OffsetDateTime start,
        OffsetDateTime end
    );

    @EntityGraph(attributePaths = "organization")
    @Query("""
    select member
    from Member member
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
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE
              )
          )
          or (
              :membershipStatus = 'EXPIRING_SOON'
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE
                    and membership.endDate between :today and :expiringThrough
              )
          )
          or (
              :membershipStatus = 'DEACTIVATED'
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.DEACTIVATED
              )
          )
          or (
              :membershipStatus = 'OVERDUE'
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.OVERDUE
              )
          )
      )
    """)
    Page<Member> searchDirectory(
        @Param("organizationId") Long organizationId,
        @Param("pattern") String pattern,
        @Param("memberStatus") MemberStatus memberStatus,
        @Param("membershipStatus") String membershipStatus,
        @Param("today") java.time.LocalDate today,
        @Param("expiringThrough") java.time.LocalDate expiringThrough,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "organization")
    @Query("""
    select member
    from Member member
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
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE
              )
          )
          or (
              :membershipStatus = 'EXPIRING_SOON'
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.ACTIVE
                    and membership.endDate between :today and :expiringThrough
              )
          )
          or (
              :membershipStatus = 'DEACTIVATED'
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.DEACTIVATED
              )
          )
          or (
              :membershipStatus = 'OVERDUE'
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.OVERDUE
              )
          )
      )
    """)
    Page<Member> searchDirectoryByStatuses(
        @Param("organizationId") Long organizationId,
        @Param("pattern") String pattern,
        @Param("memberStatuses") Collection<MemberStatus> memberStatuses,
        @Param("membershipStatus") String membershipStatus,
        @Param("today") java.time.LocalDate today,
        @Param("expiringThrough") java.time.LocalDate expiringThrough,
        Pageable pageable
    );

    long countByOrganizationIdAndDeletedFalse(Long organizationId);
}
