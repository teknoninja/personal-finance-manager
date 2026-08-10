package com.financemanager.controller;

import com.financemanager.dto.response.MonthlyReportResponse;
import com.financemanager.dto.response.YearlyReportResponse;
import com.financemanager.entity.User;
import com.financemanager.service.CurrentUserProvider;
import com.financemanager.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportResponse> monthly(@PathVariable int year, @PathVariable int month) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(reportService.getMonthlyReport(user, year, month));
    }

    @GetMapping("/yearly/{year}")
    public ResponseEntity<YearlyReportResponse> yearly(@PathVariable int year) {
        User user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(reportService.getYearlyReport(user, year));
    }
}
