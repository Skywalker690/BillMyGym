package com.gymflow.repository;

import com.gymflow.domain.Organization;
import com.gymflow.domain.ReferralReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReferralRewardRepository extends JpaRepository<ReferralReward, UUID> {
    List<ReferralReward> findByOrganization(Organization organization);
}
