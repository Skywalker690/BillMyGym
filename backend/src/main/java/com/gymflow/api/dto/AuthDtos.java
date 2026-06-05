package com.gymflow.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;

public class AuthDtos {

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {
    }

    public record GoogleLoginRequest(@NotBlank String idToken) {
    }

    public record LoginResponse(String accessToken, String role, Long organizationId, UserDto user, boolean referralsEnabled) {
    }

    public record LegalPolicyMetadataResponse(
        String applicationName,
        String currentVersion,
        LocalDate effectiveDate,
        String supportEmail,
        String legalEmail,
        String privacyEmail,
        String companyAddress
    ) {
    }

    public record RegisterOrganizationAdminRequest(
        @NotBlank String organizationName,
        @NotBlank String place,
        @NotBlank String district,
        String logoUrl,
        String mapLocationLink,
        @NotBlank String adminFullName,
        @NotBlank String adminPhoneNumber,
        @Email @NotBlank String adminEmail,
        @PositiveOrZero BigDecimal monthlyMembershipFee,
        @PositiveOrZero BigDecimal admissionFee,
        OrganizationType organizationType,
        SubscriptionPlan subscriptionPlan,
        @NotBlank @Size(min = 8) String password,
        String referralCodeUsed
    ) {
    }
}
