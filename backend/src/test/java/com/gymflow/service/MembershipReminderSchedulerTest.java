package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class MembershipReminderSchedulerTest {

    @Test
    void schedulerUsesTrueFortyEightHourInterval() throws NoSuchMethodException {
        Method method = MembershipReminderScheduler.class.getMethod("runExpiredMemberRemindersEveryTwoDays");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals(172_800_000L, scheduled.fixedRate());
        assertEquals("", scheduled.cron());
    }

    @Test
    void scheduledRunDelegatesToExpiredMemberReminderWorkflow() {
        ReminderService reminderService = org.mockito.Mockito.mock(ReminderService.class);
        MembershipReminderScheduler scheduler = new MembershipReminderScheduler(reminderService);

        scheduler.runExpiredMemberRemindersEveryTwoDays();

        verify(reminderService).runScheduledExpiredMemberReminders();
    }
}
