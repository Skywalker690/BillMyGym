package com.gymflow.api.dto;

import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class OrganizationDtos {

    public record OrganizationResponse(
        Long id,
        String name,
        String district,
        String place,
        String logoUrl,
        String mapLocationLink,
        String adminFullName,
        String adminPhoneNumber,
        String adminEmail,
        String description,
        BigDecimal monthlyMembershipFee,
        BigDecimal admissionFee,
        OrganizationType organizationType,
        SubscriptionPlan subscriptionPlan,
        OrganizationStatus status,
        LocalDate trialStartDate,
        LocalDate trialEndDate,
        LocalDate subscriptionDueDate,
        OffsetDateTime pausedAt,
        OffsetDateTime deactivatedAt,
        Long activeMembers,
        Integer includedActiveMembers,
        Long overageMembers,
        BigDecimal overageRate,
        BigDecimal overageAmount,
        BigDecimal estimatedMonthlyBill
    ) {
    }

    public record OrganizationDirectoryResponse(
        List<OrganizationResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        OrganizationDirectorySummary summary
    ) {
    }

    public record OrganizationDirectorySummary(
        long totalOrganizations,
        long totalActiveOrganizations,
        long totalRenewalPendingOrganizations,
        long totalPilotOrganizations,
        long totalTrialOrganizations,
        List<OrganizationPlanSummary> subscriptionDistribution
    ) {
    }

    public record OrganizationPlanSummary(
        String key,
        String label,
        long count,
        int percentage
    ) {
    }

    public record UpdateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String district,
        String place,
        String logoUrl,
        String mapLocationLink,
        @NotBlank String adminFullName,
        @NotBlank String adminPhoneNumber,
        @Email @NotBlank String adminEmail,
        String description,
        @PositiveOrZero BigDecimal monthlyMembershipFee,
        @PositiveOrZero BigDecimal admissionFee,
        OrganizationType organizationType,
        SubscriptionPlan subscriptionPlan,
        OrganizationStatus status
    ) {
    }

    public record ReplaceOrganizationAdminRequest(
        @NotBlank String adminFullName,
        @NotBlank String adminPhoneNumber,
        @Email @NotBlank String adminEmail,
        @NotBlank String password
    ) {
    }
}
