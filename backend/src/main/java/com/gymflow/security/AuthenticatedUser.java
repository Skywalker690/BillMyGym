package com.gymflow.security;

import com.gymflow.domain.enums.Role;

public record AuthenticatedUser(
        Long userId,
        String email,
        Role role,
        Long organizationId
) {
}
