package com.gymflow.api.dto;

import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class SubscriptionDtos {

    public record CreateSubscriptionOrderRequest(
        SubscriptionPlan subscriptionPlan
    ) {
    }

    public record CreateSubscriptionOrderResponse(
        String razorpayOrderId,
        String keyId,
        boolean paymentsEnabled,
        SubscriptionPlan subscriptionPlan,
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
        SubscriptionPlan subscriptionPlan,
        BigDecimal monthlyAmount,
        LocalDate trialEndsAt,
        LocalDate pilotEndsAt,
        LocalDate subscriptionDueDate,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        MemberLimitResponse memberLimit,
        PlanAccessResponse planAccess,
        List<SubscriptionPaymentResponse> paymentHistory,
        PaymentHistoryPageResponse paymentHistoryPage
    ) {
    }

    public record MemberLimitResponse(
        SubscriptionPlan plan,
        long usedMembers,
        long deactivatedMembers,
        Integer memberLimit,
        boolean unlimited,
        long remainingSlots,
        int usagePercent
    ) {
    }

    public record PlanAccessResponse(
        boolean canCreateMembers,
        boolean canViewAdvancedAnalytics,
        boolean canReceiveMonthlyReports,
        boolean canExportReports,
        boolean canUsePremiumFeatures
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

    public record PaymentHistoryPageResponse(
        List<SubscriptionPaymentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
    ) {
    }
}
