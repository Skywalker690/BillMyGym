package com.gymflow.api.dto;

import com.gymflow.domain.enums.OrganizationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

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
        OrganizationStatus status
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
        OrganizationStatus status
    ) {
    }
}
