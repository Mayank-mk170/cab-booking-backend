
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Driver;
import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.entity.RideStatus;
import com.mayank.cabbooking.entity.Role;
import com.mayank.cabbooking.repository.AuthRepository;
import com.mayank.cabbooking.repository.DriverRepository;
import com.mayank.cabbooking.repository.RideRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class DriverMatchingService {

    private final AuthRepository authRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;
    private final FareCalculatorService fareCalculatorService;

    public DriverMatchingService(
            AuthRepository authRepository,
            DriverRepository driverRepository,
            RideRepository rideRepository,
            FareCalculatorService fareCalculatorService) {

        this.authRepository = authRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
        this.fareCalculatorService = fareCalculatorService;
    }

    @Transactional
    public void updateLocation(
            String email,
            BigDecimal latitude,
            BigDecimal longitude) {

        Auth user = getUser(email);

        if (user.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException(
                    "Only DRIVER accounts can update driver location");
        }

        Driver driver = driverRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Driver profile not found"));

        if (latitude == null || longitude == null
                || latitude.compareTo(new BigDecimal("-90")) < 0
                || latitude.compareTo(new BigDecimal("90")) > 0
                || longitude.compareTo(new BigDecimal("-180")) < 0
                || longitude.compareTo(new BigDecimal("180")) > 0) {
            throw new IllegalArgumentException(
                    "Invalid latitude or longitude");
        }

        driver.setCurrentLatitude(latitude);
        driver.setCurrentLongitude(longitude);

        driverRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public List<Driver> findMatchingDrivers(Long rideId, String riderEmail) {

        Auth rider = getUser(riderEmail);

        if (rider.getRole() != Role.RIDER) {
            throw new IllegalArgumentException(
                    "Only RIDER accounts can view matches for their rides");
        }

        Ride ride = rideRepository.findByIdAndRider_Id(
                        rideId, rider.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ride not found for this rider"));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new IllegalArgumentException(
                    "Driver matching is only available for requested rides");
        }

        if (ride.getPickupLatitude() == null
                || ride.getPickupLongitude() == null) {
            throw new IllegalArgumentException(
                    "Ride pickup coordinates are missing");
        }

        List<RideStatus> activeStatuses = List.of(
                RideStatus.ACCEPTED,
                RideStatus.DRIVER_ARRIVING,
                RideStatus.IN_PROGRESS
        );

        return driverRepository.findByAvailableTrueAndVerifiedTrue()
                .stream()
                .filter(driver -> driver.getCurrentLatitude() != null
                        && driver.getCurrentLongitude() != null)
                .filter(driver -> !rideRepository
                        .existsByDriver_IdAndStatusIn(
                                driver.getId(), activeStatuses))
                .filter(driver -> !driverRepository
                        .existsById(driver.getId())
                        || driver.isVerified())
                .sorted(Comparator.comparing(driver ->
                        fareCalculatorService.calculateDistanceKm(
                                ride.getPickupLatitude(),
                                ride.getPickupLongitude(),
                                driver.getCurrentLatitude(),
                                driver.getCurrentLongitude())))
                .toList();
    }

    private Auth getUser(String email) {
        return authRepository.findByEmail(
                        email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Authenticated user not found"));
    }
}
