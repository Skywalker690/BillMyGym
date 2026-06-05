package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.gymflow.api.dto.UserDto;
import com.gymflow.api.mapper.OrganizationMapper;
import com.gymflow.domain.Organization;
import com.gymflow.domain.User;
import com.gymflow.domain.enums.Role;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSettingRepository;
import com.gymflow.security.JwtService;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationSettingRepository settingRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OrganizationMapper organizationMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private OtpService otpService;
    @Mock
    private EmailService emailService;
    @Mock
    private BillingLifecycleService billingLifecycleService;
    @Mock
    private WhatsAppDeliveryService whatsAppDeliveryService;
    @Mock
    private ReferralService referralService;
    @Mock
    private PlatformSettingService platformSettingService;
    @Mock
    private LegalPolicyService legalPolicyService;

    @InjectMocks
    private AuthService authService;

    @Test
    void getCurrentUserIncludesOrganizationIdForOrgAdmin() throws Exception {
        Organization organization = new Organization();
        setEntityId(organization, 77L);

        User user = new User();
        setEntityId(user, 11L);
        user.setEmail("org.admin@example.com");
        user.setRole(Role.ORG_ADMIN);
        user.setOrganization(organization);
        user.setTermsAccepted(true);
        user.setPrivacyAccepted(true);

        when(userRepository.findByEmailAndDeletedFalse("org.admin@example.com")).thenReturn(Optional.of(user));
        when(legalPolicyService.requiresConsent(user)).thenReturn(false);
        when(legalPolicyService.consentMessage(user)).thenReturn(null);

        UserDto response = authService.getCurrentUser("org.admin@example.com");

        assertEquals(11L, response.getId());
        assertEquals("org.admin@example.com", response.getEmail());
        assertEquals("ORG_ADMIN", response.getRole());
        assertEquals(77L, response.getOrganizationId());
        assertTrue(response.getTermsAccepted());
        assertTrue(response.getPrivacyAccepted());
        assertFalse(response.getConsentRequired());
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
