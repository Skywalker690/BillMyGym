package com.gymflow.repository;

import com.gymflow.domain.Member;
import com.gymflow.domain.enums.MemberStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
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
          :search is null
          or lower(member.fullName) like lower(concat('%', cast(:search as string), '%'))
          or lower(member.phone) like lower(concat('%', cast(:search as string), '%'))
          or (
              member.email is not null
              and lower(member.email) like lower(concat('%', cast(:search as string), '%'))
          )
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
              :membershipStatus = 'EXPIRED'
              and exists (
                  select membership.id
                  from MemberMembership membership
                  where membership.member = member
                    and membership.deleted = false
                    and membership.status = com.gymflow.domain.enums.MembershipStatus.EXPIRED
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
        @Param("search") String search,
        @Param("memberStatus") MemberStatus memberStatus,
        @Param("membershipStatus") String membershipStatus,
        @Param("today") java.time.LocalDate today,
        @Param("expiringThrough") java.time.LocalDate expiringThrough,
        Pageable pageable
    );

    long countByOrganizationIdAndDeletedFalse(Long organizationId);
}
