package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.PaymentCoverageSummary;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MembershipLifecycleService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DUE_TODAY_WINDOW_DAYS = 0;

    public void refreshMembership(MemberMembership membership, int reminderDaysBeforeExpiry) {
        if (membership.getStatus() == MembershipStatus.OVERDUE) {
            return;
        }
        membership.setStatus(resolveMembershipStatus(
            LocalDate.now(APP_ZONE),
            membership.getEndDate(),
            reminderDaysBeforeExpiry
        ));
    }

    public LocalDate calculateMembershipEndDate(LocalDate startDate, int durationMonths) {
        if (startDate == null) {
            throw new BadRequestException("Membership start date is required");
        }
        if (durationMonths <= 0) {
            throw new BadRequestException("Membership duration months must be greater than zero");
        }
        // BUG-fix: Use 30-day periods instead of calendar months so membership
        // duration is always deterministic: 1 month = 30 days, 2 months = 60 days, etc.
        return startDate.plusDays((long) durationMonths * 30).minusDays(1);
    }

    public MembershipStatus resolveMembershipStatus(LocalDate today, LocalDate membershipEndDate, int reminderDaysBeforeExpiry) {
        if (membershipEndDate.isBefore(today)) {
            return MembershipStatus.OVERDUE;
        }
        return MembershipStatus.ACTIVE;
    }

    public boolean isExpiringSoon(LocalDate today, LocalDate membershipEndDate, int reminderDaysBeforeExpiry) {
        if (membershipEndDate == null || membershipEndDate.isBefore(today)) {
            return false;
        }
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, membershipEndDate);
        // BUG-28: Use the passed-in reminderDaysBeforeExpiry instead of the hardcoded DUE_TODAY_WINDOW_DAYS=0
        // so that "expiring soon" covers the configured look-ahead window, not just the exact expiry day.
        return daysUntilExpiry <= reminderDaysBeforeExpiry;
    }

    public PaymentCoverageSummary buildCoverageSummary(
        MemberMembership membership,
        List<MemberPayment> payments,
        int reminderDaysBeforeExpiry
    ) {
        return buildCoverageSummary(membership, payments, BigDecimal.ZERO, reminderDaysBeforeExpiry);
    }

    public PaymentCoverageSummary buildCoverageSummary(
        MemberMembership membership,
        List<MemberPayment> payments,
        BigDecimal appliedCreditAmount,
        int reminderDaysBeforeExpiry
    ) {
        return buildCoverageSummary(
            membership,
            successfulPaymentTotal(payments),
            appliedCreditAmount,
            reminderDaysBeforeExpiry
        );
    }

    public PaymentCoverageSummary buildCoverageSummary(
        MemberMembership membership,
        BigDecimal paidAmount,
        BigDecimal appliedCreditAmount,
        int reminderDaysBeforeExpiry
    ) {
        BigDecimal normalizedAppliedCredit = appliedCreditAmount == null ? BigDecimal.ZERO : appliedCreditAmount;
        BigDecimal coveredAmount = paidAmount.add(normalizedAppliedCredit);
        BigDecimal pendingAmount = membership.getPlanAmount().subtract(coveredAmount).max(BigDecimal.ZERO);
        PaymentStatus paymentStatus = resolveCoverageStatus(membership.getPlanAmount(), coveredAmount);
        return new PaymentCoverageSummary(
            membership.getPlanAmount(),
            paidAmount,
            normalizedAppliedCredit,
            pendingAmount,
            paymentStatus,
            isExpiringSoon(LocalDate.now(APP_ZONE), membership.getEndDate(), reminderDaysBeforeExpiry)
        );
    }

    public BigDecimal successfulPaymentTotal(List<MemberPayment> payments) {
        return payments.stream()
            .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PAID)
            .filter(this::isMembershipCoveragePayment)
            .map(MemberPayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public PaymentStatus resolveCoverageStatus(BigDecimal planAmount, BigDecimal paidAmount) {
        if (paidAmount.compareTo(planAmount) >= 0) {
            return PaymentStatus.PAID;
        }
        return PaymentStatus.PENDING;
    }

    private boolean isMembershipCoveragePayment(MemberPayment payment) {
        return payment.getPaymentType() == PaymentType.MEMBERSHIP
            || payment.getPaymentType() == PaymentType.NEW_MEMBERSHIP
            || payment.getPaymentType() == PaymentType.RENEWAL;
    }
}
