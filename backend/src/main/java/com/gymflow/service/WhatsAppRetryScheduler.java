package com.gymflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WhatsAppRetryScheduler {

    private final WhatsAppDeliveryService whatsAppDeliveryService;

    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void retryDueDeliveries() {
        whatsAppDeliveryService.retryDueDeliveries();
    }
}
