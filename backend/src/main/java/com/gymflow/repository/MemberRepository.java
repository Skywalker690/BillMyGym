package com.gymflow.repository;

import com.gymflow.domain.Member;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByOrganizationIdAndDeletedFalse(Long organizationId);

    Optional<Member> findByIdAndOrganizationIdAndDeletedFalse(Long id, Long organizationId);

    List<Member> findByMembershipEndDateLessThanEqualAndDeletedFalse(LocalDate membershipEndDate);

    long countByOrganizationIdAndDeletedFalse(Long organizationId);
}
