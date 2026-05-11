package com.Mengge.finance_tracker.controller;

import com.Mengge.finance_tracker.dto.dashboard.DashboardResponse;
import com.Mengge.finance_tracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    /**
     * Monthly dashboard. Omit year and month to use the current calendar month.
     */
    @GetMapping
    public DashboardResponse getDashboard(
        Authentication authentication,
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) Integer month
    ) {
        return dashboardService.getMonthlyDashboard(authentication.getName(), year, month);
    }
}
