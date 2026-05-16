package com.gymflow.repository;

import com.gymflow.domain.MemberPayment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPaymentRepository extends JpaRepository<MemberPayment, Long> {

    @EntityGraph(attributePaths = {"member", "membership", "organization"})
    List<MemberPayment> findByOrganizationIdAndDeletedFalseOrderByPaymentDateDescCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = {"member", "membership", "organization"})
    List<MemberPayment> findByMembershipIdAndDeletedFalseOrderByPaymentDateAscCreatedAtAsc(Long membershipId);

    @EntityGraph(attributePaths = {"member", "membership", "organization"})
    List<MemberPayment> findByMembershipIdInAndDeletedFalse(Collection<Long> membershipIds);

    Optional<MemberPayment> findByGatewayTransactionIdAndDeletedFalse(String gatewayTransactionId);

    Optional<MemberPayment> findByIdAndOrganizationIdAndDeletedFalse(Long id, Long organizationId);
}
