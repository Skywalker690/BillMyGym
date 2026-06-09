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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDispatcherService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private final NotificationLogRepository notificationLogRepository;
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

        int digestsSent = (int) deliveryLogs.stream().filter(log -> log.getStatus() == NotificationStatus.SENT).count();
        if (digestsSent > 0) {
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
            persistedLog.setSentAt(OffsetDateTime.now(APP_ZONE));
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
        log.setNotificationDate(LocalDate.now(APP_ZONE));
        return log;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record DispatchResult(boolean digestTriggered, int digestsSent) {
    }

}
