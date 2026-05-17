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
import org.springframework.data.domain.PageRequest;
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
        return listReminderLogs(organizationId, 100);
    }

    public List<NotificationResponse> listRecentReminderLogs(Long organizationId, int limit) {
        return listReminderLogs(organizationId, limit);
    }

    private List<NotificationResponse> listReminderLogs(Long organizationId, int limit) {
        return notificationLogRepository.findByOrganizationIdAndTypeInAndDeletedFalseOrderByCreatedAtDesc(
                organizationId,
                List.of(NotificationType.ORGANIZATION_RENEWAL_DIGEST),
                PageRequest.of(0, Math.max(1, limit))
            ).stream()
            .map(this::toResponse)
            .toList();
    }

    private NotificationResponse toResponse(NotificationLog log) {
        return new NotificationResponse(
            log.getId(),
            log.getMember() == null ? null : log.getMember().getId(),
            log.getMemberCount(),
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
