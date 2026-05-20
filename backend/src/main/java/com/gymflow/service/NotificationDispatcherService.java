package com.gymflow.service;

import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationDeliveryStatus;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.repository.NotificationLogRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDispatcherService {

    private static final int MAX_ATTEMPTS = 3;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationDeliveryLogService notificationDeliveryLogService;
    private final UsageMetricService usageMetricService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;
    private final WhatsAppTemplateService whatsAppTemplateService;
    private final AuditLogService auditLogService;

    @Transactional
    public DispatchResult dispatchDigest(
        Organization organization,
        OrganizationSetting setting,
        OrganizationDigestBuilderService.DigestContent digestContent
    ) {
        List<NotificationLog> deliveryLogs = new ArrayList<>();

        if (setting.isWhatsappEnabled() && hasText(organization.getAdminPhoneNumber())) {
            deliveryLogs.add(sendWhatsappDigest(
                organization,
                organization.getAdminPhoneNumber(),
                digestContent
            ));
        }

        if (setting.isEmailEnabled() && hasText(organization.getAdminEmail())) {
            deliveryLogs.add(sendWithRetry(
                organization,
                NotificationChannel.EMAIL,
                organization.getAdminEmail(),
                digestContent.emailMessage(),
                digestContent.memberCount()
            ));
        }

        int digestsSent = (int) deliveryLogs.stream().filter(log -> log.getStatus() == NotificationStatus.SENT).count();
        if (digestsSent > 0) {
            for (NotificationLog log : deliveryLogs) {
                if (log.getStatus() == NotificationStatus.SENT) {
                    if (log.getChannel() == NotificationChannel.EMAIL) {
                        usageMetricService.recordEmailSent(organization);
                    }
                }
            }
            usageMetricService.recordDigestSent(organization);
        }

        return new DispatchResult(digestsSent > 0, digestsSent);
    }

    private NotificationLog sendWhatsappDigest(
        Organization organization,
        String recipient,
        OrganizationDigestBuilderService.DigestContent digestContent
    ) {
        NotificationLog log = buildPendingLog(
            organization,
            NotificationChannel.WHATSAPP,
            recipient,
            digestContent.whatsappMessage(),
            digestContent.memberCount()
        );
        NotificationLog persistedLog = notificationLogRepository.save(log);
        var result = whatsAppDeliveryService.sendTemplate(
            organization,
            persistedLog,
            recipient,
            WhatsAppTemplateService.TEMPLATE_EXPIRY_DIGEST,
            whatsAppTemplateService.expiryDigestVariables(organization, digestContent),
            NotificationType.ORGANIZATION_RENEWAL_DIGEST
        );
        persistedLog.setAttemptCount(result.attemptCount());
        persistedLog.setFailureReason(result.errorMessage());
        if (result.sent()) {
            persistedLog.setStatus(NotificationStatus.SENT);
            persistedLog.setSentAt(OffsetDateTime.now());
            auditLogService.logOrganizationActivity(
                "NOTIFICATION_SENT",
                "WhatsApp reminder sent to " + recipient,
                AuditActivityScope.NOTIFICATION_ACTIVITY,
                AuditEventStatus.SUCCESS,
                "NOTIFICATION",
                organization.getId(),
                recipient
            );
        } else if (result.status() == NotificationDeliveryStatus.RETRYING) {
            persistedLog.setStatus(NotificationStatus.RETRYING);
            auditLogService.logOrganizationActivity(
                "NOTIFICATION_RETRYING",
                "WhatsApp reminder retry scheduled for " + recipient,
                AuditActivityScope.NOTIFICATION_ACTIVITY,
                AuditEventStatus.INFO,
                "NOTIFICATION",
                organization.getId(),
                recipient
            );
        } else {
            persistedLog.setStatus(NotificationStatus.FAILED);
            auditLogService.logOrganizationActivity(
                "NOTIFICATION_FAILED",
                "WhatsApp reminder failed for " + recipient,
                AuditActivityScope.NOTIFICATION_ACTIVITY,
                AuditEventStatus.FAILED,
                "NOTIFICATION",
                organization.getId(),
                recipient
            );
        }
        return notificationLogRepository.save(persistedLog);
    }

    private NotificationLog sendWithRetry(
        Organization organization,
        NotificationChannel channel,
        String recipient,
        String message,
        int memberCount
    ) {
        NotificationLog log = buildPendingLog(organization, channel, recipient, message, memberCount);
        NotificationLog persistedLog = notificationLogRepository.save(log);
        var deliveryLog = notificationDeliveryLogService.create(persistedLog, organization, NotificationType.ORGANIZATION_RENEWAL_DIGEST);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            persistedLog.setAttemptCount(attempt);
            if (attempt > 1) {
                persistedLog.setStatus(NotificationStatus.RETRYING);
                notificationDeliveryLogService.markAttempt(deliveryLog, attempt, NotificationDeliveryStatus.RETRYING);
            } else {
                notificationDeliveryLogService.markAttempt(deliveryLog, attempt, NotificationDeliveryStatus.PENDING);
            }

            DeliveryResult result = simulateSend(channel, recipient);
            if (result.success()) {
                persistedLog.setStatus(NotificationStatus.SENT);
                persistedLog.setFailureReason(null);
                persistedLog.setSentAt(OffsetDateTime.now());
                notificationDeliveryLogService.markAttempt(deliveryLog, attempt, NotificationDeliveryStatus.SENT);
                auditLogService.logOrganizationActivity(
                    "NOTIFICATION_SENT",
                    channel.name() + " reminder sent to " + recipient,
                    AuditActivityScope.NOTIFICATION_ACTIVITY,
                    AuditEventStatus.SUCCESS,
                    "NOTIFICATION",
                    organization.getId(),
                    recipient
                );
                return notificationLogRepository.save(persistedLog);
            }

            persistedLog.setFailureReason(result.failureReason());
        }

        persistedLog.setStatus(NotificationStatus.FAILED);
        notificationDeliveryLogService.markAttempt(deliveryLog, MAX_ATTEMPTS, NotificationDeliveryStatus.FAILED);
        auditLogService.logOrganizationActivity(
            "NOTIFICATION_FAILED",
            channel.name() + " reminder failed for " + recipient,
            AuditActivityScope.NOTIFICATION_ACTIVITY,
            AuditEventStatus.FAILED,
            "NOTIFICATION",
            organization.getId(),
            recipient
        );
        return notificationLogRepository.save(persistedLog);
    }

    private NotificationLog buildPendingLog(
        Organization organization,
        NotificationChannel channel,
        String recipient,
        String message,
        int memberCount
    ) {
        NotificationLog log = new NotificationLog();
        log.setOrganization(organization);
        log.setMember(null);
        log.setMemberCount(memberCount);
        log.setType(NotificationType.ORGANIZATION_RENEWAL_DIGEST);
        log.setChannel(channel);
        log.setRecipient(recipient);
        log.setStatus(NotificationStatus.PENDING);
        log.setMessage(message);
        log.setAttemptCount(0);
        log.setNotificationDate(LocalDate.now());
        return log;
    }

    private DeliveryResult simulateSend(NotificationChannel channel, String recipient) {
        if (!hasText(recipient)) {
            return new DeliveryResult(false, channel.name() + " recipient is missing");
        }
        return new DeliveryResult(true, null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record DispatchResult(boolean digestTriggered, int digestsSent) {
    }

    private record DeliveryResult(boolean success, String failureReason) {
    }
}
