package com.gymflow.api.dto;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class SettingDtos {

    public record SettingsResponse(
        boolean whatsappEnabled,
        boolean emailEnabled,
        String brandingName,
        BigDecimal monthlyMembershipFee,
        BigDecimal admissionFee
    ) {
    }

    public record UpdateSettingsRequest(
        Boolean whatsappEnabled,
        Boolean emailEnabled,
        String brandingName,
        @PositiveOrZero BigDecimal monthlyMembershipFee,
        @PositiveOrZero BigDecimal admissionFee
    ) {
    }
}
