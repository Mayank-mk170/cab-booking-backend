
package com.mayank.cabbooking.repository;

import com.mayank.cabbooking.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    List<Vehicle> findByDriver_Id(Long driverId);

    List<Vehicle> findByDriver_IdAndActiveTrue(Long driverId);

    List<Vehicle> findByVerifiedTrueAndActiveTrue();

    boolean existsByRegistrationNumber(String registrationNumber);
}
