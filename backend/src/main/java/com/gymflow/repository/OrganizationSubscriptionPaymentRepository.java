package com.gymflow.repository;

import com.gymflow.domain.OrganizationSubscriptionPayment;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    Optional<OrganizationSubscriptionPayment> findTopByOrganizationIdAndPaymentStatusAndDeletedFalseOrderBySubscriptionEndDescCreatedAtDesc(
        Long organizationId,
        SubscriptionPaymentStatus paymentStatus
    );

    @EntityGraph(attributePaths = "organization")
    List<OrganizationSubscriptionPayment> findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = "organization")
    Page<OrganizationSubscriptionPayment> findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(
        Long organizationId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "organization")
    List<OrganizationSubscriptionPayment> findByDeletedFalseOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "organization")
    Page<OrganizationSubscriptionPayment> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

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

    long countByPaymentStatusAndSubscriptionEndGreaterThanEqualAndDeletedFalse(
        SubscriptionPaymentStatus status,
        LocalDate subscriptionEnd
    );

    long countByPaymentStatusAndDeletedFalseAndCreatedAtBetween(
        SubscriptionPaymentStatus status,
        OffsetDateTime start,
        OffsetDateTime end
    );

    long countByPaymentStatusAndDeletedFalseAndPaymentDateBetween(
        SubscriptionPaymentStatus status,
        LocalDate start,
        LocalDate end
    );

    @Query("""
        select coalesce(sum(payment.amount), 0)
        from OrganizationSubscriptionPayment payment
        where payment.deleted = false
          and payment.paymentStatus = :status
          and payment.createdAt between :start and :end
        """)
    BigDecimal sumByPaymentStatusAndCreatedAtBetween(
        @Param("status") SubscriptionPaymentStatus status,
        @Param("start") OffsetDateTime start,
        @Param("end") OffsetDateTime end
    );

    @Query("""
        select coalesce(sum(payment.amount), 0)
        from OrganizationSubscriptionPayment payment
        where payment.deleted = false
          and payment.paymentStatus = :status
          and payment.paymentDate between :startDate and :endDate
        """)
    BigDecimal sumByPaymentStatusAndPaymentDateBetween(
        @Param("status") SubscriptionPaymentStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

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
