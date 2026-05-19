package com.gymflow.repository;

import com.gymflow.domain.ProcessedWebhookEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, Long> {

    Optional<ProcessedWebhookEvent> findByEventId(String eventId);
}
