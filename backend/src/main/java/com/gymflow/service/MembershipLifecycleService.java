package com.gymflow.service;

import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Member;
import com.gymflow.domain.enums.MembershipDuration;
import com.gymflow.domain.enums.MembershipStatus;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class MembershipLifecycleService {

    public void refreshMembership(Member member, int reminderDaysBeforeExpiry) {
        if (member.getJoiningDate() == null) {
            throw new BadRequestException("Joining date is required");
        }
        if (member.getMembershipDuration() == null) {
            throw new BadRequestException("Membership duration is required");
        }

        member.setMembershipStartDate(member.getJoiningDate());
        member.setMembershipEndDate(calculateMembershipEndDate(
            member.getJoiningDate(),
            member.getMembershipDuration(),
            member.getCustomDurationDays()
        ));
        member.setMembershipStatus(resolveMembershipStatus(
            LocalDate.now(),
            member.getMembershipEndDate(),
            reminderDaysBeforeExpiry,
            member.getOverdueMarkedAt() != null
        ));
    }

    public LocalDate calculateMembershipEndDate(
        LocalDate joiningDate,
        MembershipDuration duration,
        Integer customDurationDays
    ) {
        if (duration.isCustom()) {
            if (customDurationDays == null || customDurationDays <= 0) {
                throw new BadRequestException("Custom duration days must be greater than zero");
            }
            return joiningDate.plusDays(customDurationDays);
        }
        return joiningDate.plusMonths(duration.months());
    }

    public MembershipStatus resolveMembershipStatus(
        LocalDate today,
        LocalDate membershipEndDate,
        int reminderDaysBeforeExpiry,
        boolean overdueMarked
    ) {
        if (membershipEndDate.isBefore(today)) {
            return overdueMarked ? MembershipStatus.OVERDUE : MembershipStatus.EXPIRED;
        }

        LocalDate reminderDate = membershipEndDate.minusDays(Math.max(reminderDaysBeforeExpiry, 0));
        if (!today.isBefore(reminderDate)) {
            return MembershipStatus.EXPIRING_SOON;
        }

        return MembershipStatus.ACTIVE;
    }
}
