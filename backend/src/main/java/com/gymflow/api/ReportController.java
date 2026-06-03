package com.gymflow.api;

import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.FinancialReportService;
import com.gymflow.service.ReportService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final FinancialReportService financialReportService;

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

    @GetMapping("/financial")
    public ResponseEntity<?> financialReport(
        @RequestParam(defaultValue = "monthly") String period,
        @RequestParam(required = false) String range,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            "Financial report fetched successfully",
            financialReportService.financialReport(period, range, from, to)
        ));
    }
}
