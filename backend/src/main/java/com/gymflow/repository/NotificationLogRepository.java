package com.gymflow.repository;

import com.gymflow.domain.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    long countByOrganizationIdAndDeletedFalse(Long organizationId);
}
