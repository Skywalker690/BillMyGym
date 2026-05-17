package com.gymflow.api;

import com.gymflow.api.dto.SettingDtos.UpdateSettingsRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.OrganizationSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final OrganizationSettingService organizationSettingService;

    @GetMapping
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization settings fetched successfully",
            organizationSettingService.getCurrentSettings()
        ));
    }

    @PatchMapping
    public ResponseEntity<?> updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization settings updated successfully",
            organizationSettingService.updateCurrentSettings(request)
        ));
    }
}
