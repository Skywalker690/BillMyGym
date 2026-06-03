package com.gymflow.api;

import com.gymflow.service.FinancialReportService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final FinancialReportService financialReportService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @RequestParam(defaultValue = "csv") String type,
        @RequestParam(defaultValue = "monthly") String period,
        @RequestParam(required = false) String range,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to
    ) {
        FinancialReportService.ReportExport export = financialReportService.export(type, period, range, from, to);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(export.contentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(export.filename()).build().toString())
            .body(export.content());
    }
}
