package com.gymflow.repository;

import com.gymflow.domain.OrganizationSubscription;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, Long> {

    Optional<OrganizationSubscription> findTopByOrganizationIdAndDeletedFalseOrderByEndDateDesc(Long organizationId);

    long countByDeletedFalseAndEndDateGreaterThanEqual(LocalDate date);
}
