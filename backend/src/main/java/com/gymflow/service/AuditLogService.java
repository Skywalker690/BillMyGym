package com.gymflow.service;

import com.gymflow.domain.AuditLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.User;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.AuditEventStatus;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.AuditLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final PlatformTransactionManager transactionManager;

    public void log(String event, String description, AuditCategory category, String zone) {
        AuthenticatedUser actor = SecurityUtils.currentUserOrSystem();
        User user = "SYSTEM".equalsIgnoreCase(actor.email())
            ? null
            : appUserRepository.findByEmailAndDeletedFalse(actor.email()).orElse(null);
        persist(
            event,
            description,
            category,
            defaultScopeFor(category),
            AuditEventStatus.INFO,
            zone,
            user == null ? actor.email() : resolveActorName(user),
            actor.email(),
            user == null || user.getRole() == null ? null : user.getRole().name(),
            user == null ? null : user.getOrganization(),
            null,
            null,
            null,
            null
        );
    }

    public void logSystem(String event, String description, AuditCategory category, String zone) {
        persist(
            event,
            description,
            category,
            defaultScopeFor(category),
            AuditEventStatus.INFO,
            zone,
            "SYSTEM",
            "SYSTEM",
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    public void logActor(String event, String description, AuditCategory category, String zone, String actorEmail) {
        User user = appUserRepository.findByEmailAndDeletedFalse(actorEmail).orElse(null);
        persist(
            event,
            description,
            category,
            defaultScopeFor(category),
            AuditEventStatus.INFO,
            zone,
            user == null ? actorEmail : resolveActorName(user),
            actorEmail,
            user == null || user.getRole() == null ? null : user.getRole().name(),
            user == null ? null : user.getOrganization(),
            null,
            null,
            null,
            null
        );
    }

    public void logOrganizationActivity(
        String event,
        String description,
        AuditActivityScope scope,
        AuditEventStatus eventStatus,
        String zone,
        Long organizationId,
        String targetName
    ) {
        AuthenticatedUser actor = SecurityUtils.currentUserOrSystem();
        User user = "SYSTEM".equalsIgnoreCase(actor.email())
            ? null
            : appUserRepository.findByEmailAndDeletedFalse(actor.email()).orElse(null);
        Organization organization = user == null ? null : user.getOrganization();
        if (organization == null && organizationId != null) {
            organization = organizationRepository.findByIdAndDeletedFalse(organizationId).orElse(null);
        }
        persist(
            event,
            description,
            AuditCategory.ORGANIZATION,
            scope,
            eventStatus,
            zone,
            user == null ? actor.email() : resolveActorName(user),
            actor.email(),
            user == null || user.getRole() == null ? null : user.getRole().name(),
            organization,
            targetName,
            null,
            null,
            null
        );
    }

    public void logLoginActivity(
        String event,
        String description,
        AuditEventStatus eventStatus,
        User user,
        RequestAuditContext requestAuditContext
    ) {
        if (user == null) {
            return;
        }
        persist(
            event,
            description,
            AuditCategory.AUTHENTICATION,
            AuditActivityScope.GLOBAL_LOGIN,
            eventStatus,
            "AUTH",
            resolveActorName(user),
            user.getEmail(),
            user.getRole() == null ? null : user.getRole().name(),
            user.getOrganization(),
            null,
            requestAuditContext == null ? null : requestAuditContext.ipAddress(),
            requestAuditContext == null ? null : requestAuditContext.device(),
            requestAuditContext == null ? null : requestAuditContext.browser()
        );
    }

    private void persist(
        String event,
        String description,
        AuditCategory category,
        AuditActivityScope scope,
        AuditEventStatus eventStatus,
        String zone,
        String actorName,
        String actorEmail,
        String actorRole,
        Organization organization,
        String targetName,
        String ipAddress,
        String device,
        String browser
    ) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionTemplate.executeWithoutResult(status -> auditLogRepository.saveAndFlush(AuditLog.builder()
                .event(event)
                .description(description)
                .category(category)
                .activityScope(scope)
                .eventStatus(eventStatus)
                .zone(zone)
                .actorName(actorName)
                .actorEmail(actorEmail)
                .actorRole(actorRole)
                .organization(organization)
                .targetName(targetName)
                .ipAddress(ipAddress)
                .device(device)
                .browser(browser)
                .build()));
        } catch (Exception ex) {
            log.warn("Failed to persist audit log for event={}", event, ex);
        }
    }

    private AuditActivityScope defaultScopeFor(AuditCategory category) {
        return switch (category) {
            case AUTHENTICATION -> AuditActivityScope.GLOBAL_LOGIN;
            case MEMBER -> AuditActivityScope.MEMBER_ACTIVITY;
            case ORGANIZATION, SYSTEM, SECURITY -> AuditActivityScope.ADMIN_ACTIVITY;
        };
    }

    private String resolveActorName(User user) {
        if (user == null) {
            return "SYSTEM";
        }
        if (user.getOrganization() != null && user.getOrganization().getAdminFullName() != null
            && !user.getOrganization().getAdminFullName().isBlank()) {
            return user.getOrganization().getAdminFullName();
        }
        return user.getEmail();
    }

    public record RequestAuditContext(
        String ipAddress,
        String device,
        String browser
    ) {
    }
}
