package com.gymflow.repository;

import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.enums.NotificationType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    long countByOrganizationIdAndDeletedFalse(Long organizationId);

    @EntityGraph(attributePaths = {"member", "organization"})
    List<NotificationLog> findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(Long organizationId);

    @EntityGraph(attributePaths = {"member", "organization"})
    List<NotificationLog> findByOrganizationIdAndTypeInAndDeletedFalseOrderByCreatedAtDesc(
        Long organizationId,
        Collection<NotificationType> types
    );

    @EntityGraph(attributePaths = {"member", "organization"})
    List<NotificationLog> findByOrganizationIdAndTypeInAndDeletedFalseOrderByCreatedAtDesc(
        Long organizationId,
        Collection<NotificationType> types,
        Pageable pageable
    );

    long countByOrganizationIdAndTypeAndDeletedFalse(Long organizationId, NotificationType type);

    boolean existsByOrganizationIdAndNotificationDateAndTypeAndDeletedFalse(
        Long organizationId,
        LocalDate notificationDate,
        NotificationType type
    );
}
