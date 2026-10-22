package com.mayank.cabbooking.dto;

import com.mayank.cabbooking.entity.PaymentMethod;
import com.mayank.cabbooking.entity.VehicleType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RideRequest {

    @NotBlank(message = "Pickup location is required")
    @Size(max = 255,
            message = "Pickup location cannot exceed 255 characters")
    private String pickupLocation;

    @NotBlank(message = "Drop-off location is required")
    @Size(max = 255,
            message = "Drop-off location cannot exceed 255 characters")
    private String dropoffLocation;

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal pickupLongitude;

    @NotNull(message = "Drop-off latitude is required")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal dropoffLatitude;

    @NotNull(message = "Drop-off longitude is required")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal dropoffLongitude;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType = VehicleType.ECONOMY;

    private PaymentMethod paymentMethod = PaymentMethod.CASH;
}
