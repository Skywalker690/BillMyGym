package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.PaymentCoverageSummary;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MembershipLifecycleService {

    public void refreshMembership(MemberMembership membership, int reminderDaysBeforeExpiry) {
        if (membership.getStatus() == MembershipStatus.COMPLETED) {
            return;
        }
        membership.setStatus(resolveMembershipStatus(
            LocalDate.now(),
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
        if (membershipEndDate.isEqual(today)) {
            return MembershipStatus.EXPIRED;
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
        BigDecimal paidAmount = successfulPaymentTotal(payments);
        BigDecimal pendingAmount = membership.getPlanAmount().subtract(paidAmount).max(BigDecimal.ZERO);
        PaymentStatus paymentStatus = resolveCoverageStatus(membership.getPlanAmount(), paidAmount);
        return new PaymentCoverageSummary(
            membership.getPlanAmount(),
            paidAmount,
            pendingAmount,
            paymentStatus,
            isExpiringSoon(LocalDate.now(), membership.getEndDate(), reminderDaysBeforeExpiry)
        );
    }

    public BigDecimal successfulPaymentTotal(List<MemberPayment> payments) {
        return payments.stream()
            .filter(payment -> payment.getPaymentStatus() == PaymentStatus.PAID || payment.getPaymentStatus() == PaymentStatus.PARTIAL)
            .map(MemberPayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public PaymentStatus resolveCoverageStatus(BigDecimal planAmount, BigDecimal paidAmount) {
        if (paidAmount.compareTo(planAmount) >= 0) {
            return PaymentStatus.PAID;
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return PaymentStatus.PARTIAL;
        }
        return PaymentStatus.PENDING;
    }
}
