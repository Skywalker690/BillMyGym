package com.gymflow.service;

import com.gymflow.api.dto.MembershipDtos.PaymentCoverageSummary;
import com.gymflow.api.dto.PaymentDtos.PaymentResponse;
import com.gymflow.api.dto.PaymentDtos.RecordPaymentRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentLedgerService {

    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MembershipLifecycleService membershipLifecycleService;
    private final OrganizationSettingService organizationSettingService;
    private final AuditLogService auditLogService;

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public PaymentResponse recordPayment(RecordPaymentRequest request) {
        Long organizationId = requireOrganizationId();
        MemberMembership membership = memberMembershipRepository.findByIdAndOrganizationIdAndDeletedFalse(
            request.membershipId(),
            organizationId
        ).orElseThrow(() -> new NotFoundException("Membership not found"));
        if (membership.getMember().isDeleted() || membership.getMember().getStatus() == com.gymflow.domain.enums.MemberStatus.DEACTIVATED) {
            throw new BadRequestException("Cannot record payment for deleted or deactivated members");
        }
        if (request.gatewayTransactionId() != null && !request.gatewayTransactionId().isBlank()) {
            memberPaymentRepository.findByGatewayTransactionIdAndDeletedFalse(request.gatewayTransactionId())
                .ifPresent(existing -> {
                    throw new BadRequestException("This gateway transaction has already been processed");
                });
        }

        MemberPayment payment = new MemberPayment();
        payment.setMember(membership.getMember());
        payment.setMembership(membership);
        payment.setOrganization(membership.getOrganization());
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate());
        payment.setBillingMonth(YearMonth.from(request.paymentDate()).toString());
        payment.setPaymentType(request.paymentType());
        payment.setGatewayTransactionId(hasText(request.gatewayTransactionId()) ? request.gatewayTransactionId().trim() : null);
        payment.setNotes(request.notes());

        List<MemberPayment> previousPayments = new ArrayList<>(
            memberPaymentRepository.findByMembershipIdAndDeletedFalseOrderByPaymentDateAscCreatedAtAsc(membership.getId())
        );
        payment.setPaymentStatus(resolvePaymentStatus(membership, previousPayments, request));
        payment = memberPaymentRepository.save(payment);

        auditLogService.log(
            "PAYMENT_CREATED",
            "Created payment id=" + payment.getId() + " for membership id=" + membership.getId(),
            AuditCategory.MEMBER,
            "PAYMENT"
        );

        previousPayments.add(payment);
        int reminderDays = organizationSettingService
            .getOrCreateSettings(organizationId)
            .getReminderDaysBeforeExpiry();
        PaymentCoverageSummary coverage = membershipLifecycleService.buildCoverageSummary(membership, previousPayments, reminderDays);
        return toResponse(payment, coverage);
    }

    public List<PaymentResponse> listPayments() {
        Long organizationId = requireOrganizationId();
        List<MemberPayment> payments = memberPaymentRepository.findByOrganizationIdAndDeletedFalseOrderByPaymentDateDescCreatedAtDesc(organizationId);
        int reminderDays = organizationSettingService.getOrCreateSettings(organizationId).getReminderDaysBeforeExpiry();
        Map<Long, List<MemberPayment>> paymentsByMembershipId = loadPaymentsByMembershipId(payments);
        return payments.stream()
            .map(payment -> toResponse(
                payment,
                membershipLifecycleService.buildCoverageSummary(
                    payment.getMembership(),
                    paymentsByMembershipId.getOrDefault(payment.getMembership().getId(), List.of()),
                    reminderDays
                )
            ))
            .toList();
    }

    private PaymentStatus resolvePaymentStatus(
        MemberMembership membership,
        List<MemberPayment> previousPayments,
        RecordPaymentRequest request
    ) {
        if (request.paymentStatus() == PaymentStatus.FAILED) {
            return PaymentStatus.FAILED;
        }
        BigDecimal totalPaid = membershipLifecycleService.successfulPaymentTotal(previousPayments).add(request.amount());
        if (request.paymentStatus() == PaymentStatus.PENDING) {
            return PaymentStatus.PENDING;
        }
        return membershipLifecycleService.resolveCoverageStatus(membership.getPlanAmount(), totalPaid);
    }

    private PaymentResponse toResponse(MemberPayment payment, PaymentCoverageSummary coverage) {
        return new PaymentResponse(
            payment.getId(),
            payment.getMember().getId(),
            payment.getMembership().getId(),
            payment.getOrganization().getId(),
            payment.getAmount(),
            payment.getPaymentDate(),
            payment.getBillingMonth(),
            payment.getPaymentStatus(),
            payment.getPaymentType(),
            payment.getGatewayTransactionId(),
            payment.getNotes(),
            coverage
        );
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public void softDeletePayment(Long paymentId) {
        Long organizationId = requireOrganizationId();
        MemberPayment payment = memberPaymentRepository.findByIdAndOrganizationIdAndDeletedFalse(paymentId, organizationId)
            .orElseThrow(() -> new NotFoundException("Payment not found"));
        payment.setDeleted(true);
        payment.setDeletedAt(OffsetDateTime.now());
    }

    private Long requireOrganizationId() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<Long, List<MemberPayment>> loadPaymentsByMembershipId(Collection<MemberPayment> payments) {
        List<Long> membershipIds = payments.stream()
            .map(payment -> payment.getMembership().getId())
            .distinct()
            .toList();
        if (membershipIds.isEmpty()) {
            return Map.of();
        }
        return memberPaymentRepository.findByMembershipIdInAndDeletedFalse(membershipIds).stream()
            .collect(Collectors.groupingBy(
                payment -> payment.getMembership().getId(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    items -> items.stream()
                        .sorted((left, right) -> {
                            int paymentDateComparison = left.getPaymentDate().compareTo(right.getPaymentDate());
                            if (paymentDateComparison != 0) {
                                return paymentDateComparison;
                            }
                            return left.getCreatedAt().compareTo(right.getCreatedAt());
                        })
                        .toList()
                )
            ));
    }
}
