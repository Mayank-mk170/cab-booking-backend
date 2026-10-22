
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.ApiResponse;
import com.mayank.cabbooking.dto.VehicleRequest;
import com.mayank.cabbooking.dto.VehicleResponse;
import com.mayank.cabbooking.service.VehicleService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> registerVehicle(
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication) {

        VehicleResponse vehicle = vehicleService.registerVehicle(
                authentication.getName(),
                request
        );

        ApiResponse<VehicleResponse> response = new ApiResponse<>(
                true,
                "Vehicle registered successfully. Awaiting verification.",
                vehicle
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getMyVehicles(
            Authentication authentication) {

        List<VehicleResponse> vehicles =
                vehicleService.getMyVehicles(authentication.getName());

        ApiResponse<List<VehicleResponse>> response = new ApiResponse<>(
                true,
                "Vehicles retrieved successfully",
                vehicles
        );

        return ResponseEntity.ok(response);
    }
}
