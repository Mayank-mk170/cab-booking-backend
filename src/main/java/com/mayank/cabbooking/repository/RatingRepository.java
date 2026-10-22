
package com.mayank.cabbooking.repository;

import com.mayank.cabbooking.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository
        extends JpaRepository<Rating, Long> {

    Optional<Rating> findByRideId(Long rideId);

    List<Rating> findByRideDriverId(Long driverId);

    boolean existsByRideId(Long rideId);

    long countByRideDriverId(Long driverId);
}
