package com.gymflow.repository;

import com.gymflow.domain.MemberCreditLedgerEntry;
import com.gymflow.domain.enums.MemberCreditLedgerTransactionType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberCreditLedgerRepository extends JpaRepository<MemberCreditLedgerEntry, Long> {

    @EntityGraph(attributePaths = {"member", "organization", "referenceMembership"})
    List<MemberCreditLedgerEntry> findByMemberIdAndOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(Long memberId, Long organizationId);

    @Modifying
    @Query("delete from MemberCreditLedgerEntry entry where entry.organization.id = :organizationId")
    int deleteAllByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("""
        select coalesce(sum(
            case
                when entry.transactionType = com.gymflow.domain.enums.MemberCreditLedgerTransactionType.CREDIT then entry.amount
                else -entry.amount
            end
        ), 0)
        from MemberCreditLedgerEntry entry
        where entry.deleted = false
          and entry.member.id = :memberId
          and entry.organization.id = :organizationId
        """)
    BigDecimal balanceForMember(@Param("memberId") Long memberId, @Param("organizationId") Long organizationId);

    @Query("""
        select coalesce(sum(entry.amount), 0)
        from MemberCreditLedgerEntry entry
        where entry.deleted = false
          and entry.referenceMembership.id = :membershipId
          and entry.transactionType = com.gymflow.domain.enums.MemberCreditLedgerTransactionType.DEBIT
        """)
    BigDecimal appliedCreditForMembership(@Param("membershipId") Long membershipId);

    @Query("""
        select entry.member.id as memberId,
               coalesce(sum(
                   case
                       when entry.transactionType = com.gymflow.domain.enums.MemberCreditLedgerTransactionType.CREDIT then entry.amount
                       else -entry.amount
                   end
               ), 0) as amount
        from MemberCreditLedgerEntry entry
        where entry.deleted = false
          and entry.member.id in :memberIds
          and entry.organization.id = :organizationId
        group by entry.member.id
        """)
    List<MemberAmountProjection> balancesForMembers(
        @Param("organizationId") Long organizationId,
        @Param("memberIds") Collection<Long> memberIds
    );

    @Query("""
        select entry.referenceMembership.id as membershipId,
               coalesce(sum(entry.amount), 0) as amount
        from MemberCreditLedgerEntry entry
        where entry.deleted = false
          and entry.referenceMembership.id in :membershipIds
          and entry.transactionType = :transactionType
        group by entry.referenceMembership.id
        """)
    List<MembershipAmountProjection> amountsForMemberships(
        @Param("membershipIds") Collection<Long> membershipIds,
        @Param("transactionType") MemberCreditLedgerTransactionType transactionType
    );

    interface MemberAmountProjection {
        Long getMemberId();

        BigDecimal getAmount();
    }

    interface MembershipAmountProjection {
        Long getMembershipId();

        BigDecimal getAmount();
    }
}
