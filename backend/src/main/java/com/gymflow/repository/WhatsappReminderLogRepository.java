package com.gymflow.repository;

import com.gymflow.domain.WhatsappReminderLog;
import com.gymflow.domain.enums.WhatsappReminderStatus;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappReminderLogRepository extends JpaRepository<WhatsappReminderLog, Long> {

    long countByOrganizationIdAndStatusAndSentAtBetweenAndDeletedFalse(
        Long organizationId,
        WhatsappReminderStatus status,
        OffsetDateTime from,
        OffsetDateTime to
    );
}
