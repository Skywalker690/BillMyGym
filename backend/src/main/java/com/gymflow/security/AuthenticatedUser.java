package com.gymflow.security;

import com.gymflow.domain.enums.Role;

public record AuthenticatedUser(Long userId, Long organizationId, Role role, String email) {
}
