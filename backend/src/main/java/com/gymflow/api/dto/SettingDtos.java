package com.gymflow.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SettingDtos {

    public record SettingsResponse(
        boolean whatsappEnabled,
        boolean emailEnabled,
        int reminderDaysBeforeExpiry,
        String brandingName
    ) {
    }

    public record UpdateSettingsRequest(
        Boolean whatsappEnabled,
        Boolean emailEnabled,
        @Min(0) @Max(30) Integer reminderDaysBeforeExpiry,
        String brandingName
    ) {
    }
}
