
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.AdminDashboardStatsResponse;
import com.mayank.cabbooking.dto.ApiResponse;
import com.mayank.cabbooking.service.AdminDashboardService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(
            AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getStats(
            Authentication authentication) {

        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Dashboard statistics retrieved successfully",
                        dashboardService.getStats()
                )
        );
    }
}
