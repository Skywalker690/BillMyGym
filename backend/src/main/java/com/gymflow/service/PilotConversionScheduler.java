package com.gymflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PilotConversionScheduler {

    private final PilotConversionService pilotConversionService;

    @Transactional
    @Scheduled(cron = "0 0 8 * * *")
    public void convertPilots() {
        pilotConversionService.convertEligiblePilots();
    }
}
