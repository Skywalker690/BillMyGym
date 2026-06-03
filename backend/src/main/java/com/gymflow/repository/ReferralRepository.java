package com.gymflow.repository;

import com.gymflow.domain.Organization;
import com.gymflow.domain.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {
    Optional<Referral> findByReferred(Organization referred);
    List<Referral> findByReferrer(Organization referrer);
    long countByReferrer(Organization referrer);
}
