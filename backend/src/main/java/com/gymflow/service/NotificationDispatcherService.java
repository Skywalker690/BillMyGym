package com.gymflow.service;

import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.UsageMetricRepository;
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
public class NotificationDispatcherService {

    private static final int MAX_ATTEMPTS = 3;
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final NotificationLogRepository notificationLogRepository;
    private final UsageMetricRepository usageMetricRepository;

    @Transactional
    public DispatchResult dispatchDigest(
        Organization organization,
        OrganizationSetting setting,
        OrganizationDigestBuilderService.DigestContent digestContent
    ) {
        List<NotificationLog> deliveryLogs = new ArrayList<>();

        if (setting.isWhatsappEnabled() && hasText(organization.getAdminPhoneNumber())) {
            deliveryLogs.add(sendWithRetry(
                organization,
                NotificationChannel.WHATSAPP,
                organization.getAdminPhoneNumber(),
                digestContent.whatsappMessage(),
                digestContent.memberCount()
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
            UsageMetric metric = resolveMetric(organization);
            for (NotificationLog log : deliveryLogs) {
                if (log.getStatus() == NotificationStatus.SENT) {
                    if (log.getChannel() == NotificationChannel.WHATSAPP) {
                        metric.incrementMonthlyWhatsappCount();
                    } else if (log.getChannel() == NotificationChannel.EMAIL) {
                        metric.incrementMonthlyEmailCount();
                    }
                }
            }
            metric.incrementMonthlyDigestCount();
            usageMetricRepository.save(metric);
        }

        return new DispatchResult(digestsSent > 0, digestsSent);
    }

    private NotificationLog sendWithRetry(
        Organization organization,
        NotificationChannel channel,
        String recipient,
        String message,
        int memberCount
    ) {
        NotificationLog log = buildPendingLog(organization, channel, recipient, message, memberCount);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            log.setAttemptCount(attempt);
            if (attempt > 1) {
                log.setStatus(NotificationStatus.RETRY);
            }

            DeliveryResult result = simulateSend(channel, recipient);
            if (result.success()) {
                log.setStatus(NotificationStatus.SENT);
                log.setFailureReason(null);
                log.setSentAt(OffsetDateTime.now(APP_ZONE));
                return notificationLogRepository.save(log);
            }

            log.setFailureReason(result.failureReason());
        }

        log.setStatus(NotificationStatus.FAILED);
        return notificationLogRepository.save(log);
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

    private UsageMetric resolveMetric(Organization organization) {
        YearMonth currentMonth = YearMonth.now();
        return usageMetricRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            organization.getId(),
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        ).orElseGet(() -> UsageMetric.empty(organization, currentMonth));
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
