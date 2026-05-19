package com.gymflow.service;

import com.gymflow.api.dto.ReminderDtos.ReminderRunResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberMembershipService memberMembershipService;
    private final OrganizationSettingService organizationSettingService;
    private final OrganizationDigestBuilderService organizationDigestBuilderService;
    private final NotificationDispatcherService notificationDispatcherService;
    private final OrganizationRepository organizationRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public ReminderRunResponse runRemindersForCurrentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return runReminders(organizationId);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public ReminderRunResponse runReminders(Long organizationId) {
        memberMembershipService.refreshMembershipStatuses(organizationId);

        OffsetDateTime dayStart = LocalDate.now(APP_ZONE)
            .atStartOfDay(APP_ZONE)
            .toOffsetDateTime();
        List<MemberMembership> candidates = memberMembershipRepository.findReminderCandidatesForDay(organizationId, dayStart).stream()
            .filter(membership -> membership.getOrganization().getStatus() == OrganizationStatus.ACTIVE
                || membership.getOrganization().getStatus() == OrganizationStatus.TRIAL)
            .toList();
        if (notificationLogRepository.existsByOrganizationIdAndNotificationDateAndTypeAndDeletedFalse(
            organizationId,
            LocalDate.now(APP_ZONE),
            NotificationType.ORGANIZATION_RENEWAL_DIGEST
        )) {
            return new ReminderRunResponse(candidates.size(), 0, 0);
        }
        Map<Organization, List<MemberMembership>> membershipsByOrganization = candidates.stream()
            .collect(Collectors.groupingBy(MemberMembership::getOrganization));

        int digestsSent = 0;

        for (Map.Entry<Organization, List<MemberMembership>> entry : membershipsByOrganization.entrySet()) {
            Organization organization = entry.getKey();
            List<MemberMembership> memberships = entry.getValue();
            OrganizationSetting setting = organizationSettingService.getOrCreateSettings(organization.getId());
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

        return new ReminderRunResponse(
            candidates.size(),
            membershipsByOrganization.size(),
            digestsSent
        );
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public ReminderRunResponse runDailyReminders() {
        int processedMemberships = 0;
        int organizationsGrouped = 0;
        int digestsSent = 0;
        for (Organization organization : organizationRepository.findAllByDeletedFalseAndStatusIn(
            List.of(OrganizationStatus.ACTIVE, OrganizationStatus.TRIAL)
        )) {
            ReminderRunResponse response = runReminders(organization.getId());
            processedMemberships += response.processedMembers();
            organizationsGrouped += response.organizationsGrouped();
            digestsSent += response.digestsSent();
        }
        return new ReminderRunResponse(processedMemberships, organizationsGrouped, digestsSent);
    }
}
