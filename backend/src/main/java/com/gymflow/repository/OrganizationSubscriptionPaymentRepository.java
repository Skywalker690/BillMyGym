package com.gymflow.repository;

import com.gymflow.domain.OrganizationSubscriptionPayment;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationSubscriptionPaymentRepository extends JpaRepository<OrganizationSubscriptionPayment, Long> {

    Optional<OrganizationSubscriptionPayment> findByRazorpayOrderIdAndDeletedFalse(String razorpayOrderId);

    Optional<OrganizationSubscriptionPayment> findByRazorpayPaymentIdAndDeletedFalse(String razorpayPaymentId);

    Optional<OrganizationSubscriptionPayment> findByGatewayTransactionIdAndDeletedFalse(String gatewayTransactionId);

    Optional<OrganizationSubscriptionPayment> findByWebhookEventIdAndDeletedFalse(String webhookEventId);

    Optional<OrganizationSubscriptionPayment> findTopByRazorpaySubscriptionIdAndDeletedFalseOrderByCreatedAtDesc(String razorpaySubscriptionId);

    @EntityGraph(attributePaths = "organization")
    List<OrganizationSubscriptionPayment> findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = "organization")
    Page<OrganizationSubscriptionPayment> findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(
        Long organizationId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "organization")
    List<OrganizationSubscriptionPayment> findByDeletedFalseOrderByCreatedAtDesc();

    Optional<OrganizationSubscriptionPayment> findTopByOrganizationIdAndPaymentStatusInAndDeletedFalseOrderByCreatedAtDesc(
        Long organizationId,
        Collection<SubscriptionPaymentStatus> statuses
    );

    @EntityGraph(attributePaths = "organization")
    List<OrganizationSubscriptionPayment> findByPaymentStatusInAndNextRetryDateLessThanEqualAndDeletedFalse(
        Collection<SubscriptionPaymentStatus> statuses,
        LocalDate nextRetryDate
    );

    long countByPaymentStatusInAndDeletedFalse(Collection<SubscriptionPaymentStatus> statuses);

    long countByPaymentStatusAndDeletedFalse(SubscriptionPaymentStatus status);

    @Query("""
        select coalesce(sum(payment.amount), 0)
        from OrganizationSubscriptionPayment payment
        where payment.deleted = false
          and payment.paymentStatus = com.gymflow.domain.enums.SubscriptionPaymentStatus.PAID
          and payment.paymentDate between :startDate and :endDate
        """)
    BigDecimal sumPaidBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
        select coalesce(sum(payment.amount), 0)
        from OrganizationSubscriptionPayment payment
        where payment.deleted = false
          and payment.paymentStatus = com.gymflow.domain.enums.SubscriptionPaymentStatus.PAID
        """)
    BigDecimal sumPaidAll();

    @Query("""
        select coalesce(sum(payment.amount), 0)
        from OrganizationSubscriptionPayment payment
        where payment.deleted = false
          and payment.organization.id = :organizationId
          and payment.paymentStatus = com.gymflow.domain.enums.SubscriptionPaymentStatus.PAID
        """)
    BigDecimal sumPaidForOrganization(@Param("organizationId") Long organizationId);

    @Query("""
        select min(payment.paymentDate)
        from OrganizationSubscriptionPayment payment
        where payment.deleted = false
          and payment.paymentStatus = com.gymflow.domain.enums.SubscriptionPaymentStatus.PAID
        """)
    LocalDate findEarliestPaidPaymentDate();
}
