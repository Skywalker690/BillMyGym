package com.gymflow.api.dto;

import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class SubscriptionDtos {

    public record CreateSubscriptionOrderResponse(
        String razorpayOrderId,
        String keyId,
        boolean paymentsEnabled,
        BigDecimal amount,
        long amountInPaise,
        String currency,
        LocalDate subscriptionStart,
        LocalDate subscriptionEnd,
        SubscriptionPaymentStatus paymentStatus
    ) {
    }

    public record VerifySubscriptionPaymentRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
    ) {
    }

    public record SubscriptionStatusResponse(
        Long organizationId,
        String organizationName,
        OrganizationType organizationType,
        OrganizationStatus organizationStatus,
        BigDecimal monthlyAmount,
        LocalDate trialEndsAt,
        LocalDate pilotEndsAt,
        LocalDate subscriptionDueDate,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        List<SubscriptionPaymentResponse> paymentHistory
    ) {
    }

    public record SubscriptionPaymentResponse(
        Long id,
        BigDecimal amount,
        LocalDate paymentDate,
        LocalDate subscriptionStart,
        LocalDate subscriptionEnd,
        SubscriptionPaymentStatus paymentStatus,
        String gatewayTransactionId,
        String razorpayOrderId,
        String razorpayPaymentId,
        OffsetDateTime createdAt
    ) {
    }
}
