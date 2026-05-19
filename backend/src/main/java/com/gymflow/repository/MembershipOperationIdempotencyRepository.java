package com.gymflow.repository;

import com.gymflow.domain.MembershipOperationIdempotency;
import com.gymflow.domain.enums.MembershipOperationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipOperationIdempotencyRepository extends JpaRepository<MembershipOperationIdempotency, Long> {

    Optional<MembershipOperationIdempotency> findByOrganizationIdAndMemberIdAndOperationTypeAndIdempotencyKeyAndDeletedFalse(
        Long organizationId,
        Long memberId,
        MembershipOperationType operationType,
        String idempotencyKey
    );
}
