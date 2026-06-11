package com.gymflow.repository;

import com.gymflow.domain.WhatsappReminderLog;
import com.gymflow.domain.enums.WhatsappReminderStatus;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhatsappReminderLogRepository extends JpaRepository<WhatsappReminderLog, Long> {

    long countByOrganizationIdAndStatusAndSentAtBetweenAndDeletedFalse(
        Long organizationId,
        WhatsappReminderStatus status,
        OffsetDateTime from,
        OffsetDateTime to
    );

    @Modifying
    @Query("delete from WhatsappReminderLog log where log.organization.id = :organizationId")
    int deleteAllByOrganizationId(@Param("organizationId") Long organizationId);
}
