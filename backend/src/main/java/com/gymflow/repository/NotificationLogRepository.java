package com.gymflow.repository;

import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(
        value = """
            select notification from NotificationLog notification
            where notification.organization.id = :organizationId
              and notification.deleted = false
              and (:channel is null or notification.channel = :channel)
              and (:status is null or notification.status = :status)
              and (:fromDate is null or notification.createdAt >= :fromDate)
              and (:toDate is null or notification.createdAt < :toDate)
              and (
                :pattern is null
                or (notification.recipient is not null and lower(notification.recipient) like :pattern)
                or (notification.message is not null and lower(notification.message) like :pattern)
              )
            order by notification.createdAt desc
            """,
        countQuery = """
            select count(notification) from NotificationLog notification
            where notification.organization.id = :organizationId
              and notification.deleted = false
              and (:channel is null or notification.channel = :channel)
              and (:status is null or notification.status = :status)
              and (:fromDate is null or notification.createdAt >= :fromDate)
              and (:toDate is null or notification.createdAt < :toDate)
              and (
                :pattern is null
                or (notification.recipient is not null and lower(notification.recipient) like :pattern)
                or (notification.message is not null and lower(notification.message) like :pattern)
              )
            """
    )
    Page<NotificationLog> findAuditNotificationPage(
        @Param("organizationId") Long organizationId,
        @Param("channel") NotificationChannel channel,
        @Param("status") NotificationStatus status,
        @Param("fromDate") OffsetDateTime fromDate,
        @Param("toDate") OffsetDateTime toDate,
        @Param("pattern") String pattern,
        Pageable pageable
    );

    long countByOrganizationIdAndDeletedFalseAndChannelAndStatus(
        Long organizationId,
        NotificationChannel channel,
        NotificationStatus status
    );

    long countByOrganizationIdAndDeletedFalseAndStatus(
        Long organizationId,
        NotificationStatus status
    );

    long countByOrganizationIdAndDeletedFalseAndStatusAndCreatedAtBetween(
        Long organizationId,
        NotificationStatus status,
        OffsetDateTime fromDate,
        OffsetDateTime toDate
    );

    boolean existsByOrganizationIdAndNotificationDateAndTypeAndStatusAndDeletedFalse(
        Long organizationId,
        LocalDate notificationDate,
        NotificationType type,
        NotificationStatus status
    );
}
