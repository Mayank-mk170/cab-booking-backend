
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.DriverLocationEvent;
import com.mayank.cabbooking.dto.DriverLocationRequest;
import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Driver;
import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.entity.RideStatus;
import com.mayank.cabbooking.entity.Role;
import com.mayank.cabbooking.repository.AuthRepository;
import com.mayank.cabbooking.repository.DriverRepository;
import com.mayank.cabbooking.repository.RideRepository;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverLocationController {

    private final AuthRepository authRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public DriverLocationController(
            AuthRepository authRepository,
            DriverRepository driverRepository,
            RideRepository rideRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.authRepository = authRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/location")
    @Transactional
    public ResponseEntity<?> updateLocation(
            Principal principal,
            @RequestBody DriverLocationRequest request) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Authentication required"));
        }

        BigDecimal latitude = request.getLatitude();
        BigDecimal longitude = request.getLongitude();

        if (latitude == null || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid GPS coordinates"));
        }

        String email = principal.getName()
                .trim().toLowerCase(Locale.ROOT);

        Auth user = authRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found"));

        if (user.getRole() != Role.DRIVER) {
            return ResponseEntity.status(403)
                    .body(Map.of("message",
                            "Only drivers can update GPS location"));
        }

        Driver driver = driverRepository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found"));

        if (!driver.isVerified()) {
            return ResponseEntity.status(403)
                    .body(Map.of("message",
                            "Driver verification is required"));
        }

        driver.setCurrentLatitude(latitude);
        driver.setCurrentLongitude(longitude);
        driverRepository.save(driver);

        List<RideStatus> activeStatuses = List.of(
                RideStatus.ACCEPTED,
                RideStatus.DRIVER_ARRIVING,
                RideStatus.IN_PROGRESS
        );

        List<Ride> rides =
                rideRepository.findByDriver_IdOrderByRequestedAtDesc(
                        driver.getId());

        LocalDateTime updatedAt = LocalDateTime.now();

        for (Ride ride : rides) {
            if (activeStatuses.contains(ride.getStatus())) {
                DriverLocationEvent event = new DriverLocationEvent(
                        ride.getId(),
                        latitude,
                        longitude,
                        updatedAt
                );

                messagingTemplate.convertAndSend(
                        "/topic/rides/" + ride.getId() + "/location",
                        event
                );
            }
        }

        return ResponseEntity.ok(Map.of(
                "message", "Driver location updated",
                "latitude", latitude,
                "longitude", longitude
        ));
    }
}
