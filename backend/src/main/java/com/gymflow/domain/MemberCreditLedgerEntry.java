package com.gymflow.domain;

import com.gymflow.domain.enums.MemberCreditLedgerReason;
import com.gymflow.domain.enums.MemberCreditLedgerTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "member_credit_ledger", indexes = {
    @Index(name = "idx_member_credit_ledger_member", columnList = "member_id, organization_id, deleted"),
    @Index(name = "idx_member_credit_ledger_organization_created", columnList = "organization_id, created_at DESC, deleted"),
    @Index(name = "idx_member_credit_ledger_membership", columnList = "reference_membership_id, transaction_type, deleted")
})
public class MemberCreditLedgerEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_membership_id")
    private MemberMembership referenceMembership;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberCreditLedgerTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberCreditLedgerReason reason;

    @Column(length = 1000)
    private String notes;

    private Long createdByUserId;

    @Column(length = 255)
    private String createdByEmail;
}
