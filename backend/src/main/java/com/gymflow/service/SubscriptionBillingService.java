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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionBillingService {

    static final BigDecimal MONTHLY_AMOUNT = new BigDecimal("299.00");
    private static final String PAYMENT_LINK = "/app/subscription";
    private static final List<Integer> REMINDER_OFFSETS = List.of(7, 3, 1);

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UsageMetricService usageMetricService;
    private final EmailService emailService;
    private final NotificationDeliveryLogService notificationDeliveryLogService;

    @Transactional
    public void processDailyBilling() {
        LocalDate today = LocalDate.now();
        sendUpcomingDueReminders(today);
        pauseOverdueOrganizations(today);
        deactivateLongUnpaidOrganizations(today);
        retryFailedPayments(today);
    }

    private void sendUpcomingDueReminders(LocalDate today) {
        for (int offset : REMINDER_OFFSETS) {
            LocalDate dueDate = today.plusDays(offset);
            for (Organization organization : organizationRepository.findAllByDeletedFalseAndOrganizationTypeAndSubscriptionDueDateIn(
                OrganizationType.REGULAR,
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
                if (organization.getStatus() == OrganizationStatus.TRIAL) {
                    emailService.sendTrialEndingEmail(
                        organization,
                        MONTHLY_AMOUNT,
                        dueDate,
                        PAYMENT_LINK,
                        usage.getMonthlyWhatsappCount(),
                        usage.getMonthlyEmailCount(),
                        usage.getActiveMembers(),
                        summary
                    );
                    notificationDeliveryLogService.recordSuccessfulDelivery(organization, NotificationType.TRIAL_ENDING);
                } else {
                    emailService.sendSubscriptionReminderEmail(
                        organization,
                        organization.getAdminEmail(),
                        MONTHLY_AMOUNT,
                        dueDate,
                        PAYMENT_LINK,
                        usage.getMonthlyWhatsappCount(),
                        usage.getMonthlyEmailCount(),
                        usage.getActiveMembers(),
                        summary
                    );
                    notificationDeliveryLogService.recordSuccessfulDelivery(organization, NotificationType.SUBSCRIPTION_REMINDER);
                }
            }
        }
    }

    private void pauseOverdueOrganizations(LocalDate today) {
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndOrganizationTypeAndStatusAndSubscriptionDueDateBefore(
            OrganizationType.REGULAR,
            OrganizationStatus.ACTIVE,
            today
        )) {
            pauseOrganization(organization, today);
        }
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndOrganizationTypeAndStatusAndSubscriptionDueDateBefore(
            OrganizationType.REGULAR,
            OrganizationStatus.TRIAL,
            today
        )) {
            pauseOrganization(organization, today);
        }
    }

    private void pauseOrganization(Organization organization, LocalDate today) {
        organization.setStatus(OrganizationStatus.PAUSED);
        if (organization.getPausedAt() == null) {
            organization.setPausedAt(OffsetDateTime.now());
        }
        if (notificationDeliveryLogService.hasSuccessfulDeliveryForDay(
            organization.getId(),
            NotificationType.SUBSCRIPTION_PAUSED,
            today
        )) {
            return;
        }
        UsageMetric usage = usageMetricService.refreshMonthlySnapshot(organization);
        emailService.sendPausedNotificationEmail(
            organization,
            MONTHLY_AMOUNT,
            organization.getSubscriptionDueDate() == null ? today : organization.getSubscriptionDueDate(),
            PAYMENT_LINK,
            usage.getMonthlyWhatsappCount(),
            usage.getMonthlyEmailCount(),
            usage.getActiveMembers(),
            billingSummary(usage)
        );
        notificationDeliveryLogService.recordSuccessfulDelivery(organization, NotificationType.SUBSCRIPTION_PAUSED);
    }

    private void deactivateLongUnpaidOrganizations(LocalDate today) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(90);
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndStatus(OrganizationStatus.PAUSED)) {
            if (organization.getPausedAt() == null || !organization.getPausedAt().isBefore(cutoff)) {
                continue;
            }
            if (notificationDeliveryLogService.hasSuccessfulDeliveryForDay(
                organization.getId(),
                NotificationType.SUBSCRIPTION_DEACTIVATED,
                today
            )) {
                organization.setStatus(OrganizationStatus.DEACTIVATED);
                organization.setDeactivatedAt(OffsetDateTime.now());
                continue;
            }
            organization.setStatus(OrganizationStatus.DEACTIVATED);
            organization.setDeactivatedAt(OffsetDateTime.now());
            UsageMetric usage = usageMetricService.refreshMonthlySnapshot(organization);
            emailService.sendDeactivatedNotificationEmail(
                organization,
                MONTHLY_AMOUNT,
                organization.getSubscriptionDueDate() == null ? today : organization.getSubscriptionDueDate(),
                PAYMENT_LINK,
                usage.getMonthlyWhatsappCount(),
                usage.getMonthlyEmailCount(),
                usage.getActiveMembers(),
                billingSummary(usage)
            );
            notificationDeliveryLogService.recordSuccessfulDelivery(organization, NotificationType.SUBSCRIPTION_DEACTIVATED);
        }
    }

    private void retryFailedPayments(LocalDate today) {
        subscriptionPaymentRepository.findByPaymentStatusInAndNextRetryDateLessThanEqualAndDeletedFalse(
            List.of(SubscriptionPaymentStatus.FAILED, SubscriptionPaymentStatus.PENDING),
            today
        ).forEach(payment -> {
            payment.setRetryCount(payment.getRetryCount() + 1);
            payment.setPaymentStatus(SubscriptionPaymentStatus.PENDING);
            payment.setNextRetryDate(today.plusDays(Math.min(payment.getRetryCount() + 1L, 7L)));
        });
    }

    private String billingSummary(UsageMetric usage) {
        return "WhatsApp sent: %d, emails sent: %d, renewal digests: %d."
            .formatted(usage.getMonthlyWhatsappCount(), usage.getMonthlyEmailCount(), usage.getMonthlyDigestCount());
    }
}
