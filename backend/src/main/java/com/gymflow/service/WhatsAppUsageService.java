package com.gymflow.service;

import com.gymflow.domain.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsAppUsageService {

    private final UsageMetricService usageMetricService;

    @Transactional
    public void recordSent(Organization organization) {
        usageMetricService.recordWhatsappDelivered(organization);
    }
}
