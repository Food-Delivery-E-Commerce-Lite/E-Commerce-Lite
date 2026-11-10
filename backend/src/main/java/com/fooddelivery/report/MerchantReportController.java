package com.fooddelivery.report;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.ApiResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/merchant/restaurants/{id}")
public class MerchantReportController {
    private final ReportService reports;

    public MerchantReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/report")
    public ApiResponse<ReportDtos.MerchantReport> report(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reports.merchantReport(me.id(), id, from, to));
    }

    @GetMapping(value = "/report.csv", produces = "text/csv")
    public ResponseEntity<String> reportCsv(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String csv = reports.merchantCsv(me.id(), id, from, to);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                .attachment().filename("bao-cao-quan-" + id + ".csv").build().toString())
            .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
            .body(csv);
    }
}
