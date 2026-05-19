package com.gymflow.service;

import com.gymflow.domain.NotificationDeliveryLog;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.NotificationDeliveryStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.repository.NotificationDeliveryLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryLogService {

    private final NotificationDeliveryLogRepository notificationDeliveryLogRepository;

    @Transactional(readOnly = true)
    public boolean hasSuccessfulDeliveryForDay(Long organizationId, NotificationType type, LocalDate date) {
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return notificationDeliveryLogRepository.existsByOrganizationIdAndTypeAndStatusInAndLastAttemptBetween(
            organizationId,
            type,
            List.of(NotificationDeliveryStatus.SENT),
            start,
            end
        );
    }

    @Transactional
    public NotificationDeliveryLog create(NotificationLog notification, Organization organization, NotificationType type) {
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setNotification(notification);
        log.setOrganization(organization);
        log.setType(type);
        log.setStatus(NotificationDeliveryStatus.PENDING);
        log.setAttemptCount(0);
        return notificationDeliveryLogRepository.save(log);
    }

    @Transactional
    public void markAttempt(NotificationDeliveryLog log, int attempt, NotificationDeliveryStatus status) {
        log.setAttemptCount(attempt);
        log.setStatus(status);
        log.setLastAttempt(Instant.now());
    }

    @Transactional
    public void recordSuccessfulDelivery(Organization organization, NotificationType type) {
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setOrganization(organization);
        log.setType(type);
        log.setStatus(NotificationDeliveryStatus.SENT);
        log.setAttemptCount(1);
        log.setLastAttempt(Instant.now());
        notificationDeliveryLogRepository.save(log);
    }
}
