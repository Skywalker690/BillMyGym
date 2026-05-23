package com.gymflow.api.dto;

import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class MemberDtos {

    public record MemberRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        MemberStatus status,
        String deactivationReason
    ) {
    }

    public record MemberResponse(
        Long id,
        Long organizationId,
        String fullName,
        String phone,
        MemberStatus status,
        OffsetDateTime deactivatedAt,
        String deactivationReason,
        boolean deleted,
        OffsetDateTime deletedAt
    ) {
    }

    public record CurrentMembershipSummary(
        Long id,
        int durationMonths,
        BigDecimal planAmount,
        LocalDate startDate,
        LocalDate endDate,
        MembershipStatus status,
        boolean expiringSoon,
        PaymentStatus paymentStatus,
        BigDecimal paidAmount,
        BigDecimal appliedCreditAmount,
        BigDecimal pendingAmount,
        BigDecimal walletBalance
    ) {
    }

    public record MemberDirectoryItem(
        Long id,
        Long organizationId,
        String fullName,
        String phone,
        MemberStatus status,
        CurrentMembershipSummary currentMembership
    ) {
    }

    public record MemberDirectorySummary(
        long totalMembers,
        long activeMembers,
        long deactivatedMembers,
        long activeMemberships,
        long expiringSoonMemberships,
        long expiredMemberships,
        long overdueMemberships,
        SubscriptionDtos.MemberLimitResponse memberLimit
    ) {
    }

    public record MemberPageResponse(
        List<MemberDirectoryItem> members,
        MemberDirectorySummary summary,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
    ) {
    }
}
