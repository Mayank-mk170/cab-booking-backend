
package com.mayank.cabbooking.repository;

import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.entity.RideStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByRider_IdOrderByRequestedAtDesc(Long riderId);

    List<Ride> findByDriver_IdOrderByRequestedAtDesc(Long driverId);

    List<Ride> findByStatusOrderByRequestedAtAsc(RideStatus status);

    Optional<Ride> findByIdAndRider_Id(Long id, Long riderId);

    Optional<Ride> findByIdAndDriver_Id(Long id, Long driverId);

    boolean existsByRider_IdAndStatusIn(
            Long riderId, List<RideStatus> statuses);

    boolean existsByDriver_IdAndStatus(Long id, RideStatus rideStatus);

    boolean existsByDriver_IdAndStatusIn(
            Long driverId, List<RideStatus> statuses);
}
