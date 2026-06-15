package com.gymflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class MembershipPlanDtos {

    public record MembershipPlanResponse(
        Long id,
        Long organizationId,
        String name,
        BigDecimal price,
        boolean active,
        boolean defaultPlan,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {
    }

    public record MembershipPlanListResponse(
        List<MembershipPlanResponse> plans
    ) {
    }

    public record CreateMembershipPlanRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price,
        Boolean active,
        Boolean defaultPlan
    ) {
    }

    public record UpdateMembershipPlanRequest(
        String name,
        @PositiveOrZero BigDecimal price,
        Boolean active,
        Boolean defaultPlan
    ) {
    }
}
