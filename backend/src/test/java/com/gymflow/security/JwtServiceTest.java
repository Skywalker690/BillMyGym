package com.gymflow.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gymflow.domain.AppUser;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.Role;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void shouldIssueAndParseTokenWithRoleAndOrgContext() throws Exception {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
        properties.setAccessTokenExpirationMinutes(30);
        JwtService jwtService = new JwtService(properties);

        Organization organization = new Organization();
        setEntityId(organization, 42L);

        AppUser user = new AppUser();
        setEntityId(user, 101L);
        user.setEmail("org.admin@example.com");
        user.setRole(Role.ORG_ADMIN);
        user.setOrganization(organization);

        String token = jwtService.issueToken(user);
        AuthenticatedUser parsed = jwtService.parseToken(token);

        assertEquals(101L, parsed.userId());
        assertEquals(42L, parsed.organizationId());
        assertEquals(Role.ORG_ADMIN, parsed.role());
        assertEquals("org.admin@example.com", parsed.email());
    }

    private static void setEntityId(Object entity, Long id) throws Exception {
        Class<?> current = entity.getClass();
        while (current != null) {
            try {
                Field f = current.getDeclaredField("id");
                f.setAccessible(true);
                f.set(entity, id);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalStateException("id field not found");
    }
}
