
package com.mayank.cabbooking.dto;

import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.entity.Vehicle;
import com.mayank.cabbooking.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class RideResponse {

    private Long id;
    private Long riderId;
    private Long driverId;

    private String driverName;
    private Double driverRating;

    private String pickupLocation;
    private String dropoffLocation;

    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;
    private BigDecimal dropoffLatitude;
    private BigDecimal dropoffLongitude;

    private BigDecimal distanceKm;
    private BigDecimal fare;

    private String status;
    private String vehicleType;

    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private String vehicleNumber;
    private String vehicleMake;
    private String vehicleModel;

    public RideResponse() {
    }

    public static RideResponse fromEntity(Ride ride) {
        return fromEntity(ride, null);
    }

    public static RideResponse fromEntity(
            Ride ride,
            VehicleRepository vehicleRepository) {

        RideResponse response = new RideResponse();

        response.setId(ride.getId());

        response.setRiderId(
                ride.getRider() != null
                        ? ride.getRider().getId()
                        : null
        );

        response.setDriverId(
                ride.getDriver() != null
                        ? ride.getDriver().getId()
                        : null
        );

        if (ride.getDriver() != null) {
            response.setDriverName(
                    ride.getDriver().getUser() != null
                            ? ride.getDriver().getUser().getName()
                            : null
            );

            response.setDriverRating(ride.getDriver().getRating());

            if (vehicleRepository != null) {
                List<Vehicle> vehicles =
                        vehicleRepository.findByDriver_IdAndActiveTrue(
                                ride.getDriver().getId()
                        );

                Vehicle vehicle = vehicles.stream()
                        .filter(Vehicle::isVerified)
                        .findFirst()
                        .orElseGet(() ->
                                vehicles.stream().findFirst().orElse(null)
                        );

                if (vehicle != null) {
                    response.setVehicleNumber(
                            vehicle.getRegistrationNumber()
                    );
                    response.setVehicleMake(vehicle.getMake());
                    response.setVehicleModel(vehicle.getModel());
                }
            }
        }

        response.setPickupLocation(ride.getPickupLocation());
        response.setDropoffLocation(ride.getDropoffLocation());

        response.setPickupLatitude(ride.getPickupLatitude());
        response.setPickupLongitude(ride.getPickupLongitude());
        response.setDropoffLatitude(ride.getDropoffLatitude());
        response.setDropoffLongitude(ride.getDropoffLongitude());

        response.setDistanceKm(ride.getDistanceKm());
        response.setFare(ride.getFare());

        response.setStatus(
                ride.getStatus() != null
                        ? ride.getStatus().name()
                        : null
        );

        response.setVehicleType(
                ride.getVehicleType() != null
                        ? ride.getVehicleType().name()
                        : null
        );

        response.setRequestedAt(ride.getRequestedAt());
        response.setStartedAt(ride.getStartedAt());
        response.setCompletedAt(ride.getCompletedAt());

        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRiderId() { return riderId; }
    public void setRiderId(Long riderId) { this.riderId = riderId; }

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public Double getDriverRating() { return driverRating; }
    public void setDriverRating(Double driverRating) { this.driverRating = driverRating; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }

    public BigDecimal getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(BigDecimal pickupLatitude) { this.pickupLatitude = pickupLatitude; }

    public BigDecimal getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(BigDecimal pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public BigDecimal getDropoffLatitude() { return dropoffLatitude; }
    public void setDropoffLatitude(BigDecimal dropoffLatitude) { this.dropoffLatitude = dropoffLatitude; }

    public BigDecimal getDropoffLongitude() { return dropoffLongitude; }
    public void setDropoffLongitude(BigDecimal dropoffLongitude) { this.dropoffLongitude = dropoffLongitude; }

    public BigDecimal getDistanceKm() { return distanceKm; }
    public void setDistanceKm(BigDecimal distanceKm) { this.distanceKm = distanceKm; }

    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
}
