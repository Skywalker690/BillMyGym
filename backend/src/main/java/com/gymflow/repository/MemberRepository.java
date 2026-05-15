package com.gymflow.repository;

import com.gymflow.domain.Member;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByOrganizationIdAndDeletedFalse(Long organizationId);

    Optional<Member> findByIdAndOrganizationIdAndDeletedFalse(Long id, Long organizationId);

    List<Member> findByMembershipEndDateLessThanEqualAndDeletedFalse(LocalDate membershipEndDate);

    long countByOrganizationIdAndDeletedFalse(Long organizationId);

    @Query("""
        select member
        from Member member
        where member.deleted = false
          and member.membershipEndDate <= :thresholdDate
          and (:organizationId is null or member.organization.id = :organizationId)
        """)
    List<Member> findReminderCandidates(@Param("thresholdDate") LocalDate thresholdDate, @Param("organizationId") Long organizationId);
}
