package com.gymflow.service;

import com.gymflow.domain.PlatformSetting;
import com.gymflow.repository.PlatformSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformSettingService {

    private final PlatformSettingRepository platformSettingRepository;

    @Transactional
    public PlatformSetting getSettings() {
        return platformSettingRepository.findAll().stream().findFirst()
                .orElseGet(() -> platformSettingRepository.save(new PlatformSetting()));
    }

    @Transactional
    public void updateReferralsEnabled(boolean enabled) {
        PlatformSetting settings = getSettings();
        settings.setReferralsEnabled(enabled);
        platformSettingRepository.save(settings);
    }
}
