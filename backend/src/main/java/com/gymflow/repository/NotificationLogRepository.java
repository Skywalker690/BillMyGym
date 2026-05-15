package com.gymflow.repository;

import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.enums.NotificationType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    long countByOrganizationIdAndDeletedFalse(Long organizationId);

    List<NotificationLog> findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(Long organizationId);

    List<NotificationLog> findByOrganizationIdAndTypeInAndDeletedFalseOrderByCreatedAtDesc(
        Long organizationId,
        Collection<NotificationType> types
    );
}
