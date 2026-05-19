package com.gymflow.service;

import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.MembershipOperationIdempotency;
import com.gymflow.domain.enums.IdempotencyResourceType;
import com.gymflow.domain.enums.MembershipOperationType;
import com.gymflow.repository.MembershipOperationIdempotencyRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipOperationIdempotencyService {

    private final MembershipOperationIdempotencyRepository repository;

    public String requireKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("X-Idempotency-Key header is required");
        }
        return idempotencyKey.trim();
    }

    public Optional<MembershipOperationIdempotency> findExisting(
        Long organizationId,
        Long memberId,
        MembershipOperationType operationType,
        String idempotencyKey
    ) {
        return repository.findByOrganizationIdAndMemberIdAndOperationTypeAndIdempotencyKeyAndDeletedFalse(
            organizationId,
            memberId,
            operationType,
            idempotencyKey
        );
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
        MembershipOperationIdempotency record = new MembershipOperationIdempotency();
        record.setOrganizationId(organizationId);
        record.setMemberId(memberId);
        record.setOperationType(operationType);
        record.setIdempotencyKey(requireKey(idempotencyKey));
        record.setResourceType(resourceType);
        record.setResourceId(resourceId);
        return repository.save(record);
    }
}
