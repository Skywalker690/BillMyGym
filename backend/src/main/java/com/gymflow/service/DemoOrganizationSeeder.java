package com.gymflow.service;

import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.MembershipPlan;
import com.gymflow.domain.NotificationDeliveryLog;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.WhatsappReminderLog;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationDeliveryStatus;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import com.gymflow.domain.enums.WhatsappReminderStatus;
import com.gymflow.repository.MemberCreditLedgerRepository;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.MembershipOperationIdempotencyRepository;
import com.gymflow.repository.NotificationDeliveryLogRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.UsageMetricRepository;
import com.gymflow.repository.WhatsappReminderLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DemoOrganizationSeeder {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal MONTHLY_FEE = new BigDecimal("800.00");
    private static final BigDecimal ADMISSION_FEE = new BigDecimal("300.00");

    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MemberCreditLedgerRepository memberCreditLedgerRepository;
    private final MembershipOperationIdempotencyRepository membershipOperationIdempotencyRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;
    private final WhatsappReminderLogRepository whatsappReminderLogRepository;
    private final UsageMetricRepository usageMetricRepository;
    private final ReportCacheService reportCacheService;
    private final MembershipPlanService membershipPlanService;

    @Transactional
    public DemoResetResult resetDemoWorkspace() {
        Organization organization = organizationRepository
            .findFirstByDeletedFalseAndOrganizationTypeOrderByCreatedAtAsc(OrganizationType.DEMO)
            .orElseThrow(() -> new NotFoundException("Demo organization not found"));

        if (organization.getOrganizationType() != OrganizationType.DEMO) {
            throw new BadRequestException("Only demo organizations can be reset");
        }

        clearDemoData(organization.getId());
        seedDemoData(organization);
        reportCacheService.evictOrganization(organization.getId());
        return new DemoResetResult(organization.getId(), "Demo workspace reset successfully");
    }

    private void clearDemoData(Long organizationId) {
        notificationDeliveryLogRepository.deleteAllByOrganizationId(organizationId);
        whatsappReminderLogRepository.deleteAllByOrganizationId(organizationId);
        notificationLogRepository.deleteAllByOrganizationId(organizationId);
        membershipOperationIdempotencyRepository.deleteAllByOrganizationId(organizationId);
        memberCreditLedgerRepository.deleteAllByOrganizationId(organizationId);
        memberPaymentRepository.deleteAllByOrganizationId(organizationId);
        memberMembershipRepository.deleteAllByOrganizationId(organizationId);
        memberRepository.deleteAllByOrganizationId(organizationId);
        usageMetricRepository.deleteAllByOrganizationId(organizationId);
    }

    private void seedDemoData(Organization organization) {
        LocalDate today = LocalDate.now(APP_ZONE);
        YearMonth currentMonth = YearMonth.from(today);

        organization.setOrganizationType(OrganizationType.DEMO);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setMonthlyMembershipFee(MONTHLY_FEE);
        organization.setAdmissionFee(ADMISSION_FEE);
        organization.setTrialStartDate(today.minusMonths(1));
        organization.setTrialEndDate(today.plusMonths(1));
        organization.setSubscriptionDueDate(today.plusMonths(1));
        organizationRepository.save(organization);

        MembershipPlan regularPlan = membershipPlanService.ensureRegularDefaultPlan(organization, MONTHLY_FEE);
        List<MemberMembership> memberships = new ArrayList<>();
        for (int index = 1; index <= 30; index++) {
            Member member = memberRepository.save(demoMember(organization, index));
            MemberMembership membership = memberMembershipRepository.save(demoMembership(member, organization, regularPlan, index, today));
            memberships.add(membership);
            seedPayments(organization, member, membership, index, currentMonth);
        }

        seedNotifications(organization, memberships, today);
        seedUsageMetrics(organization, currentMonth);
    }

    private Member demoMember(Organization organization, int index) {
        Member member = new Member();
        member.setOrganization(organization);
        member.setFullName("Demo Member %02d".formatted(index));
        member.setPhone("+91900000%04d".formatted(index));
        member.setStatus(MemberStatus.ACTIVE);
        return member;
    }

    private MemberMembership demoMembership(Member member, Organization organization, MembershipPlan regularPlan, int index, LocalDate today) {
        MemberMembership membership = new MemberMembership();
        membership.setMember(member);
        membership.setOrganization(organization);
        membership.setMembershipPlan(regularPlan);
        membership.setPlanName(regularPlan.getName());
        membership.setMonthlyPlanPrice(regularPlan.getPrice());
        membership.setDurationMonths(1);
        membership.setPlanAmount(MONTHLY_FEE);

        if (index <= 20) {
            membership.setStartDate(today.minusDays(10 + index));
            membership.setEndDate(today.plusDays(20 + index));
            membership.setStatus(MembershipStatus.ACTIVE);
        } else if (index <= 25) {
            membership.setStartDate(today.minusDays(30));
            membership.setEndDate(today);
            membership.setStatus(MembershipStatus.ACTIVE);
        } else if (index <= 28) {
            membership.setStartDate(today.minusDays(40));
            membership.setEndDate(today.minusDays(index - 24L));
            membership.setStatus(MembershipStatus.OVERDUE);
        } else {
            membership.setStartDate(today.minusDays(70));
            membership.setEndDate(today.minusDays(30 + index));
            membership.setStatus(MembershipStatus.OVERDUE);
        }

        return membership;
    }

    private void seedPayments(
        Organization organization,
        Member member,
        MemberMembership membership,
        int index,
        YearMonth currentMonth
    ) {
        LocalDate admissionDate = currentMonth.atDay(Math.min(index, currentMonth.lengthOfMonth()));
        memberPaymentRepository.save(demoPayment(
            organization,
            member,
            membership,
            ADMISSION_FEE,
            admissionDate,
            PaymentType.ADMISSION,
            "Demo admission fee"
        ));

        if (index <= 20) {
            memberPaymentRepository.save(demoPayment(
                organization,
                member,
                membership,
                MONTHLY_FEE,
                admissionDate,
                PaymentType.MEMBERSHIP,
                "Demo monthly membership payment"
            ));
        }
    }

    private MemberPayment demoPayment(
        Organization organization,
        Member member,
        MemberMembership membership,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentType paymentType,
        String notes
    ) {
        MemberPayment payment = new MemberPayment();
        payment.setOrganization(organization);
        payment.setMember(member);
        payment.setMembership(membership);
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setBillingMonth(YearMonth.from(paymentDate).toString());
        payment.setPaymentStatus(PaymentStatus.PAID);
        payment.setPaymentType(paymentType);
        payment.setNotes(notes);
        payment.setGatewayTransactionId("demo-%s-%d-%s".formatted(
            paymentType.name().toLowerCase(),
            membership.getMember().getId(),
            paymentDate
        ));
        return payment;
    }

    private void seedNotifications(Organization organization, List<MemberMembership> memberships, LocalDate today) {
        List<NotificationLog> notifications = List.of(
            demoNotification(organization, memberships.get(20).getMember(), NotificationChannel.WHATSAPP, NotificationType.ORGANIZATION_RENEWAL_DIGEST, "+919000000021", "5 memberships expire today.", today),
            demoNotification(organization, memberships.get(25).getMember(), NotificationChannel.WHATSAPP, NotificationType.INDIVIDUAL_WHATSAPP_REMINDER, "+919000000026", "Demo reminder: membership is overdue.", today.minusDays(1)),
            demoNotification(organization, memberships.get(28).getMember(), NotificationChannel.WHATSAPP, NotificationType.MANUAL_REMINDER_TRIGGER, "+919000000029", "Demo manual reminder sent.", today.minusDays(2)),
            demoNotification(organization, null, NotificationChannel.EMAIL, NotificationType.MONTHLY_ORGANIZATION_REPORT, organization.getAdminEmail(), "Demo monthly report delivered.", today.minusDays(3)),
            demoNotification(organization, null, NotificationChannel.EMAIL, NotificationType.SUBSCRIPTION_PAYMENT_SUCCESS, organization.getAdminEmail(), "Demo billing receipt delivered.", today.minusDays(4))
        );

        for (NotificationLog notification : notificationLogRepository.saveAll(notifications)) {
            notificationDeliveryLogRepository.save(deliveryLog(organization, notification));
        }

        whatsappReminderLogRepository.saveAll(List.of(
            whatsappReminder(organization, memberships.get(25).getMember(), today.minusDays(1)),
            whatsappReminder(organization, memberships.get(26).getMember(), today.minusDays(2)),
            whatsappReminder(organization, memberships.get(27).getMember(), today.minusDays(3))
        ));
    }

    private NotificationLog demoNotification(
        Organization organization,
        Member member,
        NotificationChannel channel,
        NotificationType type,
        String recipient,
        String message,
        LocalDate notificationDate
    ) {
        NotificationLog notification = new NotificationLog();
        notification.setOrganization(organization);
        notification.setMember(member);
        notification.setMemberCount(member == null ? 0 : 1);
        notification.setChannel(channel);
        notification.setType(type);
        notification.setRecipient(recipient == null || recipient.isBlank() ? "demo@billmygym.test" : recipient);
        notification.setStatus(NotificationStatus.SENT);
        notification.setAttemptCount(1);
        notification.setNotificationDate(notificationDate);
        notification.setSentAt(notificationDate.atTime(10, 30).atZone(APP_ZONE).toOffsetDateTime());
        notification.setMessage(message);
        return notification;
    }

    private NotificationDeliveryLog deliveryLog(Organization organization, NotificationLog notification) {
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setOrganization(organization);
        log.setNotification(notification);
        log.setType(notification.getType());
        log.setStatus(NotificationDeliveryStatus.SENT);
        log.setAttemptCount(1);
        log.setLastAttempt(notification.getSentAt() == null ? Instant.now() : notification.getSentAt().toInstant());
        log.setProvider(notification.getChannel().name());
        log.setTemplateName("demo_%s".formatted(notification.getType().name().toLowerCase()));
        log.setRecipient(notification.getRecipient());
        log.setProviderMessageId("demo-message-%d".formatted(notification.getId()));
        log.setRequestPayload("{}");
        return log;
    }

    private WhatsappReminderLog whatsappReminder(Organization organization, Member member, LocalDate sentDate) {
        WhatsappReminderLog log = new WhatsappReminderLog();
        log.setOrganization(organization);
        log.setMember(member);
        log.setMessage("Demo WhatsApp reminder sent to %s.".formatted(member.getFullName()));
        log.setStatus(WhatsappReminderStatus.SENT);
        log.setSentAt(sentDate.atTime(11, 15).atZone(APP_ZONE).toOffsetDateTime());
        return log;
    }

    private void seedUsageMetrics(Organization organization, YearMonth currentMonth) {
        UsageMetric current = UsageMetric.empty(organization, currentMonth);
        current.setMonthlyWhatsappCount(8);
        current.setMonthlyEmailCount(5);
        current.setMonthlyDigestCount(2);
        current.setActiveMembers(25);
        current.setActiveAdmins(1);
        current.setSubscriptionStatus(organization.getStatus().name());

        UsageMetric previous = UsageMetric.empty(organization, currentMonth.minusMonths(1));
        previous.setMonthlyWhatsappCount(6);
        previous.setMonthlyEmailCount(4);
        previous.setMonthlyDigestCount(1);
        previous.setActiveMembers(24);
        previous.setActiveAdmins(1);
        previous.setSubscriptionStatus(organization.getStatus().name());

        usageMetricRepository.saveAll(List.of(current, previous));
    }

    public record DemoResetResult(Long organizationId, String message) {
    }
}
