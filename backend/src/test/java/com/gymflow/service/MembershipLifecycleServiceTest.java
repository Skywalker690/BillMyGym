package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gymflow.domain.enums.MembershipDuration;
import com.gymflow.domain.enums.MembershipStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MembershipLifecycleServiceTest {

    private final MembershipLifecycleService membershipLifecycleService = new MembershipLifecycleService();

    @Test
    void calculatesMonthBasedMembershipEndDate() {
        LocalDate result = membershipLifecycleService.calculateMembershipEndDate(
            LocalDate.of(2026, 1, 15),
            MembershipDuration.THREE_MONTHS,
            null
        );

        assertEquals(LocalDate.of(2026, 4, 15), result);
    }

    @Test
    void marksMemberAsExpiringSoonInsideReminderWindow() {
        MembershipStatus status = membershipLifecycleService.resolveMembershipStatus(
            LocalDate.of(2026, 5, 16),
            LocalDate.of(2026, 5, 17),
            1,
            false
        );

        assertEquals(MembershipStatus.EXPIRING_SOON, status);
    }

    @Test
    void marksExpiredMemberAsOverdueAfterOverdueWorkflowStarts() {
        MembershipStatus status = membershipLifecycleService.resolveMembershipStatus(
            LocalDate.of(2026, 5, 16),
            LocalDate.of(2026, 5, 15),
            1,
            true
        );

        assertEquals(MembershipStatus.OVERDUE, status);
    }
}
