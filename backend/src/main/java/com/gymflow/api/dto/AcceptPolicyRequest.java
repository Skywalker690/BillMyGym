package com.gymflow.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcceptPolicyRequest {
    @NotNull(message = "Policy acceptance flag is required")
    private Boolean accepted;

    @NotBlank(message = "Policy version is required")
    private String version;
}
