package com.gymflow.service;

import com.gymflow.domain.Member;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.UsageMetricRepository;
import java.time.OffsetDateTime;
import java.time.YearMonth;
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
    private final UsageMetricRepository usageMetricRepository;

    @Transactional
    public DispatchResult dispatch(Member member, OrganizationSetting setting, NotificationType type) {
        List<NotificationLog> deliveryLogs = new ArrayList<>();
        boolean reminderTriggered = false;

        if (setting.isWhatsappEnabled() && hasText(member.getPhoneNumber())) {
            reminderTriggered = true;
            deliveryLogs.add(sendWithRetry(
                member,
                type,
                NotificationChannel.WHATSAPP,
                member.getPhoneNumber(),
                buildWhatsappMessage(member, setting, type)
            ));
        }

        if (setting.isEmailEnabled() && hasText(member.getEmail())) {
            reminderTriggered = true;
            deliveryLogs.add(sendWithRetry(
                member,
                type,
                NotificationChannel.EMAIL,
                member.getEmail(),
                buildEmailMessage(member, setting, type)
            ));
        }

        UsageMetric metric = resolveMetric(member);
        boolean sentAny = false;
        for (NotificationLog log : deliveryLogs) {
            if (log.getStatus() == NotificationStatus.SENT) {
                sentAny = true;
                if (log.getChannel() == NotificationChannel.WHATSAPP) {
                    metric.incrementMonthlyWhatsappCount();
                } else if (log.getChannel() == NotificationChannel.EMAIL) {
                    metric.incrementMonthlyEmailCount();
                }
            }
        }
        if (reminderTriggered && sentAny && type != NotificationType.WELCOME_MESSAGE) {
            metric.incrementMonthlyRemindersExecuted();
        }
        usageMetricRepository.save(metric);

        return new DispatchResult(
            reminderTriggered,
            (int) deliveryLogs.stream().filter(log -> log.getStatus() == NotificationStatus.SENT).count()
        );
    }

    private NotificationLog sendWithRetry(
        Member member,
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        String message
    ) {
        NotificationLog log = buildPendingLog(member, type, channel, recipient, message);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            log.setAttemptCount(attempt);
            if (attempt > 1) {
                log.setStatus(NotificationStatus.RETRY);
            }

            DeliveryResult result = simulateSend(channel, recipient);
            if (result.success()) {
                log.setStatus(NotificationStatus.SENT);
                log.setFailureReason(null);
                log.setSentAt(OffsetDateTime.now());
                return notificationLogRepository.save(log);
            }

            log.setFailureReason(result.failureReason());
        }

        log.setStatus(NotificationStatus.FAILED);
        return notificationLogRepository.save(log);
    }

    private NotificationLog buildPendingLog(
        Member member,
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        String message
    ) {
        NotificationLog log = new NotificationLog();
        log.setOrganization(member.getOrganization());
        log.setMember(member);
        log.setType(type);
        log.setChannel(channel);
        log.setRecipient(recipient);
        log.setStatus(NotificationStatus.PENDING);
        log.setMessage(message);
        log.setAttemptCount(0);
        return log;
    }

    private UsageMetric resolveMetric(Member member) {
        YearMonth currentMonth = YearMonth.now();
        return usageMetricRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            member.getOrganization().getId(),
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        ).orElseGet(() -> UsageMetric.empty(member.getOrganization(), currentMonth));
    }

    private DeliveryResult simulateSend(NotificationChannel channel, String recipient) {
        if (!hasText(recipient)) {
            return new DeliveryResult(false, channel.name() + " recipient is missing");
        }
        return new DeliveryResult(true, null);
    }

    private String buildWhatsappMessage(Member member, OrganizationSetting setting, NotificationType type) {
        return switch (type) {
            case WELCOME_MESSAGE -> "Welcome to " + brandingName(setting, member);
            case MEMBERSHIP_EXPIRY -> "Hello " + member.getFullName() + ",\n\nYour membership expires tomorrow.\n\nPlease renew membership.";
            case MEMBERSHIP_OVERDUE -> "Hello " + member.getFullName() + ",\n\nYour membership has expired.\n\nPlease contact gym administration.";
        };
    }

    private String buildEmailMessage(Member member, OrganizationSetting setting, NotificationType type) {
        String title = switch (type) {
            case WELCOME_MESSAGE -> "Welcome to " + brandingName(setting, member);
            case MEMBERSHIP_EXPIRY -> "Membership expiry reminder";
            case MEMBERSHIP_OVERDUE -> "Membership overdue reminder";
        };
        String body = switch (type) {
            case WELCOME_MESSAGE -> "<p>Welcome to <strong>" + brandingName(setting, member) + "</strong>.</p>";
            case MEMBERSHIP_EXPIRY -> "<p>Hello " + member.getFullName() + ",</p><p>Your membership expires tomorrow.</p><p>Please renew membership.</p>";
            case MEMBERSHIP_OVERDUE -> "<p>Hello " + member.getFullName() + ",</p><p>Your membership has expired.</p><p>Please contact gym administration.</p>";
        };
        return "<html><body><h2>" + title + "</h2>" + body + "</body></html>";
    }

    private String brandingName(OrganizationSetting setting, Member member) {
        return hasText(setting.getBrandingName()) ? setting.getBrandingName() : member.getOrganization().getName();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record DispatchResult(boolean reminderTriggered, int notificationsSent) {
    }

    private record DeliveryResult(boolean success, String failureReason) {
    }
}
