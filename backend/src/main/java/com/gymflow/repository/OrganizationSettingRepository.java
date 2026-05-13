package com.gymflow.repository;

import com.gymflow.domain.OrganizationSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationSettingRepository extends JpaRepository<OrganizationSetting, Long> {

    Optional<OrganizationSetting> findByOrganizationIdAndDeletedFalse(Long organizationId);
}
