package com.gymflow.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    public record LoginResponse(String accessToken, String role, Long organizationId, UserDto user) {
    }

    public record RegisterOrganizationAdminRequest(
        @NotBlank String organizationName,
        @NotBlank String district,
        String logoUrl,
        String mapLocationLink,
        String address,
        @NotBlank String adminFullName,
        @NotBlank String adminPhoneNumber,
        @Email @NotBlank String adminEmail,
        String organizationDescription,
        OrganizationType organizationType,
        SubscriptionPlan subscriptionPlan,
        @NotBlank @Size(min = 8) String password
    ) {
    }
}
