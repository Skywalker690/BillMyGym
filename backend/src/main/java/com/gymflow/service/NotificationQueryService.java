package com.gymflow.service;

import com.gymflow.api.dto.NotificationDtos.NotificationResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public List<NotificationResponse> listNotifications() {
        Long organizationId = requireOrgId();
        return notificationLogRepository.findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(organizationId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public List<NotificationResponse> listReminderLogs() {
        Long organizationId = requireOrgId();
        return notificationLogRepository.findByOrganizationIdAndTypeInAndDeletedFalseOrderByCreatedAtDesc(
                organizationId,
                List.of(NotificationType.MEMBERSHIP_EXPIRY, NotificationType.MEMBERSHIP_OVERDUE)
            ).stream()
            .map(this::toResponse)
            .toList();
    }

    private NotificationResponse toResponse(NotificationLog log) {
        return new NotificationResponse(
            log.getId(),
            log.getMember().getId(),
            log.getType(),
            log.getChannel(),
            log.getRecipient(),
            log.getStatus(),
            log.getAttemptCount(),
            log.getFailureReason(),
            log.getSentAt(),
            log.getMessage(),
            log.getCreatedAt()
        );
    }

    private Long requireOrgId() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }
}
