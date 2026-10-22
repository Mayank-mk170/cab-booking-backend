
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health() {

        return new ApiResponse<>(
                true,
                "Cab Booking API is running",
                Map.of(
                        "application", "cab-booking-backend",
                        "status", "UP"
                )
        );
    }
}
