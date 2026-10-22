  package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.ApiResponse;
import com.mayank.cabbooking.dto.DriverRequest;
import com.mayank.cabbooking.dto.DriverResponse;
import com.mayank.cabbooking.service.DriverService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    // CREATE: Create your own driver profile
    @PostMapping("/api/driver/profile")
    public ResponseEntity<ApiResponse<DriverResponse>> createProfile(
            @Valid @RequestBody DriverRequest request,
            Authentication authentication) {

        DriverResponse result = driverService.createProfile(
                authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Driver profile created successfully",
                        result));
    }

    // READ: Get your own driver profile
    @GetMapping("/api/driver/profile")
    public ResponseEntity<ApiResponse<DriverResponse>> getMyProfile(
            Authentication authentication) {

        DriverResponse result =
                driverService.getMyProfile(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Driver profile retrieved successfully",
                        result));
    }

    // UPDATE: Update your own driver profile
    @PutMapping("/api/driver/profile")
    public ResponseEntity<ApiResponse<DriverResponse>> updateMyProfile(
            @Valid @RequestBody DriverRequest request,
            Authentication authentication) {

        DriverResponse result = driverService.updateMyProfile(
                authentication.getName(), request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Driver profile updated successfully",
                        result));
    }

    // DELETE: Delete your own driver profile
    @DeleteMapping("/api/driver/profile")
    public ResponseEntity<ApiResponse<Void>> deleteMyProfile(
            Authentication authentication) {

        driverService.deleteMyProfile(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Driver profile deleted successfully",
                        null));
    }

    // ADMIN: Get all drivers
    @GetMapping("/api/admin/drivers")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllDrivers(
            Authentication authentication) {

        requireAdmin(authentication);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Drivers retrieved successfully",
                        driverService.getAllDrivers()));
    }

    // ADMIN: Get a driver by ID
    @GetMapping("/api/admin/drivers/{id}")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(
            @PathVariable Long id,
            Authentication authentication) {

        requireAdmin(authentication);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Driver retrieved successfully",
                        driverService.getDriverById(id)));
    }

    // ADMIN: Verify or unverify a driver
    @PutMapping("/api/admin/drivers/{id}/verify")
    public ResponseEntity<ApiResponse<DriverResponse>> verifyDriver(
            @PathVariable Long id,
            @RequestParam boolean verified,
            Authentication authentication) {

        requireAdmin(authentication);

        DriverResponse result =
                driverService.setVerification(id, verified);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Driver verification updated successfully",
                        result));
    }

    // DRIVER: Set availability
    @PutMapping("/api/driver/availability")
    public ResponseEntity<ApiResponse<DriverResponse>> setAvailability(
            @RequestParam boolean available,
            Authentication authentication) {

        DriverResponse result = driverService.setAvailability(
                authentication.getName(), available);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Driver availability updated successfully",
                        result));
    }

    private void requireAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
