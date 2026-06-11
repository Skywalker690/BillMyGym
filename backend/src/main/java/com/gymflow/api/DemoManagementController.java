package com.gymflow.api;

import com.gymflow.service.DemoOrganizationSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/demo")
@RequiredArgsConstructor
public class DemoManagementController {

    private final DemoOrganizationSeeder demoOrganizationSeeder;

    @PostMapping("/reset")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DemoResetResponse> resetDemoWorkspace() {
        var result = demoOrganizationSeeder.resetDemoWorkspace();
        return ResponseEntity.ok(new DemoResetResponse(true, result.message()));
    }

    public record DemoResetResponse(boolean success, String message) {
    }
}
