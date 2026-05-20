package com.gymflow.api;

import com.gymflow.api.response.ApiResponse;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.service.AuditQueryService;
import com.gymflow.service.AuditQueryService.LoginAuditTab;
import com.gymflow.service.AuditQueryService.OrganizationAuditTab;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditQueryService auditQueryService;

    @GetMapping("/audit-logs/login")
    public ResponseEntity<?> globalLoginAudit(
        @RequestParam(defaultValue = "ALL") LoginAuditTab tab,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long organizationId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Global login audit fetched successfully",
            auditQueryService.listGlobalLoginAudit(tab, search, organizationId, fromDate, toDate, page, size)
        ));
    }

    @GetMapping("/audit-logs/login/export")
    public ResponseEntity<byte[]> exportGlobalLoginAudit(
        @RequestParam(defaultValue = "ALL") LoginAuditTab tab,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long organizationId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        byte[] csv = auditQueryService.exportGlobalLoginAuditCsv(tab, search, organizationId, fromDate, toDate);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=global-login-audit.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    @GetMapping("/organizations/{organizationId}/audit-logs/member-activity")
    public ResponseEntity<?> organizationMemberActivity(
        @PathVariable Long organizationId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization member activity audit fetched successfully",
            auditQueryService.listOrganizationMemberActivity(
                organizationId,
                OrganizationAuditTab.MEMBER_ACTIVITY,
                search,
                fromDate,
                toDate,
                page,
                size
            )
        ));
    }

    @GetMapping("/organizations/{organizationId}/audit-logs/member-status")
    public ResponseEntity<?> organizationMemberStatusActivity(
        @PathVariable Long organizationId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization member status audit fetched successfully",
            auditQueryService.listOrganizationMemberStatusActivity(organizationId, search, fromDate, toDate, page, size)
        ));
    }

    @GetMapping("/organizations/{organizationId}/audit-logs/admin-activity")
    public ResponseEntity<?> organizationAdminActivity(
        @PathVariable Long organizationId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization admin activity audit fetched successfully",
            auditQueryService.listOrganizationAdminActivity(organizationId, search, fromDate, toDate, page, size)
        ));
    }

    @GetMapping("/organizations/{organizationId}/audit-logs/notification-activity")
    public ResponseEntity<?> organizationNotificationActivity(
        @PathVariable Long organizationId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) NotificationChannel channel,
        @RequestParam(required = false) NotificationStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization notification audit fetched successfully",
            auditQueryService.listOrganizationNotificationActivity(
                organizationId,
                search,
                channel,
                status,
                fromDate,
                toDate,
                page,
                size
            )
        ));
    }
}
