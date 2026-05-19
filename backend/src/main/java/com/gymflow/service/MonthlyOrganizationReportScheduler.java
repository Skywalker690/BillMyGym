package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.repository.OrganizationRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyOrganizationReportScheduler {

    private final OrganizationRepository organizationRepository;
    private final PlanAccessService planAccessService;
    private final MonthlyOrganizationReportService reportService;
    private final EmailService emailService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;
    private final NotificationDeliveryLogService notificationDeliveryLogService;

    @Transactional
    @Scheduled(cron = "0 0 1 1 * *", zone = "Asia/Kolkata")
    public void sendPreviousMonthReports() {
        LocalDate today = LocalDate.now();
        YearMonth reportMonth = YearMonth.from(today.minusMonths(1));
        List<Organization> organizations = organizationRepository.findAllByDeletedFalseAndStatusIn(
            List.of(OrganizationStatus.ACTIVE, OrganizationStatus.TRIAL)
        );
        for (Organization organization : organizations) {
            if (!planAccessService.canReceiveMonthlyReports(organization)) {
                continue;
            }
            if (notificationDeliveryLogService.hasSuccessfulDeliveryForDay(
                organization.getId(),
                NotificationType.MONTHLY_ORGANIZATION_REPORT,
                today
            )) {
                continue;
            }
            try {
                MonthlyOrganizationReportService.MonthlyOrganizationReport report = reportService.buildReport(
                    organization,
                    reportMonth
                );
                emailService.sendMonthlyOrganizationReport(organization, report);
                whatsAppDeliveryService.sendMonthlyReport(organization, report);
                notificationDeliveryLogService.recordSuccessfulDelivery(
                    organization,
                    NotificationType.MONTHLY_ORGANIZATION_REPORT
                );
            } catch (RuntimeException ex) {
                log.warn("Failed to send monthly report for organization {}: {}", organization.getId(), ex.getMessage());
            }
        }
    }
}
