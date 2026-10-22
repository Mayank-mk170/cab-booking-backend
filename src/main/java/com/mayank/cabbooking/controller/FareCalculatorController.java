
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.service.FareCalculatorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/fare")
public class FareCalculatorController {

    private final FareCalculatorService fareCalculatorService;

    public FareCalculatorController(
            FareCalculatorService fareCalculatorService) {
        this.fareCalculatorService = fareCalculatorService;
    }

    /**
     * Estimate ride distance and fare using coordinates.
     */
    @PostMapping("/estimate")
    public ResponseEntity<FareEstimateResponse> estimateFare(
            @Valid @RequestBody FareEstimateRequest request) {

        BigDecimal distanceKm =
                fareCalculatorService.calculateDistanceKm(
                        request.getPickupLatitude(),
                        request.getPickupLongitude(),
                        request.getDropoffLatitude(),
                        request.getDropoffLongitude()
                );

        BigDecimal estimatedFare =
                fareCalculatorService.calculateFare(distanceKm);

        FareEstimateResponse response = new FareEstimateResponse();
        response.setDistanceKm(distanceKm);
        response.setEstimatedFare(estimatedFare);

        return ResponseEntity.ok(response);
    }

    @Getter
    @Setter
    public static class FareEstimateRequest {

        @NotNull(message = "Pickup latitude is required")
        @DecimalMin(value = "-90.0",
                message = "Pickup latitude must be at least -90")
        @DecimalMax(value = "90.0",
                message = "Pickup latitude must not exceed 90")
        private BigDecimal pickupLatitude;

        @NotNull(message = "Pickup longitude is required")
        @DecimalMin(value = "-180.0",
                message = "Pickup longitude must be at least -180")
        @DecimalMax(value = "180.0",
                message = "Pickup longitude must not exceed 180")
        private BigDecimal pickupLongitude;

        @NotNull(message = "Dropoff latitude is required")
        @DecimalMin(value = "-90.0",
                message = "Dropoff latitude must be at least -90")
        @DecimalMax(value = "90.0",
                message = "Dropoff latitude must not exceed 90")
        private BigDecimal dropoffLatitude;

        @NotNull(message = "Dropoff longitude is required")
        @DecimalMin(value = "-180.0",
                message = "Dropoff longitude must be at least -180")
        @DecimalMax(value = "180.0",
                message = "Dropoff longitude must not exceed 180")
        private BigDecimal dropoffLongitude;
    }

    @Getter
    @Setter
    public static class FareEstimateResponse {

        private BigDecimal distanceKm;
        private BigDecimal estimatedFare;
    }
}
