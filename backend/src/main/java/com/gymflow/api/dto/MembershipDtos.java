package com.gymflow.api.dto;

import com.gymflow.domain.enums.MembershipStatus;
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
        @NotBlank String planName,
        @Positive Integer durationMonths,
        @NotNull @PositiveOrZero BigDecimal planAmount,
        LocalDate startDate
    ) {
    }

    public record PaymentCoverageSummary(
        BigDecimal planAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        PaymentStatus paymentStatus,
        boolean expiringSoon
    ) {
    }

    public record MembershipResponse(
        Long id,
        Long memberId,
        Long organizationId,
        String planName,
        int durationMonths,
        BigDecimal planAmount,
        LocalDate startDate,
        LocalDate endDate,
        MembershipStatus status,
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
