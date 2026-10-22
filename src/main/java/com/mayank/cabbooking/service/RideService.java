
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.dto.RideRequest;
import com.mayank.cabbooking.dto.RideResponse;
import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Driver;
import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.entity.RideStatus;
import com.mayank.cabbooking.entity.Role;
import com.mayank.cabbooking.repository.AuthRepository;
import com.mayank.cabbooking.repository.DriverRepository;
import com.mayank.cabbooking.repository.RideRepository;
import com.mayank.cabbooking.repository.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final AuthRepository authRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final FareCalculatorService fareCalculatorService;
    private final RideEventPublisher rideEventPublisher;

    public RideService(
            RideRepository rideRepository,
            AuthRepository authRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            FareCalculatorService fareCalculatorService,
            RideEventPublisher rideEventPublisher) {

        this.rideRepository = rideRepository;
        this.authRepository = authRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.fareCalculatorService = fareCalculatorService;
        this.rideEventPublisher = rideEventPublisher;
    }

    private RideResponse toResponse(Ride ride) {
        return RideResponse.fromEntity(ride, vehicleRepository);
    }

    @Transactional
    public RideResponse requestRide(String email, RideRequest request) {

        Auth rider = getUser(email);
        requireRole(rider, Role.RIDER);

        List<RideStatus> activeStatuses = List.of(
                RideStatus.REQUESTED,
                RideStatus.ACCEPTED,
                RideStatus.DRIVER_ARRIVING,
                RideStatus.IN_PROGRESS
        );

        if (rideRepository.existsByRider_IdAndStatusIn(
                rider.getId(), activeStatuses)) {
            throw new IllegalArgumentException(
                    "You already have an active ride");
        }

        BigDecimal distanceKm =
                fareCalculatorService.calculateDistanceKm(
                        request.getPickupLatitude(),
                        request.getPickupLongitude(),
                        request.getDropoffLatitude(),
                        request.getDropoffLongitude()
                );

        if (distanceKm.compareTo(new BigDecimal("0.10")) < 0) {
            throw new IllegalArgumentException(
                    "Pickup and drop-off must be at least 0.10 km apart");
        }

        BigDecimal fare =
                fareCalculatorService.calculateFare(distanceKm);

        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setPickupLocation(request.getPickupLocation().trim());
        ride.setDropoffLocation(request.getDropoffLocation().trim());

        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setDropoffLatitude(request.getDropoffLatitude());
        ride.setDropoffLongitude(request.getDropoffLongitude());

        ride.setDistanceKm(distanceKm);
        ride.setFare(fare);
        ride.setStatus(RideStatus.REQUESTED);

        Ride savedRide = rideRepository.save(ride);
        rideEventPublisher.publishRideUpdate(savedRide);

        return toResponse(savedRide);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getMyRiderRides(String email) {

        Auth rider = getUser(email);
        requireRole(rider, Role.RIDER);

        return rideRepository
                .findByRider_IdOrderByRequestedAtDesc(rider.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RideResponse getMyRide(String email, Long rideId) {

        Auth rider = getUser(email);
        requireRole(rider, Role.RIDER);

        Ride ride = rideRepository
                .findByIdAndRider_Id(rideId, rider.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ride not found for this rider"));

        return toResponse(ride);
    }

    @Transactional
    public RideResponse cancelRide(String email, Long rideId) {

        Auth rider = getUser(email);
        requireRole(rider, Role.RIDER);

        Ride ride = rideRepository
                .findByIdAndRider_Id(rideId, rider.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ride not found for this rider"));

        if (ride.getStatus() != RideStatus.REQUESTED
                && ride.getStatus() != RideStatus.ACCEPTED
                && ride.getStatus() != RideStatus.DRIVER_ARRIVING) {
            throw new IllegalArgumentException(
                    "This ride can no longer be cancelled");
        }

        Driver assignedDriver = ride.getDriver();
        ride.setStatus(RideStatus.CANCELLED);

        if (assignedDriver != null) {
            assignedDriver.setAvailable(true);
            driverRepository.save(assignedDriver);
        }

        Ride savedRide = rideRepository.save(ride);
        rideEventPublisher.publishRideUpdate(savedRide);

        return toResponse(savedRide);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getAvailableRides(String email) {

        getVerifiedAvailableDriver(email);

        return rideRepository
                .findByStatusOrderByRequestedAtAsc(RideStatus.REQUESTED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getMyDriverRides(String email) {

        Driver driver = getDriver(email);

        return rideRepository
                .findByDriver_IdOrderByRequestedAtDesc(driver.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RideResponse acceptRide(String email, Long rideId) {

        Driver driver = getVerifiedAvailableDriver(email);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ride not found"));

        if (ride.getStatus() != RideStatus.REQUESTED
                || ride.getDriver() != null) {
            throw new IllegalArgumentException(
                    "This ride is no longer available");
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        driver.setAvailable(false);

        driverRepository.save(driver);

        Ride savedRide = rideRepository.save(ride);
        rideEventPublisher.publishRideUpdate(savedRide);

        return toResponse(savedRide);
    }

    @Transactional
    public RideResponse updateRideStatus(
            String email, Long rideId, RideStatus newStatus) {

        Driver driver = getDriver(email);

        Ride ride = rideRepository
                .findByIdAndDriver_Id(rideId, driver.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ride not found for this driver"));

        RideStatus current = ride.getStatus();

        boolean validTransition =
                (current == RideStatus.ACCEPTED
                        && newStatus == RideStatus.DRIVER_ARRIVING)
                        || (current == RideStatus.DRIVER_ARRIVING
                        && newStatus == RideStatus.IN_PROGRESS)
                        || (current == RideStatus.IN_PROGRESS
                        && newStatus == RideStatus.COMPLETED);

        if (!validTransition) {
            throw new IllegalArgumentException(
                    "Invalid ride status transition: "
                            + current + " -> " + newStatus);
        }

        ride.setStatus(newStatus);

        if (newStatus == RideStatus.IN_PROGRESS) {
            ride.setStartedAt(LocalDateTime.now());
        }

        if (newStatus == RideStatus.COMPLETED) {
            ride.setCompletedAt(LocalDateTime.now());
            driver.setAvailable(true);
            driverRepository.save(driver);
        }

        Ride savedRide = rideRepository.save(ride);
        rideEventPublisher.publishRideUpdate(savedRide);

        return toResponse(savedRide);
    }

    private Auth getUser(String email) {
        return authRepository.findByEmail(
                        email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Authenticated user not found"));
    }

    private void requireRole(Auth user, Role expectedRole) {
        if (user.getRole() != expectedRole) {
            throw new IllegalArgumentException(
                    "This operation requires the " + expectedRole + " role");
        }
    }

    private Driver getDriver(String email) {
        Auth user = getUser(email);
        requireRole(user, Role.DRIVER);

        return driverRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Driver profile not found"));
    }

    private Driver getVerifiedAvailableDriver(String email) {
        Driver driver = getDriver(email);

        if (!driver.isVerified()) {
            throw new IllegalArgumentException(
                    "Your driver profile is not verified");
        }

        if (!driver.isAvailable()) {
            throw new IllegalArgumentException(
                    "You are currently unavailable for rides");
        }

        return driver;
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getAllRidesForAdmin(String email) {

        Auth user = getUser(email);
        requireRole(user, Role.ADMIN);

        return rideRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RideResponse cancelDriverRide(String email, Long rideId) {

        Driver driver = getDriver(email);

        Ride ride = rideRepository
                .findByIdAndDriver_Id(rideId, driver.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ride not found for this driver"));

        if (ride.getStatus() != RideStatus.ACCEPTED
                && ride.getStatus() != RideStatus.DRIVER_ARRIVING) {
            throw new IllegalArgumentException(
                    "Only accepted or arriving rides can be cancelled");
        }

        ride.setStatus(RideStatus.CANCELLED);

        driver.setAvailable(true);
        driverRepository.save(driver);

        Ride savedRide = rideRepository.save(ride);

        rideEventPublisher.publishRideUpdate(savedRide);

        return toResponse(savedRide);
    }
}
