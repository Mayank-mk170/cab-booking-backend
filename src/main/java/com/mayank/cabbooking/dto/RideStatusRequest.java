
package com.mayank.cabbooking.dto;

import com.mayank.cabbooking.entity.RideStatus;
import jakarta.validation.constraints.NotNull;

public class RideStatusRequest {

    @NotNull(message = "Ride status is required")
    private RideStatus status;

    public RideStatusRequest() {
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }
}
