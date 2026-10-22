
package com.mayank.cabbooking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DriverLocationEvent(
        Long rideId,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime updatedAt
) {}
