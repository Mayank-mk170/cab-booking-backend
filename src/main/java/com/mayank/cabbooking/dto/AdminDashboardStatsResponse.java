package com.mayank.cabbooking.dto;

import java.math.BigDecimal;

public record AdminDashboardStatsResponse(
        long totalUsers,
        long totalDrivers,
        long verifiedDrivers,
        long pendingDrivers,
        long totalRides,
        long requestedRides,
        long activeRides,
        long completedRides,
        long cancelledRides,
        long rejectedRides,
        BigDecimal totalRevenue
) {}