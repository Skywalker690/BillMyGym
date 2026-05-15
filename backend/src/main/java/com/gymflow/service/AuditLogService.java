package com.gymflow.service;

import com.gymflow.domain.AuditLog;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.repository.AuditLogRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String event, String description, AuditCategory category, String zone) {
        AuthenticatedUser actor = SecurityUtils.currentUserOrSystem();
        persist(event, description, category, zone, actor.email(), actor.email());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSystem(String event, String description, AuditCategory category, String zone) {
        persist(event, description, category, zone, "SYSTEM", "SYSTEM");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActor(String event, String description, AuditCategory category, String zone, String actorEmail) {
        persist(event, description, category, zone, actorEmail, actorEmail);
    }

    private void persist(String event, String description, AuditCategory category, String zone, String actorName, String actorEmail) {
        try {
            auditLogRepository.save(AuditLog.builder()
                .event(event)
                .description(description)
                .category(category)
                .zone(zone)
                .actorName(actorName)
                .actorEmail(actorEmail)
                .build());
        } catch (Exception ex) {
            log.warn("Failed to persist audit log for event={}", event, ex);
        }
    }
}
