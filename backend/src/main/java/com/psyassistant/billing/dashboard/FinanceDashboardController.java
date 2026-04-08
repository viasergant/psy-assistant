package com.psyassistant.billing.dashboard;

import com.psyassistant.billing.dashboard.dto.FinanceDashboardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the finance dashboard.
 *
 * <p>Exposes a single endpoint:
 * <ul>
 *   <li>{@code GET /api/v1/finance/dashboard} — real-time dashboard aggregates</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/finance")
public class FinanceDashboardController {

    private final FinanceDashboardService dashboardService;

    public FinanceDashboardController(final FinanceDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Returns finance dashboard aggregates: outstanding, overdue, collected this month,
     * and aging buckets.
     *
     * @return 200 with dashboard response
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('READ_FINANCIAL_REPORTS')")
    public ResponseEntity<FinanceDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
