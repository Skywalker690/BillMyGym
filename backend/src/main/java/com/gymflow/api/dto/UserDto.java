package com.gymflow.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String googleId;
    private String avatarUrl;
    private Long clientCrmId;
    private Instant createdAt;
    private Boolean termsAccepted;
    private Boolean privacyAccepted;
    private String policyVersion;
    private Instant policyAcceptedAt;
    private String consentMessage;
    private Boolean consentRequired;

}
