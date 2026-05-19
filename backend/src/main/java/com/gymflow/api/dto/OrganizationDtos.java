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

public class OrganizationDtos {

    public record OrganizationResponse(
        Long id,
        String name,
        String district,
        String logoUrl,
        String mapLocationLink,
        String address,
        String adminFullName,
        String adminPhoneNumber,
        String adminEmail,
        String description,
        BigDecimal monthlyMembershipFee,
        OrganizationType organizationType,
        SubscriptionPlan subscriptionPlan,
        OrganizationStatus status,
        LocalDate trialStartDate,
        LocalDate trialEndDate,
        LocalDate pilotStartDate,
        LocalDate pilotEndDate,
        LocalDate subscriptionDueDate,
        OffsetDateTime pausedAt,
        OffsetDateTime deactivatedAt
    ) {
    }

    public record UpdateOrganizationRequest(
        @NotBlank String name,
        @NotBlank String district,
        String logoUrl,
        String mapLocationLink,
        String address,
        @NotBlank String adminFullName,
        @NotBlank String adminPhoneNumber,
        @Email @NotBlank String adminEmail,
        String description,
        @PositiveOrZero BigDecimal monthlyMembershipFee,
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
