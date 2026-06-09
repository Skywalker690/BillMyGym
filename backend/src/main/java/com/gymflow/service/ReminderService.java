package com.gymflow.service;

import com.gymflow.api.dto.ReminderDtos.ReminderRunResponse;
import com.gymflow.api.dto.ReminderDtos.ManualReminderQuotaResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int MANUAL_REMINDER_RUN_LIMIT_PER_MONTH = 20;

    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberMembershipService memberMembershipService;
    private final OrganizationSettingService organizationSettingService;
    private final OrganizationDigestBuilderService organizationDigestBuilderService;
    private final NotificationDispatcherService notificationDispatcherService;
    private final OrganizationRepository organizationRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final ReportCacheService reportCacheService;

    @Transactional
    public ReminderRunResponse runRemindersForCurrentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return runReminders(organizationId, true);
    }

    @Transactional
    public ReminderRunResponse runReminders(Long organizationId) {
        return runReminders(organizationId, false);
    }

    @Transactional
    public ReminderRunResponse runReminders(Long organizationId, boolean manualTrigger) {
        ManualReminderQuotaResponse quota = manualQuota(organizationId);
        if (manualTrigger && !quota.canRun()) {
            throw new BadRequestException("Monthly manual reminder run limit reached");
        }
        memberMembershipService.refreshMembershipStatuses(organizationId);

        OffsetDateTime dayStart = LocalDate.now(APP_ZONE)
            .atStartOfDay(APP_ZONE)
            .toOffsetDateTime();
        OrganizationSetting setting = organizationSettingService.getOrCreateSettings(organizationId);
        LocalDate today = LocalDate.now(APP_ZONE);
        List<MemberMembership> candidates = memberMembershipRepository.findExpiredReminderCandidatesForDay(
                organizationId,
                dayStart,
                today
            )
            .stream()
            .filter(membership -> membership.getOrganization().getStatus() == OrganizationStatus.ACTIVE
                || membership.getOrganization().getStatus() == OrganizationStatus.TRIAL)
            .toList();
        if (manualTrigger) {
            recordManualReminderTrigger(organizationId, candidates.size());
            quota = manualQuota(organizationId);
        }
        if (notificationLogRepository.existsByOrganizationIdAndNotificationDateAndTypeAndStatusAndDeletedFalse(
            organizationId,
            LocalDate.now(APP_ZONE),
            NotificationType.ORGANIZATION_RENEWAL_DIGEST,
            NotificationStatus.SENT
        )) {
            return new ReminderRunResponse(candidates.size(), 0, 0, quota);
        }
        Map<Organization, List<MemberMembership>> membershipsByOrganization = candidates.stream()
            .collect(Collectors.groupingBy(MemberMembership::getOrganization));

        int digestsSent = 0;

        for (Map.Entry<Organization, List<MemberMembership>> entry : membershipsByOrganization.entrySet()) {
            Organization organization = entry.getKey();
            List<MemberMembership> memberships = entry.getValue();
            OrganizationDigestBuilderService.DigestContent digestContent =
                organizationDigestBuilderService.buildDigest(organization, setting, memberships);

            NotificationDispatcherService.DispatchResult result = notificationDispatcherService.dispatchDigest(
                organization,
                setting,
                digestContent
            );
            if (result.digestTriggered()) {
                memberMembershipService.markReminderSent(memberships, OffsetDateTime.now(APP_ZONE));
                digestsSent += result.digestsSent();
            }
        }

        reportCacheService.evictOrganization(organizationId);
        return new ReminderRunResponse(
            candidates.size(),
            membershipsByOrganization.size(),
            digestsSent,
            quota
        );
    }

    @Transactional
    public ReminderRunResponse runScheduledExpiredMemberReminders() {
        int processedMemberships = 0;
        int organizationsGrouped = 0;
        int digestsSent = 0;
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndStatusIn(
            List.of(OrganizationStatus.ACTIVE, OrganizationStatus.TRIAL)
        )) {
            ReminderRunResponse response = runReminders(organization.getId(), false);
            processedMemberships += response.processedMembers();
            organizationsGrouped += response.organizationsGrouped();
            digestsSent += response.digestsSent();
        }
        return new ReminderRunResponse(
            processedMemberships,
            organizationsGrouped,
            digestsSent,
            new ManualReminderQuotaResponse(
                MANUAL_REMINDER_RUN_LIMIT_PER_MONTH,
                0,
                MANUAL_REMINDER_RUN_LIMIT_PER_MONTH,
                true,
                false
            )
        );
    }

    @Transactional
    public ManualReminderQuotaResponse manualQuotaForCurrentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return manualQuota(organizationId);
    }

    private ManualReminderQuotaResponse manualQuota(Long organizationId) {
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new BadRequestException("Organization not found"));
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            int used = countManualTriggersThisMonth(organizationId);
            return new ManualReminderQuotaResponse(
                Integer.MAX_VALUE,
                used,
                Integer.MAX_VALUE,
                true,
                true
            );
        }
        int used = countManualTriggersThisMonth(organizationId);
        int remaining = Math.max(MANUAL_REMINDER_RUN_LIMIT_PER_MONTH - used, 0);
        return new ManualReminderQuotaResponse(
            MANUAL_REMINDER_RUN_LIMIT_PER_MONTH,
            used,
            remaining,
            remaining > 0,
            false
        );
    }

    private int countManualTriggersThisMonth(Long organizationId) {
        OffsetDateTime monthStart = LocalDate.now(APP_ZONE)
            .withDayOfMonth(1)
            .atStartOfDay(APP_ZONE)
            .toOffsetDateTime();
        OffsetDateTime nextMonthStart = monthStart.plusMonths(1);
        return Math.toIntExact(notificationLogRepository.countByOrganizationIdAndTypeAndDeletedFalseAndCreatedAtBetween(
            organizationId,
            NotificationType.MANUAL_REMINDER_TRIGGER,
            monthStart,
            nextMonthStart
        ));
    }

    private void recordManualReminderTrigger(Long organizationId, int candidateCount) {
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new BadRequestException("Organization not found"));
        NotificationLog log = new NotificationLog();
        log.setOrganization(organization);
        log.setMemberCount(candidateCount);
        log.setType(NotificationType.MANUAL_REMINDER_TRIGGER);
        log.setChannel(NotificationChannel.EMAIL);
        log.setRecipient("MANUAL_TRIGGER");
        log.setStatus(NotificationStatus.SENT);
        log.setAttemptCount(1);
        log.setNotificationDate(LocalDate.now(APP_ZONE));
        log.setSentAt(OffsetDateTime.now(APP_ZONE));
        log.setMessage("Manual reminder control-page trigger");
        notificationLogRepository.save(log);
    }
}
