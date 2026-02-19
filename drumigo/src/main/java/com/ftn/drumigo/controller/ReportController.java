package com.ftn.drumigo.controller;

import com.ftn.drumigo.dto.ReportChartResponse;
import com.ftn.drumigo.dto.ReportResponse;
import com.ftn.drumigo.security.CustomUserDetails;
import com.ftn.drumigo.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reports/me")
    public ResponseEntity<ReportChartResponse> getMyChartReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        ReportChartResponse report = reportService.getChartReportForUser(userDetails.getUserId(), from, to);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/users/{userId}/reports")
    public ResponseEntity<ReportResponse> getUserReport(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        ReportResponse report = reportService.getUserReport(userId, from, to);
        return ResponseEntity.ok(report);
    }
}

