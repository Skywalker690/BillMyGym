package com.gymflow.domain;

import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "member_payments", indexes = {
    @Index(name = "idx_member_payments_org_deleted", columnList = "organization_id, deleted"),
    @Index(name = "idx_member_payments_org_payment_date", columnList = "organization_id, payment_date DESC, deleted"),
    @Index(name = "idx_member_payments_org_status_date", columnList = "organization_id, payment_status, payment_date DESC, deleted"),
    @Index(name = "idx_member_payments_org_type_date", columnList = "organization_id, payment_type, payment_date DESC, deleted"),
    @Index(name = "idx_member_payments_member_billing", columnList = "member_id, billing_month, deleted"),
    @Index(name = "idx_member_payments_membership_deleted", columnList = "membership_id, deleted"),
    @Index(name = "idx_member_payments_gateway_txn", columnList = "gateway_transaction_id, deleted")
})
public class MemberPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private MemberMembership membership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, length = 7)
    private String billingMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType = PaymentType.MEMBERSHIP;

    @Column(name = "gateway_transaction_id")
    private String gatewayTransactionId;

    @Column(length = 1000)
    private String notes;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;
}
