package com.gymflow.api.dto;

import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.MemberCreditLedgerReason;
import com.gymflow.domain.enums.MemberCreditLedgerTransactionType;
import com.gymflow.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class MembershipDtos {

    public record CreateMembershipRequest(
        @Positive Integer durationMonths,
        Long membershipPlanId,
        LocalDate startDate
    ) {
    }

    public record SwitchMembershipRequest(
        @Positive Integer durationMonths,
        @NotNull Long membershipPlanId,
        @NotNull LocalDate switchDate,
        String notes
    ) {
    }

    public record CreditLedgerAdjustmentRequest(
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotNull MemberCreditLedgerReason reason,
        String notes
    ) {
    }

    public record PaymentCoverageSummary(
        BigDecimal planAmount,
        BigDecimal paidAmount,
        BigDecimal appliedCreditAmount,
        BigDecimal pendingAmount,
        PaymentStatus paymentStatus,
        boolean expiringSoon
    ) {
    }

    public record CreditLedgerEntryResponse(
        Long id,
        Long memberId,
        Long organizationId,
        Long referenceMembershipId,
        BigDecimal amount,
        MemberCreditLedgerTransactionType transactionType,
        MemberCreditLedgerReason reason,
        String notes,
        Long createdByUserId,
        String createdByEmail,
        OffsetDateTime createdAt
    ) {
    }

    public record MemberWalletResponse(
        Long memberId,
        Long organizationId,
        BigDecimal balance,
        List<CreditLedgerEntryResponse> entries
    ) {
    }

    public record MembershipResponse(
        Long id,
        Long memberId,
        Long organizationId,
        Long membershipPlanId,
        String planName,
        BigDecimal monthlyPlanPrice,
        int durationMonths,
        BigDecimal planAmount,
        LocalDate startDate,
        LocalDate endDate,
        MembershipStatus status,
        BigDecimal walletBalance,
        OffsetDateTime lastSentAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        PaymentCoverageSummary paymentCoverage
    ) {
    }

    public record MembershipHistoryResponse(
        List<MembershipResponse> memberships
    ) {
    }
}
