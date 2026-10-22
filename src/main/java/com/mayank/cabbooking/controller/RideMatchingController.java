
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.ApiResponse;
import com.mayank.cabbooking.dto.DriverResponse;
import com.mayank.cabbooking.service.DriverMatchingService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rider/rides")
public class RideMatchingController {

    private final DriverMatchingService driverMatchingService;

    public RideMatchingController(
            DriverMatchingService driverMatchingService) {
        this.driverMatchingService = driverMatchingService;
    }

    @GetMapping("/{rideId}/matches")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> findMatches(
            @PathVariable Long rideId,
            Authentication authentication) {

        List<DriverResponse> result =
                driverMatchingService.findMatchingDrivers(
                                rideId, authentication.getName())
                        .stream()
                        .map(DriverResponse::fromEntity)
                        .toList();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true, "Matching drivers retrieved successfully",
                        result));
    }
}
