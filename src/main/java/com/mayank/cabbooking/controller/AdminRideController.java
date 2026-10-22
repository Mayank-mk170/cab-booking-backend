
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.ApiResponse;
import com.mayank.cabbooking.dto.RideResponse;
import com.mayank.cabbooking.service.RideService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rides")
public class AdminRideController {

    private final RideService rideService;

    public AdminRideController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RideResponse>>> getAllRides(
            Authentication authentication) {

        List<RideResponse> rides =
                rideService.getAllRidesForAdmin(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "All rides retrieved successfully",
                        rides
                )
        );
    }
}
