package com.gymflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MembershipReminderScheduler {

    private final ReminderService reminderService;

    @Transactional
    @Scheduled(cron = "0 0 10 */2 * *", zone = "Asia/Kolkata")
    public void runExpiredMemberRemindersEveryTwoDays() {
        reminderService.runScheduledExpiredMemberReminders();
    }
}
