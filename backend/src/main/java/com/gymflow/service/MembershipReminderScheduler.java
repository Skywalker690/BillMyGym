package com.gymflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MembershipReminderScheduler {

    private static final long TWO_DAYS_IN_MILLISECONDS = 2L * 24L * 60L * 60L * 1000L;

    private final ReminderService reminderService;

    @Transactional
    @Scheduled(fixedRate = TWO_DAYS_IN_MILLISECONDS)
    public void runExpiredMemberRemindersEveryTwoDays() {
        reminderService.runScheduledExpiredMemberReminders();
    }
}
