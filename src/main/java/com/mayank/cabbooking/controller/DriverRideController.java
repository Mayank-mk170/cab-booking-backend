
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.ApiResponse;
import com.mayank.cabbooking.dto.RideResponse;
import com.mayank.cabbooking.dto.RideStatusRequest;
import com.mayank.cabbooking.service.RideService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver/rides")
public class DriverRideController {

    private final RideService rideService;

    public DriverRideController(RideService rideService) {
        this.rideService = rideService;
    }

    // Get all ride requests available to this driver
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getAvailableRides(
            Authentication authentication) {

        List<RideResponse> result =
                rideService.getAvailableRides(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Available rides retrieved successfully",
                        result));
    }

    // Get this driver's ride history
    @GetMapping
    public ResponseEntity<ApiResponse<List<RideResponse>>> getMyRides(
            Authentication authentication) {

        List<RideResponse> result =
                rideService.getMyDriverRides(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Driver ride history retrieved successfully",
                        result));
    }

    // Accept a ride request
    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<RideResponse>> acceptRide(
            @PathVariable Long id,
            Authentication authentication) {

        RideResponse result =
                rideService.acceptRide(authentication.getName(), id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Ride accepted successfully",
                        result));
    }

    // Cancel a ride assigned to this driver
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(
            @PathVariable Long id,
            Authentication authentication) {

        RideResponse result =
                rideService.cancelDriverRide(authentication.getName(), id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Ride cancelled successfully",
                        result));
    }

    // Update ride status: DRIVER_ARRIVING, IN_PROGRESS, COMPLETED
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RideResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RideStatusRequest request,
            Authentication authentication) {

        RideResponse result = rideService.updateRideStatus(
                authentication.getName(),
                id,
                request.getStatus());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Ride status updated successfully",
                        result));
    }
}
