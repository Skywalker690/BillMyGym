package com.gymflow.repository;

import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberPaymentRepository extends JpaRepository<MemberPayment, Long> {

    @EntityGraph(attributePaths = {"member", "membership", "organization"})
    List<MemberPayment> findByOrganizationIdAndDeletedFalseOrderByPaymentDateDescCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = {"member", "membership", "organization"})
    List<MemberPayment> findByOrganizationIdAndDeletedFalseAndPaymentDateBetweenOrderByPaymentDateAscCreatedAtAsc(
        Long organizationId,
        LocalDate startDate,
        LocalDate endDate
    );

    @EntityGraph(attributePaths = {"member", "membership", "organization"})
    List<MemberPayment> findByMembershipIdAndDeletedFalseOrderByPaymentDateAscCreatedAtAsc(Long membershipId);

    @EntityGraph(attributePaths = {"member", "membership", "organization"})
    List<MemberPayment> findByMembershipIdInAndDeletedFalse(Collection<Long> membershipIds);

    Optional<MemberPayment> findByGatewayTransactionIdAndDeletedFalse(String gatewayTransactionId);

    Optional<MemberPayment> findByIdAndOrganizationIdAndDeletedFalse(Long id, Long organizationId);

    @Query("""
        select coalesce(sum(payment.amount), 0)
        from MemberPayment payment
        where payment.deleted = false
          and payment.organization.id = :organizationId
          and payment.paymentStatus = :status
          and payment.paymentType in :paymentTypes
          and payment.paymentDate between :startDate and :endDate
        """)
    BigDecimal sumAmountByOrganizationAndStatusAndTypesBetween(
        @Param("organizationId") Long organizationId,
        @Param("status") PaymentStatus status,
        @Param("paymentTypes") Collection<PaymentType> paymentTypes,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        select count(distinct payment.member.id)
        from MemberPayment payment
        where payment.deleted = false
          and payment.organization.id = :organizationId
          and payment.paymentStatus = :status
          and payment.paymentType = :paymentType
          and payment.amount > 0
          and payment.paymentDate between :startDate and :endDate
        """)
    long countDistinctMembersByOrganizationAndStatusAndTypeBetween(
        @Param("organizationId") Long organizationId,
        @Param("status") PaymentStatus status,
        @Param("paymentType") PaymentType paymentType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("""
        select payment.membership.id as membershipId,
               coalesce(sum(payment.amount), 0) as amount
        from MemberPayment payment
        where payment.deleted = false
          and payment.organization.id = :organizationId
          and payment.paymentStatus = com.gymflow.domain.enums.PaymentStatus.PAID
          and payment.paymentType in :paymentTypes
        group by payment.membership.id
        """)
    List<MembershipAmountProjection> sumSuccessfulAmountsByMembershipForOrganization(
        @Param("organizationId") Long organizationId,
        @Param("paymentTypes") Collection<PaymentType> paymentTypes
    );

    interface MembershipAmountProjection {
        Long getMembershipId();

        BigDecimal getAmount();
    }
}
