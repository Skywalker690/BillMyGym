package com.gymflow.service;

import com.gymflow.domain.MembershipOperationIdempotency;
import com.gymflow.domain.enums.IdempotencyResourceType;
import com.gymflow.domain.enums.MembershipOperationType;
import com.gymflow.shared.idempotency.IdempotencyService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipOperationIdempotencyService {

    private final IdempotencyService idempotencyService;

    public String requireKey(String idempotencyKey) {
        return idempotencyService.requireKey(idempotencyKey);
    }

    public Optional<MembershipOperationIdempotency> findExisting(
        Long organizationId,
        Long memberId,
        MembershipOperationType operationType,
        String idempotencyKey
    ) {
        return idempotencyService.findExisting(organizationId, memberId, operationType, idempotencyKey);
    }

    @Transactional
    public MembershipOperationIdempotency record(
        Long organizationId,
        Long memberId,
        MembershipOperationType operationType,
        String idempotencyKey,
        IdempotencyResourceType resourceType,
        Long resourceId
    ) {
        return idempotencyService.record(
            organizationId,
            memberId,
            operationType,
            idempotencyKey,
            resourceType,
            resourceId
        );
    }
}
