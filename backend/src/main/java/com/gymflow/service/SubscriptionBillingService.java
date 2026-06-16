package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionPaymentRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionBillingService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final String PAYMENT_LINK = "/app/subscription";
    private static final List<Integer> REMINDER_OFFSETS = List.of(7, 3, 1);
    private static final List<OrganizationType> BILLABLE_TYPES = List.of(OrganizationType.REGULAR);

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UsageMetricService usageMetricService;
    private final EmailService emailService;
    private final NotificationDeliveryLogService notificationDeliveryLogService;
    private final BillingLifecycleService billingLifecycleService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;
    private final SubscriptionPricingService subscriptionPricingService;
    private final OrganizationPaymentWebhookService organizationPaymentWebhookService;

    @Transactional
    public void processDailyBilling() {
        LocalDate today = LocalDate.now(APP_ZONE);
        sendUpcomingDueReminders(today);
        pauseOverdueOrganizations(today);
        deactivateLongUnpaidOrganizations(today);
        retryFailedPayments(today);
    }

    private void sendUpcomingDueReminders(LocalDate today) {
        for (int offset : REMINDER_OFFSETS) {
            LocalDate dueDate = today.plusDays(offset);
            for (Organization organization : organizationRepository.findAllByDeletedFalseAndOrganizationTypeInAndSubscriptionDueDateIn(
                BILLABLE_TYPES,
                List.of(dueDate)
            )) {
                if (organization.getStatus() == OrganizationStatus.DEACTIVATED || organization.getStatus() == OrganizationStatus.PAUSED) {
                    continue;
                }
                NotificationType type = organization.getStatus() == OrganizationStatus.TRIAL
                    ? NotificationType.TRIAL_ENDING
                    : NotificationType.SUBSCRIPTION_REMINDER;
                if (notificationDeliveryLogService.hasSuccessfulDeliveryForDay(organization.getId(), type, today)) {
                    continue;
                }
                UsageMetric usage = usageMetricService.refreshMonthlySnapshot(organization);
                String summary = billingSummary(usage);
                BigDecimal monthlyAmount = subscriptionPricingService.currentPrice(organization).totalAmount();
                if (organization.getStatus() == OrganizationStatus.TRIAL) {
                    emailService.sendTrialEndingEmail(
                        organization,
                        monthlyAmount,
                        dueDate,
                        PAYMENT_LINK,
                        usage.getMonthlyWhatsappCount(),
                        usage.getMonthlyEmailCount(),
                        usage.getActiveMembers(),
                        summary
                    );
                    notificationDeliveryLogService.recordSuccessfulDelivery(organization, NotificationType.TRIAL_ENDING);
                    whatsAppDeliveryService.sendSubscriptionDue(organization, monthlyAmount, dueDate, PAYMENT_LINK);
                } else {
                    emailService.sendSubscriptionReminderEmail(
                        organization,
                        organization.getAdminEmail(),
                        monthlyAmount,
                        dueDate,
                        PAYMENT_LINK,
                        usage.getMonthlyWhatsappCount(),
                        usage.getMonthlyEmailCount(),
                        usage.getActiveMembers(),
                        summary
                    );
                    notificationDeliveryLogService.recordSuccessfulDelivery(organization, NotificationType.SUBSCRIPTION_REMINDER);
                    whatsAppDeliveryService.sendSubscriptionDue(organization, monthlyAmount, dueDate, PAYMENT_LINK);
                }
            }
        }
    }

    private void pauseOverdueOrganizations(LocalDate today) {
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndOrganizationTypeInAndStatusAndSubscriptionDueDateBefore(
            BILLABLE_TYPES,
            OrganizationStatus.ACTIVE,
            today   // BUG-01: was today.plusDays(1) — orgs were being paused ON their due date, not after it
        )) {
            pauseOrganization(organization, today);
        }
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndOrganizationTypeInAndStatusAndSubscriptionDueDateBefore(
            BILLABLE_TYPES,
            OrganizationStatus.TRIAL,
            today   // BUG-01: same fix for trial orgs
        )) {
            pauseOrganization(organization, today);
        }
    }

    private void pauseOrganization(Organization organization, LocalDate today) {
        billingLifecycleService.markPaused(organization);
        if (notificationDeliveryLogService.hasSuccessfulDeliveryForDay(
            organization.getId(),
            NotificationType.SUBSCRIPTION_PAUSED,
            today
        )) {
            return;
        }
        UsageMetric usage = usageMetricService.refreshMonthlySnapshot(organization);
        BigDecimal monthlyAmount = subscriptionPricingService.currentPrice(organization).totalAmount();
        LocalDate dueDate = organization.getSubscriptionDueDate() == null ? today : organization.getSubscriptionDueDate();
        
        emailService.sendPausedNotificationEmail(
            organization,
            monthlyAmount,
            dueDate,
            PAYMENT_LINK,
            usage.getMonthlyWhatsappCount(),
            usage.getMonthlyEmailCount(),
            usage.getActiveMembers(),
            billingSummary(usage)
        );
        whatsAppDeliveryService.sendSubscriptionDue(
            organization,
            monthlyAmount,
            dueDate,
            PAYMENT_LINK
        );
        notificationDeliveryLogService.recordSuccessfulDelivery(organization, NotificationType.SUBSCRIPTION_PAUSED);
    }

    private void deactivateLongUnpaidOrganizations(LocalDate today) {
        OffsetDateTime cutoff = OffsetDateTime.now(APP_ZONE).minusDays(90);
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndStatus(OrganizationStatus.PAUSED)) {
            if (organization.getPausedAt() == null || !organization.getPausedAt().isBefore(cutoff)) {
                continue;
            }
            // BUG-03: Deactivate first (idempotent), then check notification dedup before sending email
            organization.setStatus(OrganizationStatus.DEACTIVATED);
            organization.setDeactivatedAt(OffsetDateTime.now(APP_ZONE));
            if (notificationDeliveryLogService.hasSuccessfulDeliveryForDay(
                organization.getId(),
                NotificationType.SUBSCRIPTION_DEACTIVATED,
                today
            )) {
                continue;
            }
            UsageMetric usage = usageMetricService.refreshMonthlySnapshot(organization);
            BigDecimal monthlyAmount = subscriptionPricingService.currentPrice(organization).totalAmount();
            LocalDate dueDate = organization.getSubscriptionDueDate() == null ? today : organization.getSubscriptionDueDate();

            emailService.sendDeactivatedNotificationEmail(
                organization,
                monthlyAmount,
                dueDate,
                PAYMENT_LINK,
                usage.getMonthlyWhatsappCount(),
                usage.getMonthlyEmailCount(),
                usage.getActiveMembers(),
                billingSummary(usage)
            );
            whatsAppDeliveryService.sendSubscriptionDue(
                organization,
                monthlyAmount,
                dueDate,
                PAYMENT_LINK
            );
            notificationDeliveryLogService.recordSuccessfulDelivery(organization, NotificationType.SUBSCRIPTION_DEACTIVATED);
        }
    }

    private void retryFailedPayments(LocalDate today) {
        subscriptionPaymentRepository.findByPaymentStatusInAndNextRetryDateLessThanEqualAndDeletedFalse(
            List.of(SubscriptionPaymentStatus.PENDING),
            today
        ).forEach(payment -> {
            payment.setRetryCount(payment.getRetryCount() + 1);
            organizationPaymentWebhookService.syncPendingPayment(payment);
            if (payment.getPaymentStatus() == SubscriptionPaymentStatus.PENDING) {
                payment.setNextRetryDate(today.plusDays(Math.min(payment.getRetryCount() + 1L, 7L)));
            }
        });
    }

    private String billingSummary(UsageMetric usage) {
        return "WhatsApp sent: %d, emails sent: %d, renewal digests: %d."
            .formatted(usage.getMonthlyWhatsappCount(), usage.getMonthlyEmailCount(), usage.getMonthlyDigestCount());
    }
}
