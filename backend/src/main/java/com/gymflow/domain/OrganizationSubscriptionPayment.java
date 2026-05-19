package com.gymflow.domain;

import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "organization_subscription_payments")
public class OrganizationSubscriptionPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan")
    private SubscriptionPlan subscriptionPlan;

    private LocalDate paymentDate;

    @Column(nullable = false)
    private LocalDate subscriptionStart;

    @Column(nullable = false)
    private LocalDate subscriptionEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPaymentStatus paymentStatus = SubscriptionPaymentStatus.PENDING;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int retryCount;

    private LocalDate nextRetryDate;

    @Column(length = 1000)
    private String failureReason;

    private String webhookEventId;

    private String gatewayTransactionId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySubscriptionId;
}
