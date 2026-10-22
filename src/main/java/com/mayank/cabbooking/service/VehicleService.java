
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.dto.VehicleRequest;
import com.mayank.cabbooking.dto.VehicleResponse;
import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Driver;
import com.mayank.cabbooking.entity.Role;
import com.mayank.cabbooking.entity.Vehicle;
import com.mayank.cabbooking.repository.AuthRepository;
import com.mayank.cabbooking.repository.DriverRepository;
import com.mayank.cabbooking.repository.VehicleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final AuthRepository authRepository;

    public VehicleService(
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            AuthRepository authRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.authRepository = authRepository;
    }

    @Transactional
    public VehicleResponse registerVehicle(
            String authenticatedEmail,
            VehicleRequest request) {

        String email = authenticatedEmail.trim().toLowerCase(Locale.ROOT);

        Auth user = authRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user not found"));

        if (user.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException(
                    "Only registered drivers can add vehicles");
        }

        Driver driver = driverRepository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Driver profile not found. Create your driver profile first."));

        String registrationNumber = request.getRegistrationNumber()
                .trim().toUpperCase(Locale.ROOT);

        if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new IllegalArgumentException(
                    "A vehicle with this registration number already exists");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setDriver(driver);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setVehicleType(request.getVehicleType().trim());
        vehicle.setMake(clean(request.getMake()));
        vehicle.setModel(clean(request.getModel()));
        vehicle.setColor(clean(request.getColor()));

        // New vehicles must be verified before being used for rides.
        vehicle.setVerified(false);
        vehicle.setActive(true);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponse.fromEntity(savedVehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(String authenticatedEmail) {

        String email = authenticatedEmail.trim().toLowerCase(Locale.ROOT);

        Auth user = authRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user not found"));

        if (user.getRole() != Role.DRIVER) {
            throw new IllegalArgumentException(
                    "Only drivers can view their vehicle list");
        }

        Driver driver = driverRepository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Driver profile not found"));

        return vehicleRepository.findByDriver_Id(driver.getId())
                .stream()
                .map(VehicleResponse::fromEntity)
                .toList();
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
