package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private MemberMembershipRepository memberMembershipRepository;

    @Mock
    private MemberMembershipService memberMembershipService;

    @Mock
    private OrganizationSettingService organizationSettingService;

    @Mock
    private OrganizationDigestBuilderService organizationDigestBuilderService;

    @Mock
    private NotificationDispatcherService notificationDispatcherService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private ReportCacheService reportCacheService;

    @Test
    void marksMembershipsAsSentWhenDigestDispatchSucceeds() {
        ReminderService reminderService = new ReminderService(
            memberMembershipRepository,
            memberMembershipService,
            organizationSettingService,
            organizationDigestBuilderService,
            notificationDispatcherService,
            organizationRepository,
            notificationLogRepository,
            reportCacheService
        );

        Organization organization = new Organization();
        organization.setStatus(OrganizationStatus.ACTIVE);
        setId(organization, 11L);

        Member member = new Member();
        member.setStatus(MemberStatus.ACTIVE);
        member.setOrganization(organization);
        setId(member, 12L);

        MemberMembership membership = new MemberMembership();
        membership.setOrganization(organization);
        membership.setMember(member);
        membership.setPlanAmount(new BigDecimal("1000.00"));
        membership.setStartDate(LocalDate.of(2026, 5, 1));
        membership.setEndDate(LocalDate.of(2026, 5, 15));
        membership.setStatus(MembershipStatus.OVERDUE);
        setId(membership, 13L);

        OrganizationSetting setting = new OrganizationSetting();
        when(memberMembershipRepository.findReminderCandidatesForDay(
            org.mockito.ArgumentMatchers.eq(11L),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        ))
            .thenReturn(List.of(membership));
        when(notificationLogRepository.existsByOrganizationIdAndNotificationDateAndTypeAndStatusAndDeletedFalse(
            org.mockito.ArgumentMatchers.eq(11L),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(NotificationType.ORGANIZATION_RENEWAL_DIGEST),
            org.mockito.ArgumentMatchers.eq(NotificationStatus.SENT)
        )).thenReturn(false);
        when(notificationLogRepository.countByOrganizationIdAndTypeAndDeletedFalseAndCreatedAtBetween(
            org.mockito.ArgumentMatchers.eq(11L),
            org.mockito.ArgumentMatchers.eq(NotificationType.MANUAL_REMINDER_TRIGGER),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(0L);
        when(organizationRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(organization));
        when(organizationSettingService.getOrCreateSettings(11L)).thenReturn(setting);
        when(organizationDigestBuilderService.buildDigest(org.mockito.ArgumentMatchers.eq(organization), org.mockito.ArgumentMatchers.eq(setting), org.mockito.ArgumentMatchers.anyList()))
            .thenReturn(new OrganizationDigestBuilderService.DigestContent("Brand", "wa", "subject", "email", 1));
        when(notificationDispatcherService.dispatchDigest(org.mockito.ArgumentMatchers.eq(organization), org.mockito.ArgumentMatchers.eq(setting), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new NotificationDispatcherService.DispatchResult(true, 1));

        var response = reminderService.runReminders(11L);

        assertEquals(1, response.processedMembers());
        assertEquals(1, response.organizationsGrouped());
        assertEquals(1, response.digestsSent());
        verify(memberMembershipService).refreshMembershipStatuses(11L);
        verify(memberMembershipService).markReminderSent(org.mockito.ArgumentMatchers.eq(List.of(membership)), org.mockito.ArgumentMatchers.any());
    }

    private void setId(Object target, Long id) {
        try {
            var field = target.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
