package com.gymflow.service;

import com.gymflow.api.dto.AuditDtos.AdminActivityAuditRow;
import com.gymflow.api.dto.AuditDtos.AuditPageResponse;
import com.gymflow.api.dto.AuditDtos.GlobalLoginAuditResponse;
import com.gymflow.api.dto.AuditDtos.GlobalLoginAuditRow;
import com.gymflow.api.dto.AuditDtos.MemberActivityAuditRow;
import com.gymflow.api.dto.AuditDtos.MemberStatusAuditRow;
import com.gymflow.api.dto.AuditDtos.NotificationAuditResponse;
import com.gymflow.api.dto.AuditDtos.NotificationAuditRow;
import com.gymflow.api.dto.AuditDtos.NotificationAuditSummary;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.AuditLog;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.Role;
import com.gymflow.repository.AuditLogRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.SecurityUtils;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int CSV_EXPORT_LIMIT = 5000;

    private final AuditLogRepository auditLogRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public GlobalLoginAuditResponse listGlobalLoginAudit(
        LoginAuditTab tab,
        String search,
        Long organizationId,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
    ) {
        ensureSuperAdmin();
        Instant fromTime = fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String pattern = normalizePattern(search);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        AuditEventStatus status = toLoginStatus(tab);

        Page<AuditLog> result = auditLogRepository.findGlobalLoginAuditPage(
            status,
            organizationId,
            fromTime,
            toTime,
            pattern,
            PageRequest.of(safePage, safeSize)
        );

        AuditPageResponse<GlobalLoginAuditRow> pageResponse = new AuditPageResponse<>(
            result.getContent().stream().map(this::toGlobalLoginRow).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        );
        return new GlobalLoginAuditResponse(
            pageResponse,
            auditLogRepository.countGlobalLoginByStatus(AuditEventStatus.SUCCESS, organizationId, fromTime, toTime, pattern),
            auditLogRepository.countGlobalLoginByStatus(AuditEventStatus.FAILED, organizationId, fromTime, toTime, pattern),
            auditLogRepository.countGlobalLoginByStatus(AuditEventStatus.SUSPICIOUS, organizationId, fromTime, toTime, pattern)
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportGlobalLoginAuditCsv(
        LoginAuditTab tab,
        String search,
        Long organizationId,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        GlobalLoginAuditResponse response = listGlobalLoginAudit(tab, search, organizationId, fromDate, toDate, 0, CSV_EXPORT_LIMIT);
        StringBuilder csv = new StringBuilder();
        csv.append("Time,Organization,Admin Name,Email,Device,Browser,IP,Status,Description\n");
        for (GlobalLoginAuditRow row : response.page().items()) {
            csv.append(safeCsv(row.time() == null ? "" : row.time().toString())).append(',')
                .append(safeCsv(row.organizationName())).append(',')
                .append(safeCsv(row.adminName())).append(',')
                .append(safeCsv(row.email())).append(',')
                .append(safeCsv(row.device())).append(',')
                .append(safeCsv(row.browser())).append(',')
                .append(safeCsv(row.ip())).append(',')
                .append(safeCsv(row.status())).append(',')
                .append(safeCsv(row.description())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public AuditPageResponse<MemberActivityAuditRow> listOrganizationMemberActivity(
        Long organizationId,
        OrganizationAuditTab tab,
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
    ) {
        ensureSuperAdmin();
        organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        AuditActivityScope scope = switch (tab) {
            case MEMBER_ACTIVITY -> AuditActivityScope.MEMBER_ACTIVITY;
            case MEMBER_STATUS_ACTIVITY -> AuditActivityScope.MEMBER_STATUS_ACTIVITY;
            case ADMIN_ACTIVITY -> AuditActivityScope.ADMIN_ACTIVITY;
            case NOTIFICATION_ACTIVITY -> throw new ForbiddenException("Use notification audit endpoint for notification activity tab");
        };
        Instant fromTime = fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String pattern = normalizePattern(search);

        Page<AuditLog> result = auditLogRepository.findOrganizationAuditPage(
            organizationId,
            scope,
            fromTime,
            toTime,
            pattern,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))
        );

        if (tab == OrganizationAuditTab.MEMBER_STATUS_ACTIVITY) {
            List<MemberActivityAuditRow> rows = result.getContent().stream()
                .map(auditLog -> new MemberActivityAuditRow(
                    auditLog.getTimestamp(),
                    auditLog.getActorName(),
                    humanizeAction(auditLog.getEvent()),
                    auditLog.getTargetName(),
                    auditLog.getDescription()
                ))
                .toList();
            return new AuditPageResponse<>(rows, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.hasNext());
        }

        List<MemberActivityAuditRow> rows = result.getContent().stream()
            .map(auditLog -> new MemberActivityAuditRow(
                auditLog.getTimestamp(),
                auditLog.getActorName(),
                humanizeAction(auditLog.getEvent()),
                auditLog.getTargetName(),
                auditLog.getDescription()
            ))
            .toList();
        return new AuditPageResponse<>(rows, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), result.hasNext());
    }

    @Transactional(readOnly = true)
    public AuditPageResponse<MemberStatusAuditRow> listOrganizationMemberStatusActivity(
        Long organizationId,
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
    ) {
        ensureSuperAdmin();
        organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        Instant fromTime = fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String pattern = normalizePattern(search);

        Page<AuditLog> result = auditLogRepository.findOrganizationAuditPage(
            organizationId,
            AuditActivityScope.MEMBER_STATUS_ACTIVITY,
            fromTime,
            toTime,
            pattern,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))
        );
        return new AuditPageResponse<>(
            result.getContent().stream()
                .map(auditLog -> new MemberStatusAuditRow(
                    auditLog.getTimestamp(),
                    auditLog.getActorName(),
                    auditLog.getTargetName(),
                    humanizeAction(auditLog.getEvent()),
                    auditLog.getDescription()
                ))
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public AuditPageResponse<AdminActivityAuditRow> listOrganizationAdminActivity(
        Long organizationId,
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
    ) {
        ensureSuperAdmin();
        organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        Instant fromTime = fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String pattern = normalizePattern(search);
        Collection<AuditActivityScope> scopes = List.of(
            AuditActivityScope.ADMIN_ACTIVITY,
            AuditActivityScope.GLOBAL_LOGIN
        );

        Page<AuditLog> result = auditLogRepository.findOrganizationAuditPageByScopes(
            organizationId,
            scopes,
            fromTime,
            toTime,
            pattern,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))
        );
        return new AuditPageResponse<>(
            result.getContent().stream()
                .map(auditLog -> new AdminActivityAuditRow(
                    auditLog.getTimestamp(),
                    auditLog.getActorName(),
                    humanizeAction(auditLog.getEvent()),
                    auditLog.getDescription()
                ))
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public NotificationAuditResponse listOrganizationNotificationActivity(
        Long organizationId,
        String search,
        NotificationChannel channel,
        NotificationStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
    ) {
        ensureSuperAdmin();
        organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        OffsetDateTime fromTime = fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        String pattern = normalizePattern(search);

        Page<NotificationLog> result = notificationLogRepository.findAuditNotificationPage(
            organizationId,
            channel,
            status,
            fromTime,
            toTime,
            pattern,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))
        );
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        OffsetDateTime dayStart = today.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime dayEnd = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime monthStart = today.withDayOfMonth(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime monthEnd = today.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        NotificationAuditSummary summary = new NotificationAuditSummary(
            notificationLogRepository.countByOrganizationIdAndDeletedFalseAndChannelAndStatus(
                organizationId,
                NotificationChannel.WHATSAPP,
                NotificationStatus.SENT
            ),
            notificationLogRepository.countByOrganizationIdAndDeletedFalseAndChannelAndStatus(
                organizationId,
                NotificationChannel.EMAIL,
                NotificationStatus.SENT
            ),
            notificationLogRepository.countByOrganizationIdAndDeletedFalseAndStatus(
                organizationId,
                NotificationStatus.FAILED
            ),
            notificationLogRepository.countByOrganizationIdAndDeletedFalseAndStatusAndCreatedAtBetween(
                organizationId,
                NotificationStatus.SENT,
                dayStart,
                dayEnd
            ),
            notificationLogRepository.countByOrganizationIdAndDeletedFalseAndStatusAndCreatedAtBetween(
                organizationId,
                NotificationStatus.SENT,
                monthStart,
                monthEnd
            )
        );

        AuditPageResponse<NotificationAuditRow> pageResponse = new AuditPageResponse<>(
            result.getContent().stream().map(this::toNotificationRow).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        );
        return new NotificationAuditResponse(pageResponse, summary);
    }

    private void ensureSuperAdmin() {
        if (SecurityUtils.currentUser().role() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only super admin can access audit logs");
        }
    }

    private String normalizePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase() + "%";
    }

    private AuditEventStatus toLoginStatus(LoginAuditTab tab) {
        return switch (tab) {
            case ALL -> null;
            case SUCCESSFUL -> AuditEventStatus.SUCCESS;
            case FAILED -> AuditEventStatus.FAILED;
            case SUSPICIOUS -> AuditEventStatus.SUSPICIOUS;
        };
    }

    private GlobalLoginAuditRow toGlobalLoginRow(AuditLog auditLog) {
        return new GlobalLoginAuditRow(
            auditLog.getTimestamp(),
            auditLog.getOrganization() == null ? null : auditLog.getOrganization().getId(),
            auditLog.getOrganization() == null ? "-" : auditLog.getOrganization().getName(),
            auditLog.getActorName(),
            auditLog.getActorEmail(),
            auditLog.getDevice(),
            auditLog.getBrowser(),
            auditLog.getIpAddress(),
            auditLog.getEventStatus().name(),
            auditLog.getDescription()
        );
    }

    private NotificationAuditRow toNotificationRow(NotificationLog notificationLog) {
        return new NotificationAuditRow(
            notificationLog.getCreatedAt() == null ? null : notificationLog.getCreatedAt().toInstant(),
            notificationLog.getChannel(),
            notificationLog.getRecipient(),
            notificationLog.getStatus(),
            notificationDescription(notificationLog)
        );
    }

    private String notificationDescription(NotificationLog notificationLog) {
        String type = notificationLog.getType() == null ? "Notification" : humanizeAction(notificationLog.getType().name());
        String reason = notificationLog.getFailureReason();
        if (reason == null || reason.isBlank()) {
            return type;
        }
        return type + " (" + reason + ")";
    }

    private String humanizeAction(String event) {
        if (event == null || event.isBlank()) {
            return "-";
        }
        String normalized = event.replace('_', ' ').trim().toLowerCase();
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String safeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public enum LoginAuditTab {
        ALL,
        SUCCESSFUL,
        FAILED,
        SUSPICIOUS
    }

    public enum OrganizationAuditTab {
        MEMBER_ACTIVITY,
        MEMBER_STATUS_ACTIVITY,
        NOTIFICATION_ACTIVITY,
        ADMIN_ACTIVITY
    }
}
