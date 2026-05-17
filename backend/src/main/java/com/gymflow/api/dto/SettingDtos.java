package com.gymflow.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class SettingDtos {

    public record SettingsResponse(
        boolean whatsappEnabled,
        boolean emailEnabled,
        int reminderDaysBeforeExpiry,
        String brandingName,
        BigDecimal monthlyMembershipFee
    ) {
    }

    public record UpdateSettingsRequest(
        Boolean whatsappEnabled,
        Boolean emailEnabled,
        @Min(0) @Max(30) Integer reminderDaysBeforeExpiry,
        String brandingName,
        @PositiveOrZero BigDecimal monthlyMembershipFee
    ) {
    }
}
