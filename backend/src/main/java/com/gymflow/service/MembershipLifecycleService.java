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

    public void refreshMembership(MemberMembership membership, int reminderDaysBeforeExpiry) {
        if (membership.getStatus() == MembershipStatus.DEACTIVATED) {
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
        return startDate.plusMonths(durationMonths).minusDays(1);
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
        return daysUntilExpiry <= Math.max(reminderDaysBeforeExpiry, 0);
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
        BigDecimal paidAmount = successfulPaymentTotal(payments);
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
