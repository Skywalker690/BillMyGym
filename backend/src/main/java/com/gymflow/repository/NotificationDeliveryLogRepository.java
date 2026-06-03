package com.gymflow.repository;

import com.gymflow.domain.NotificationDeliveryLog;
import com.gymflow.domain.enums.NotificationDeliveryStatus;
import com.gymflow.domain.enums.NotificationType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, Long> {

    boolean existsByOrganizationIdAndTypeAndStatusInAndLastAttemptBetween(
        Long organizationId,
        NotificationType type,
        Collection<NotificationDeliveryStatus> statuses,
        Instant start,
        Instant end
    );

    Optional<NotificationDeliveryLog> findTopByOrganizationIdAndTypeOrderByCreatedAtDesc(Long organizationId, NotificationType type);

    Optional<NotificationDeliveryLog> findTopByProviderMessageIdAndDeletedFalseOrderByCreatedAtDesc(String providerMessageId);

    @EntityGraph(attributePaths = "organization")
    List<NotificationDeliveryLog> findByProviderAndStatusInAndNextRetryAtLessThanEqualAndDeletedFalseOrderByNextRetryAtAsc(
        String provider,
        Collection<NotificationDeliveryStatus> statuses,
        Instant nextRetryAt
    );
}
