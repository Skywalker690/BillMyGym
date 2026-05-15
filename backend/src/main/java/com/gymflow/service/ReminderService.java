package com.gymflow.service;

import com.gymflow.api.dto.ReminderDtos.ReminderRunResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Member;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.repository.MemberRepository;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final MemberRepository memberRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final OrganizationSettingService organizationSettingService;
    private final NotificationDispatcherService notificationDispatcherService;

    @Transactional
    public ReminderRunResponse runRemindersForCurrentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return runReminders(organizationId);
    }

    @Transactional
    public ReminderRunResponse runReminders(Long organizationId) {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(30);
        List<Member> members = memberRepository.findReminderCandidates(thresholdDate, organizationId);

        int processedMembers = 0;
        int reminderEventsTriggered = 0;
        int notificationsSent = 0;

        for (Member member : members) {
            Organization organization = member.getOrganization();
            if (organization.getStatus() != OrganizationStatus.ACTIVE) {
                continue;
            }

            OrganizationSetting setting = organizationSettingService.getOrCreateSettings(organization.getId());
            membershipLifecycleService.refreshMembership(member, setting.getReminderDaysBeforeExpiry());
            processedMembers++;

            if (shouldSendExpiryReminder(member, today, setting)) {
                NotificationDispatcherService.DispatchResult result = notificationDispatcherService.dispatch(
                    member,
                    setting,
                    NotificationType.MEMBERSHIP_EXPIRY
                );
                if (result.reminderTriggered()) {
                    reminderEventsTriggered++;
                    notificationsSent += result.notificationsSent();
                }
                continue;
            }

            if (shouldSendOverdueReminder(member, today)) {
                NotificationDispatcherService.DispatchResult result = notificationDispatcherService.dispatch(
                    member,
                    setting,
                    NotificationType.MEMBERSHIP_OVERDUE
                );
                if (result.reminderTriggered()) {
                    reminderEventsTriggered++;
                    notificationsSent += result.notificationsSent();
                    member.setOverdueMarkedAt(OffsetDateTime.now());
                    member.setMembershipStatus(MembershipStatus.OVERDUE);
                }
            }
        }

        return new ReminderRunResponse(processedMembers, reminderEventsTriggered, notificationsSent);
    }

    private boolean shouldSendExpiryReminder(Member member, LocalDate today, OrganizationSetting setting) {
        return member.getMembershipEndDate().equals(today.plusDays(setting.getReminderDaysBeforeExpiry()))
            && member.getMembershipStatus() == MembershipStatus.EXPIRING_SOON;
    }

    private boolean shouldSendOverdueReminder(Member member, LocalDate today) {
        return member.getMembershipEndDate().isBefore(today)
            && member.getMembershipStatus() == MembershipStatus.EXPIRED
            && member.getOverdueMarkedAt() == null;
    }
}
