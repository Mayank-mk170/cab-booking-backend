
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.ApiResponse;
import com.mayank.cabbooking.dto.RideRequest;
import com.mayank.cabbooking.dto.RideResponse;
import com.mayank.cabbooking.service.RideService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rider/rides")
public class RiderRideController {

    private final RideService rideService;

    public RiderRideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RideResponse>> requestRide(
            @Valid @RequestBody RideRequest request,
            Authentication authentication) {

        RideResponse result = rideService.requestRide(
                authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true, "Ride requested successfully", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RideResponse>>> getMyRides(
            Authentication authentication) {

        List<RideResponse> result =
                rideService.getMyRiderRides(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true, "Ride history retrieved successfully", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RideResponse>> getMyRide(
            @PathVariable Long id,
            Authentication authentication) {

        RideResponse result =
                rideService.getMyRide(authentication.getName(), id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true, "Ride retrieved successfully", result));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(
            @PathVariable Long id,
            Authentication authentication) {

        RideResponse result =
                rideService.cancelRide(authentication.getName(), id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true, "Ride cancelled successfully", result));
    }
}
