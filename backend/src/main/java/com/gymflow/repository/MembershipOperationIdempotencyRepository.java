package com.gymflow.repository;

import com.gymflow.domain.MembershipOperationIdempotency;
import com.gymflow.domain.enums.MembershipOperationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipOperationIdempotencyRepository extends JpaRepository<MembershipOperationIdempotency, Long> {

    Optional<MembershipOperationIdempotency> findByOrganizationIdAndMemberIdAndOperationTypeAndIdempotencyKeyAndDeletedFalse(
        Long organizationId,
        Long memberId,
        MembershipOperationType operationType,
        String idempotencyKey
    );

    @Modifying
    @Query("delete from MembershipOperationIdempotency entry where entry.organizationId = :organizationId")
    int deleteAllByOrganizationId(@Param("organizationId") Long organizationId);
}
