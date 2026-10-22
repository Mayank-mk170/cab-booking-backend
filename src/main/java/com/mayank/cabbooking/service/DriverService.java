 package com.mayank.cabbooking.service;

import com.mayank.cabbooking.dto.DriverRequest;
import com.mayank.cabbooking.dto.DriverResponse;
import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Driver;
import com.mayank.cabbooking.entity.Role;
import com.mayank.cabbooking.entity.RideStatus;
import com.mayank.cabbooking.repository.AuthRepository;
import com.mayank.cabbooking.repository.DriverRepository;
import com.mayank.cabbooking.repository.RideRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DriverService {

    private final AuthRepository authRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;

    public DriverService(
            AuthRepository authRepository,
            DriverRepository driverRepository,
            RideRepository rideRepository) {

        this.authRepository = authRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    // Create a driver profile without requiring date of birth.
    @Transactional
    public DriverResponse createProfile(
            String email,
            DriverRequest request) {

        Auth user = getUser(email);

        if (user.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException(
                    "Only DRIVER accounts can create driver profiles");
        }

        if (driverRepository.findByUser_Id(user.getId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Driver profile already exists");
        }

        String license = normalizeLicense(request.getLicenseNumber());

        if (driverRepository.existsByLicenseNumber(license)) {
            throw new IllegalArgumentException(
                    "License number is already registered");
        }

        Driver driver = new Driver();
        driver.setUser(user);
        driver.setLicenseNumber(license);

        // Date of birth is optional.
        driver.setDateOfBirth(request.getDateOfBirth());

        driver.setVerified(false);
        driver.setAvailable(false);

        Driver savedDriver = driverRepository.save(driver);

        return DriverResponse.fromEntity(savedDriver);
    }

    // Get the authenticated driver's profile.
    @Transactional(readOnly = true)
    public DriverResponse getMyProfile(String email) {

        Auth user = getUser(email);
        Driver driver = getDriverByUserId(user.getId());

        return DriverResponse.fromEntity(driver);
    }

    // Get a driver profile by its ID.
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long id) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver not found"));

        return DriverResponse.fromEntity(driver);
    }

    // Get all driver profiles for the admin workflow.
    @Transactional(readOnly = true)
    public List<DriverResponse> getAllDrivers() {

        return driverRepository.findAll()
                .stream()
                .map(DriverResponse::fromEntity)
                .toList();
    }

    // Update licence details and require verification again
    // only when submitted details have changed.
    @Transactional
    public DriverResponse updateMyProfile(
            String email,
            DriverRequest request) {

        Auth user = getUser(email);
        Driver driver = getDriverByUserId(user.getId());

        String license = normalizeLicense(request.getLicenseNumber());

        driverRepository.findByLicenseNumber(license)
                .filter(existing ->
                        !existing.getId().equals(driver.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "License number is already registered");
                });

        boolean licenseChanged =
                !license.equals(driver.getLicenseNumber());

        // If DOB is omitted, preserve the existing value.
        boolean dateOfBirthChanged =
                request.getDateOfBirth() != null
                        && !Objects.equals(
                        request.getDateOfBirth(),
                        driver.getDateOfBirth());

        driver.setLicenseNumber(license);

        if (request.getDateOfBirth() != null) {
            driver.setDateOfBirth(request.getDateOfBirth());
        }

        if (licenseChanged || dateOfBirthChanged) {
            driver.setVerified(false);
            driver.setAvailable(false);
        }

        Driver savedDriver = driverRepository.save(driver);

        return DriverResponse.fromEntity(savedDriver);
    }

    // Delete the authenticated driver's profile.
    @Transactional
    public void deleteMyProfile(String email) {

        Auth user = getUser(email);
        Driver driver = getDriverByUserId(user.getId());

        // Existing ride or vehicle references may prevent deletion.
        driverRepository.delete(driver);
    }

    // Admin: verify or unverify a driver.
    @Transactional
    public DriverResponse setVerification(
            Long id,
            boolean verified) {

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver not found"));

        driver.setVerified(verified);

        if (!verified) {
            driver.setAvailable(false);
        }

        Driver savedDriver = driverRepository.save(driver);

        return DriverResponse.fromEntity(savedDriver);
    }

    // Change the authenticated driver's availability.
    @Transactional
    public DriverResponse setAvailability(
            String email,
            boolean available) {

        Auth user = getUser(email);

        if (user.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException(
                    "Only DRIVER accounts can change availability");
        }

        Driver driver = getDriverByUserId(user.getId());

        if (available && !driver.isVerified()) {
            throw new IllegalArgumentException(
                    "Your driver profile must be verified first");
        }

        if (!available) {
            boolean rideInProgress =
                    rideRepository.existsByDriver_IdAndStatus(
                            driver.getId(),
                            RideStatus.IN_PROGRESS);

            if (rideInProgress) {
                throw new IllegalArgumentException(
                        "You cannot go offline during an active ride");
            }
        }

        driver.setAvailable(available);

        Driver savedDriver = driverRepository.save(driver);

        return DriverResponse.fromEntity(savedDriver);
    }

    // Find the authenticated user by email.
    private Auth getUser(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email is required");
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        return authRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found"));
    }

    // Find a driver profile using the owning user's ID.
    private Driver getDriverByUserId(Long userId) {

        return driverRepository.findByUser_Id(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found"));
    }

    // Normalize licence numbers before saving or comparing.
    private String normalizeLicense(String licenseNumber) {

        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "License number is required");
        }

        String normalized = licenseNumber.trim().toUpperCase(Locale.ROOT);

        if (normalized.length() > 50) {
            throw new IllegalArgumentException(
                    "License number cannot exceed 50 characters");
        }

        return normalized;
    }
}
