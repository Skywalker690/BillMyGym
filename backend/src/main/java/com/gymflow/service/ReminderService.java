package com.gymflow.service;

import com.gymflow.domain.Member;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationSettingRepository;
import com.gymflow.repository.UsageMetricRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {

    private final MemberRepository memberRepository;
    private final OrganizationSettingRepository settingRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final UsageMetricRepository usageMetricRepository;

    @Value("${gymflow.reminder.pre-expiry-days:1}")
    private int defaultPreExpiryDays;

    public ReminderService(
        MemberRepository memberRepository,
        OrganizationSettingRepository settingRepository,
        NotificationLogRepository notificationLogRepository,
        UsageMetricRepository usageMetricRepository
    ) {
        this.memberRepository = memberRepository;
        this.settingRepository = settingRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.usageMetricRepository = usageMetricRepository;
    }

    @Scheduled(cron = "0 0 10 * * *")
    @Transactional
    public void processExpiryReminders() {
        LocalDate thresholdDate = LocalDate.now().plusDays(defaultPreExpiryDays);
        List<Member> members = memberRepository.findByMembershipEndDateLessThanEqualAndDeletedFalse(thresholdDate);

        for (Member member : members) {
            Organization organization = member.getOrganization();
            OrganizationSetting setting = settingRepository.findByOrganizationIdAndDeletedFalse(organization.getId())
                .orElse(null);
            if (setting == null) {
                continue;
            }

            UsageMetric metric = usageMetricRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
                organization.getId(),
                YearMonth.now().getYear(),
                YearMonth.now().getMonthValue()
            ).orElseGet(() -> UsageMetric.empty(organization, YearMonth.now()));

            if (setting.isWhatsappEnabled()) {
                notificationLogRepository.save(buildLog(member, NotificationChannel.WHATSAPP));
                metric.incrementWhatsappMessagesSent();
            }
            if (setting.isEmailEnabled() && member.getEmail() != null && !member.getEmail().isBlank()) {
                notificationLogRepository.save(buildLog(member, NotificationChannel.EMAIL));
                metric.incrementEmailsSent();
            }

            metric.setActiveMembers((int) memberRepository.countByOrganizationIdAndDeletedFalse(organization.getId()));
            usageMetricRepository.save(metric);
        }
    }

    private NotificationLog buildLog(Member member, NotificationChannel channel) {
        NotificationLog log = new NotificationLog();
        log.setOrganization(member.getOrganization());
        log.setMember(member);
        log.setType(NotificationType.MEMBERSHIP_EXPIRY);
        log.setChannel(channel);
        log.setRecipient(channel == NotificationChannel.WHATSAPP ? member.getPhoneNumber() : member.getEmail());
        log.setStatus(NotificationStatus.QUEUED.name());
        log.setMessage("Your gym membership is near expiry. Please renew.");
        return log;
    }
}
