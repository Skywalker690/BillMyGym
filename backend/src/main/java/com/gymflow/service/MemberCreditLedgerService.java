package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.CreditLedgerAdjustmentRequest;
import com.gymflow.api.dto.MembershipDtos.CreditLedgerEntryResponse;
import com.gymflow.api.dto.MembershipDtos.MemberWalletResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberCreditLedgerEntry;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.MemberCreditLedgerTransactionType;
import com.gymflow.domain.enums.MembershipOperationType;
import com.gymflow.domain.enums.IdempotencyResourceType;
import com.gymflow.repository.MemberCreditLedgerRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import com.gymflow.shared.idempotency.IdempotencyService;
import com.gymflow.shared.money.MoneyUtils;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCreditLedgerService {

    private final MemberCreditLedgerRepository memberCreditLedgerRepository;
    private final MemberRepository memberRepository;
    private final IdempotencyService idempotencyService;
    private final ReportCacheService reportCacheService;

    public BigDecimal balanceForMember(Long memberId, Long organizationId) {
        return scale(memberCreditLedgerRepository.balanceForMember(memberId, organizationId));
    }

    public BigDecimal appliedCreditForMembership(Long membershipId) {
        return scale(memberCreditLedgerRepository.appliedCreditForMembership(membershipId));
    }

    public Map<Long, BigDecimal> balancesForMembers(Long organizationId, Collection<Long> memberIds) {
        return memberCreditLedgerRepository.balancesForMembers(organizationId, memberIds).stream()
            .collect(Collectors.toMap(
                MemberCreditLedgerRepository.MemberAmountProjection::getMemberId,
                projection -> scale(projection.getAmount())
            ));
    }

    public Map<Long, BigDecimal> appliedCreditForMemberships(Collection<Long> membershipIds) {
        return memberCreditLedgerRepository.amountsForMemberships(membershipIds, MemberCreditLedgerTransactionType.DEBIT).stream()
            .collect(Collectors.toMap(
                MemberCreditLedgerRepository.MembershipAmountProjection::getMembershipId,
                projection -> scale(projection.getAmount())
            ));
    }

    @Transactional(readOnly = true)
    public MemberWalletResponse wallet(Long memberId, Long organizationId) {
        memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, organizationId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        List<CreditLedgerEntryResponse> entries = memberCreditLedgerRepository
            .findByMemberIdAndOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(memberId, organizationId)
            .stream()
            .map(this::toResponse)
            .toList();
        return new MemberWalletResponse(memberId, organizationId, balanceForMember(memberId, organizationId), entries);
    }

    @Transactional
    public MemberWalletResponse manualCredit(Long memberId, Long organizationId, CreditLedgerAdjustmentRequest request, String idempotencyKey) {
        return manualAdjustment(memberId, organizationId, request, idempotencyKey, MemberCreditLedgerTransactionType.CREDIT);
    }

    @Transactional
    public MemberWalletResponse manualDebit(Long memberId, Long organizationId, CreditLedgerAdjustmentRequest request, String idempotencyKey) {
        return manualAdjustment(memberId, organizationId, request, idempotencyKey, MemberCreditLedgerTransactionType.DEBIT);
    }

    @Transactional
    public MemberCreditLedgerEntry createEntry(
        Member member,
        Organization organization,
        MemberMembership referenceMembership,
        BigDecimal amount,
        MemberCreditLedgerTransactionType transactionType,
        com.gymflow.domain.enums.MemberCreditLedgerReason reason,
        String notes
    ) {
        BigDecimal normalizedAmount = scale(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Ledger amount cannot be negative");
        }
        if (transactionType == MemberCreditLedgerTransactionType.DEBIT) {
            BigDecimal available = balanceForMember(member.getId(), organization.getId());
            if (available.compareTo(normalizedAmount) < 0) {
                throw new BadRequestException("Insufficient wallet balance");
            }
        }
        AuthenticatedUser actor = SecurityUtils.currentUserOrSystem();
        MemberCreditLedgerEntry entry = new MemberCreditLedgerEntry();
        entry.setMember(member);
        entry.setOrganization(organization);
        entry.setReferenceMembership(referenceMembership);
        entry.setAmount(normalizedAmount);
        entry.setTransactionType(transactionType);
        entry.setReason(reason);
        entry.setNotes(notes);
        entry.setCreatedByUserId(actor.userId());
        entry.setCreatedByEmail(actor.email());
        return memberCreditLedgerRepository.save(entry);
    }

    public CreditLedgerEntryResponse toResponse(MemberCreditLedgerEntry entry) {
        return new CreditLedgerEntryResponse(
            entry.getId(),
            entry.getMember().getId(),
            entry.getOrganization().getId(),
            entry.getReferenceMembership() == null ? null : entry.getReferenceMembership().getId(),
            entry.getAmount(),
            entry.getTransactionType(),
            entry.getReason(),
            entry.getNotes(),
            entry.getCreatedByUserId(),
            entry.getCreatedByEmail(),
            entry.getCreatedAt()
        );
    }

    private MemberWalletResponse manualAdjustment(
        Long memberId,
        Long organizationId,
        CreditLedgerAdjustmentRequest request,
        String idempotencyKey,
        MemberCreditLedgerTransactionType transactionType
    ) {
        String requiredKey = idempotencyService.requireKey(idempotencyKey);
        MembershipOperationType operationType = transactionType == MemberCreditLedgerTransactionType.CREDIT
            ? MembershipOperationType.CREDIT_ADJUSTMENT
            : MembershipOperationType.DEBIT_ADJUSTMENT;
        var existing = idempotencyService.findExisting(organizationId, memberId, operationType, requiredKey).orElse(null);
        if (existing != null) {
            return wallet(memberId, organizationId);
        }
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalseForUpdate(memberId, organizationId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        MemberCreditLedgerEntry entry = createEntry(
            member,
            member.getOrganization(),
            null,
            request.amount(),
            transactionType,
            request.reason(),
            request.notes()
        );
        idempotencyService.record(
            organizationId,
            memberId,
            operationType,
            requiredKey,
            IdempotencyResourceType.CREDIT_LEDGER_ENTRY,
            entry.getId()
        );
        reportCacheService.evictOrganization(organizationId);
        return wallet(memberId, organizationId);
    }

    private BigDecimal scale(BigDecimal amount) {
        return MoneyUtils.normalize(amount);
    }
}
