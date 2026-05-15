package com.gymflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipReminderScheduler {

    private final ReminderService reminderService;

    @Scheduled(cron = "0 0 10 * * *")
    public void runDailyReminders() {
        reminderService.runReminders(null);
    }
}
