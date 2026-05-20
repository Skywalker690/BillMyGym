package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.api.dto.PaymentDtos.RecordPaymentRequest;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import com.gymflow.domain.enums.Role;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.shared.idempotency.IdempotencyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PaymentLedgerServiceTest {

    @Mock
    private MemberMembershipRepository memberMembershipRepository;

    @Mock
    private MemberPaymentRepository memberPaymentRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OrganizationSettingService organizationSettingService;

    @Mock
    private IdempotencyService idempotencyService;

    private final MembershipLifecycleService membershipLifecycleService = new MembershipLifecycleService();

    private PaymentLedgerService paymentLedgerService;

    @BeforeEach
    void setUp() {
        paymentLedgerService = new PaymentLedgerService(
            memberMembershipRepository,
            memberPaymentRepository,
            membershipLifecycleService,
            organizationSettingService,
            auditLogService,
            idempotencyService
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(1L, "admin@billmygym.local", Role.ORG_ADMIN, 100L),
                null,
                List.of()
            )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsManualPaymentWithoutGatewayTransactionId() {
        MemberMembership membership = membership(200L, 100L);
        when(idempotencyService.requireKey("payment-key")).thenReturn("payment-key");
        when(idempotencyService.findExisting(100L, null, com.gymflow.domain.enums.MembershipOperationType.PAYMENT_RECORD, "payment-key"))
            .thenReturn(Optional.empty());
        when(memberMembershipRepository.findByIdAndOrganizationIdAndDeletedFalseForUpdate(200L, 100L))
            .thenReturn(Optional.of(membership));
        when(memberPaymentRepository.findByMembershipIdAndDeletedFalseOrderByPaymentDateAscCreatedAtAsc(200L))
            .thenReturn(List.of());
        OrganizationSetting setting = new OrganizationSetting();
        setting.setReminderDaysBeforeExpiry(3);
        when(organizationSettingService.getOrCreateSettings(100L)).thenReturn(setting);
        when(memberPaymentRepository.save(any(MemberPayment.class))).thenAnswer(invocation -> {
            MemberPayment payment = invocation.getArgument(0);
            payment.setPaymentStatus(PaymentStatus.PARTIAL);
            return payment;
        });

        RecordPaymentRequest request = new RecordPaymentRequest(
            200L,
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 5, 16),
            PaymentType.NEW_MEMBERSHIP,
            PaymentStatus.PARTIAL,
            "manual payment"
        );

        var response = paymentLedgerService.recordPayment(request, "payment-key");

        assertEquals(PaymentStatus.PARTIAL, response.paymentStatus());
        assertEquals("2026-05", response.billingMonth());
        verify(memberPaymentRepository).save(any(MemberPayment.class));
    }

    private MemberMembership membership(Long membershipId, Long organizationId) {
        Organization organization = new Organization();
        organization.setName("Gym");

        Member member = new Member();
        member.setOrganization(organization);
        member.setStatus(MemberStatus.ACTIVE);

        MemberMembership membership = new MemberMembership();
        membership.setOrganization(organization);
        membership.setMember(member);
        membership.setPlanAmount(new BigDecimal("3600.00"));
        membership.setStartDate(LocalDate.of(2026, 5, 1));
        membership.setEndDate(LocalDate.of(2026, 8, 1));
        membership.setStatus(com.gymflow.domain.enums.MembershipStatus.ACTIVE);

        setId(organization, organizationId);
        setId(member, 300L);
        setId(membership, membershipId);
        return membership;
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

