
package com.mayank.cabbooking.repository;

import com.mayank.cabbooking.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByUser_Id(Long userId);

    Optional<Driver> findByUser_EmailIgnoreCase(String email);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    List<Driver> findByAvailableTrueAndVerifiedTrue();

    List<Driver> findByVerifiedFalse();

    boolean existsByLicenseNumber(String licenseNumber);

    long countByVerifiedTrue();

    long countByVerifiedFalse();
}
