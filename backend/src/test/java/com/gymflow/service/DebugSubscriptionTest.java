package com.gymflow.service;

import com.gymflow.domain.enums.Role;
import com.gymflow.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

@SpringBootTest
public class DebugSubscriptionTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Test
    public void testStatusForOrg4() {
        try {
            AuthenticatedUser user = new AuthenticatedUser(10L, "owner08@billmygym.local", Role.ORG_ADMIN, 8L);
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())
            );
            var status = subscriptionService.statusForOrganization(8L);
            System.out.println("Status for org 8: " + status.organizationStatus());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
