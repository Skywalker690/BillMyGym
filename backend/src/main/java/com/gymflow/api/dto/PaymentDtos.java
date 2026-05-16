package com.gymflow.api.dto;

import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentDtos {

    public record RecordPaymentRequest(
        @NotNull Long membershipId,
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotNull PaymentType paymentType,
        PaymentStatus paymentStatus,
        String gatewayTransactionId,
        String notes
    ) {
    }

    public record PaymentResponse(
        Long id,
        Long memberId,
        Long membershipId,
        Long organizationId,
        BigDecimal amount,
        LocalDate paymentDate,
        String billingMonth,
        PaymentStatus paymentStatus,
        PaymentType paymentType,
        String gatewayTransactionId,
        String notes,
        MembershipDtos.PaymentCoverageSummary paymentCoverage
    ) {
    }
}
