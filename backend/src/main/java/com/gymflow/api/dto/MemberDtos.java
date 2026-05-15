package com.gymflow.api.dto;

import com.gymflow.domain.enums.MembershipDuration;
import com.gymflow.domain.enums.MembershipStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class MemberDtos {

    public record MemberRequest(
        @NotBlank String fullName,
        @NotBlank String phoneNumber,
        String email,
        @NotNull LocalDate joiningDate,
        @NotNull MembershipDuration membershipDuration,
        @Positive Integer customDurationDays,
        String notes
    ) {
    }

    public record MemberResponse(
        Long id,
        Long organizationId,
        String fullName,
        String phoneNumber,
        String email,
        LocalDate joiningDate,
        MembershipDuration membershipDuration,
        Integer customDurationDays,
        LocalDate membershipStartDate,
        LocalDate membershipEndDate,
        MembershipStatus membershipStatus,
        String notes
    ) {
    }
}
