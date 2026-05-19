package com.gymflow.subscription.event;

import com.gymflow.domain.enums.NotificationType;
import com.gymflow.service.EmailService;
import com.gymflow.service.NotificationDeliveryLogService;
import com.gymflow.service.WhatsAppDeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionNotificationListener {

    private final EmailService emailService;
    private final NotificationDeliveryLogService notificationDeliveryLogService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;

    @EventListener
    public void onSubscriptionPaymentPaid(SubscriptionPaymentPaidEvent event) {
        emailService.sendPaymentSuccessEmail(
            event.organization(),
            event.payment().getAmount(),
            event.periodStart(),
            event.periodEnd(),
            event.usage().getMonthlyWhatsappCount(),
            event.usage().getMonthlyEmailCount(),
            event.usage().getActiveMembers(),
            event.billingSummary()
        );
        notificationDeliveryLogService.recordSuccessfulDelivery(event.organization(), NotificationType.SUBSCRIPTION_PAYMENT_SUCCESS);
        whatsAppDeliveryService.sendPaymentSuccess(
            event.organization(),
            "%s - %s".formatted(event.periodStart(), event.periodEnd())
        );
    }

    @EventListener
    public void onSubscriptionPaymentFailed(SubscriptionPaymentFailedEvent event) {
        emailService.sendPaymentFailedEmail(
            event.payment().getOrganization(),
            event.payment().getAmount(),
            event.payment().getOrganization().getSubscriptionDueDate(),
            "/app/subscription",
            event.payment().getFailureReason(),
            event.usage().getMonthlyWhatsappCount(),
            event.usage().getMonthlyEmailCount(),
            event.usage().getActiveMembers(),
            event.billingSummary()
        );
        notificationDeliveryLogService.recordSuccessfulDelivery(
            event.payment().getOrganization(),
            NotificationType.SUBSCRIPTION_PAYMENT_FAILED
        );
    }
}
