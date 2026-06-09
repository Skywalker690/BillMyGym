package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionPaymentRepository;
import com.gymflow.repository.UsageMetricRepository;
import com.gymflow.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;

    @Mock
    private UsageMetricRepository usageMetricRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PlanAccessService planAccessService;

    @Mock
    private MemberLimitService memberLimitService;

    @Mock
    private SubscriptionPricingService subscriptionPricingService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OrganizationPaymentWebhookService organizationPaymentWebhookService;

    @Mock
    private OrganizationLifecycleService organizationLifecycleService;

    @Mock
    private IndividualWhatsappReminderService individualWhatsappReminderService;

    private final BillingLifecycleService billingLifecycleService = new BillingLifecycleService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrderRejectsOrganizationsStillInsideTrialAccessWindow() {
        Organization organization = new Organization();
        organization.setOrganizationType(OrganizationType.REGULAR);
        organization.setSubscriptionPlan(SubscriptionPlan.BILLMYGYM);
        organization.setStatus(OrganizationStatus.TRIAL);
        organization.setTrialStartDate(LocalDate.now().minusDays(10));
        organization.setTrialEndDate(LocalDate.now().plusDays(49));
        organization.setSubscriptionDueDate(LocalDate.now().plusDays(50));
        setId(organization, 11L);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            new AuthenticatedUser(7L, "admin@example.com", Role.ORG_ADMIN, 11L),
            null,
            "ROLE_ORG_ADMIN"
        ));
        when(organizationRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(organization));

        SubscriptionService service = new SubscriptionService(
            organizationRepository,
            subscriptionPaymentRepository,
            usageMetricRepository,
            memberRepository,
            planAccessService,
            memberLimitService,
            subscriptionPricingService,
            auditLogService,
            organizationPaymentWebhookService,
            billingLifecycleService,
            organizationLifecycleService,
            individualWhatsappReminderService,
            new ObjectMapper()
        );

        assertThrows(
            BadRequestException.class,
            () -> service.createOrder(new CreateSubscriptionOrderRequest(SubscriptionPlan.BILLMYGYM, false))
        );
    }

    private void setId(Object target, Long id) {
        try {
            var field = target.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
