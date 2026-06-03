package com.gymflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SubscriptionBillingScheduler {

    private final SubscriptionBillingService subscriptionBillingService;

    @Transactional
    @Scheduled(cron = "0 15 9 * * *")
    public void processBillingLifecycle() {
        subscriptionBillingService.processDailyBilling();
    }
}
