
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.dto.AdminDashboardStatsResponse;
import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Driver;
import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.entity.RideStatus;
import com.mayank.cabbooking.repository.AuthRepository;
import com.mayank.cabbooking.repository.DriverRepository;
import com.mayank.cabbooking.repository.RideRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminDashboardService {

    private final AuthRepository authRepository;
    private final DriverRepository driverRepository;
    private final RideRepository rideRepository;

    public AdminDashboardService(
            AuthRepository authRepository,
            DriverRepository driverRepository,
            RideRepository rideRepository) {
        this.authRepository = authRepository;
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    public AdminDashboardStatsResponse getStats() {
        List<Ride> rides = rideRepository.findAll();

        long requested = rides.stream()
                .filter(r -> r.getStatus() == RideStatus.REQUESTED)
                .count();

        long active = rides.stream()
                .filter(r ->
                        r.getStatus() == RideStatus.ACCEPTED
                                || r.getStatus() == RideStatus.DRIVER_ARRIVING
                                || r.getStatus() == RideStatus.IN_PROGRESS)
                .count();

        long completed = rides.stream()
                .filter(r -> r.getStatus() == RideStatus.COMPLETED)
                .count();

        long cancelled = rides.stream()
                .filter(r -> r.getStatus() == RideStatus.CANCELLED)
                .count();

        BigDecimal revenue = rides.stream()
                .filter(r -> r.getStatus() == RideStatus.COMPLETED)
                .map(Ride::getFare)
                .filter(fare -> fare != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdminDashboardStatsResponse(
                authRepository.count(),
                driverRepository.count(),
                driverRepository.countByVerifiedTrue(),
                driverRepository.countByVerifiedFalse(),
                rides.size(),
                requested,
                active,
                completed,
                cancelled,
                0,
                revenue
        );
    }
}
