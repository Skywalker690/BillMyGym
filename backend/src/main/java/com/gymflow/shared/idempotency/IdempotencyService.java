package com.gymflow.shared.idempotency;

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
public class IdempotencyService {

    private static final long ORGANIZATION_SCOPED_MEMBER_ID = 0L;

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
            normalizeMemberId(memberId),
            operationType,
            idempotencyKey
        );
    }

    public Optional<MembershipOperationIdempotency> findExistingForOrganization(
        Long organizationId,
        MembershipOperationType operationType,
        String idempotencyKey
    ) {
        return findExisting(organizationId, ORGANIZATION_SCOPED_MEMBER_ID, operationType, idempotencyKey);
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
        record.setMemberId(normalizeMemberId(memberId));
        record.setOperationType(operationType);
        record.setIdempotencyKey(requireKey(idempotencyKey));
        record.setResourceType(resourceType);
        record.setResourceId(resourceId);
        return repository.save(record);
    }

    @Transactional
    public MembershipOperationIdempotency recordForOrganization(
        Long organizationId,
        MembershipOperationType operationType,
        String idempotencyKey,
        IdempotencyResourceType resourceType,
        Long resourceId
    ) {
        return record(
            organizationId,
            ORGANIZATION_SCOPED_MEMBER_ID,
            operationType,
            idempotencyKey,
            resourceType,
            resourceId
        );
    }

    private Long normalizeMemberId(Long memberId) {
        return memberId == null ? ORGANIZATION_SCOPED_MEMBER_ID : memberId;
    }
}
