
package com.mayank.cabbooking.dto;

import com.mayank.cabbooking.entity.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RideEvent {

    private Long rideId;
    private RideStatus status;
    private String pickupLocation;
    private String dropoffLocation;
    private BigDecimal fare;
    private LocalDateTime timestamp;
}
