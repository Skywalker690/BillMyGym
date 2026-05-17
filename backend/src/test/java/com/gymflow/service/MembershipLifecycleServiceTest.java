package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gymflow.api.dto.MembershipDtos.PaymentCoverageSummary;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MembershipLifecycleServiceTest {

    private final MembershipLifecycleService membershipLifecycleService = new MembershipLifecycleService();

    @Test
    void calculatesMonthBasedMembershipEndDate() {
        LocalDate result = membershipLifecycleService.calculateMembershipEndDate(
            LocalDate.of(2026, 1, 15),
            3
        );

        assertEquals(LocalDate.of(2026, 4, 14), result);
    }

    @Test
    void treatsUpcomingExpiryAsDynamicIndicatorNotPersistedStatus() {
        MembershipStatus status = membershipLifecycleService.resolveMembershipStatus(
            LocalDate.of(2026, 5, 16),
            LocalDate.of(2026, 5, 17),
            1
        );

        assertEquals(MembershipStatus.ACTIVE, status);
        assertEquals(true, membershipLifecycleService.isExpiringSoon(
            LocalDate.of(2026, 5, 16),
            LocalDate.of(2026, 5, 17),
            1
        ));
    }

    @Test
    void marksExpiredMembershipAsOverdueAfterExpiryDate() {
        MembershipStatus status = membershipLifecycleService.resolveMembershipStatus(
            LocalDate.of(2026, 5, 16),
            LocalDate.of(2026, 5, 15),
            1
        );

        assertEquals(MembershipStatus.OVERDUE, status);
    }

    @Test
    void marksMembershipAsExpiredOnExpiryDate() {
        MembershipStatus status = membershipLifecycleService.resolveMembershipStatus(
            LocalDate.of(2026, 5, 16),
            LocalDate.of(2026, 5, 16),
            1
        );

        assertEquals(MembershipStatus.EXPIRED, status);
    }

    @Test
    void buildsPartialCoverageSummary() {
        MemberMembership membership = new MemberMembership();
        membership.setPlanAmount(new BigDecimal("3600.00"));

        MemberPayment payment = new MemberPayment();
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setPaymentStatus(PaymentStatus.PARTIAL);

        membership.setEndDate(LocalDate.now().plusDays(10));
        PaymentCoverageSummary summary = membershipLifecycleService.buildCoverageSummary(membership, List.of(payment), 3);

        assertEquals(new BigDecimal("1000.00"), summary.paidAmount());
        assertEquals(new BigDecimal("2600.00"), summary.pendingAmount());
        assertEquals(PaymentStatus.PARTIAL, summary.paymentStatus());
        assertEquals(false, summary.expiringSoon());
    }

    @Test
    void buildsPaidCoverageSummaryForPrepaidMembership() {
        MemberMembership membership = new MemberMembership();
        membership.setPlanAmount(new BigDecimal("12000.00"));

        MemberPayment payment = new MemberPayment();
        payment.setAmount(new BigDecimal("12000.00"));
        payment.setPaymentStatus(PaymentStatus.PAID);

        membership.setEndDate(LocalDate.now().plusDays(1));
        PaymentCoverageSummary summary = membershipLifecycleService.buildCoverageSummary(membership, List.of(payment), 3);

        assertEquals(new BigDecimal("12000.00"), summary.paidAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.pendingAmount()));
        assertEquals(PaymentStatus.PAID, summary.paymentStatus());
        assertEquals(true, summary.expiringSoon());
    }
}
