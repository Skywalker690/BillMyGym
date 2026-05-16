package com.gymflow.api;

import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboardReport() {
        return ResponseEntity.ok(ApiResponse.success(
            "Dashboard report fetched successfully",
            reportService.dashboardReport()
        ));
    }

    @GetMapping("/reminders")
    public ResponseEntity<?> reminderReport() {
        return ResponseEntity.ok(ApiResponse.success(
            "Reminder report fetched successfully",
            reportService.reminderReport()
        ));
    }
}
