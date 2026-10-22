
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.entity.VehicleType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FareCalculatorService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private static final BigDecimal ROAD_DISTANCE_FACTOR =
            new BigDecimal("1.30");

    // Default fare configuration for each vehicle class.
    private static final BigDecimal BIKE_BASE_FARE =
            new BigDecimal("20.00");
    private static final BigDecimal BIKE_RATE_PER_KM =
            new BigDecimal("8.00");

    private static final BigDecimal ECONOMY_BASE_FARE =
            new BigDecimal("40.00");
    private static final BigDecimal ECONOMY_RATE_PER_KM =
            new BigDecimal("12.00");

    private static final BigDecimal COMFORT_BASE_FARE =
            new BigDecimal("60.00");
    private static final BigDecimal COMFORT_RATE_PER_KM =
            new BigDecimal("16.00");

    private static final BigDecimal MINIMUM_FARE =
            new BigDecimal("60.00");

    public BigDecimal calculateDistanceKm(
            BigDecimal pickupLatitude,
            BigDecimal pickupLongitude,
            BigDecimal dropoffLatitude,
            BigDecimal dropoffLongitude) {

        validateCoordinates(pickupLatitude, pickupLongitude);
        validateCoordinates(dropoffLatitude, dropoffLongitude);

        double lat1 = Math.toRadians(pickupLatitude.doubleValue());
        double lon1 = Math.toRadians(pickupLongitude.doubleValue());
        double lat2 = Math.toRadians(dropoffLatitude.doubleValue());
        double lon2 = Math.toRadians(dropoffLongitude.doubleValue());

        double latDifference = lat2 - lat1;
        double lonDifference = lon2 - lon1;

        double a =
                Math.sin(latDifference / 2)
                        * Math.sin(latDifference / 2)
                        + Math.cos(lat1)
                        * Math.cos(lat2)
                        * Math.sin(lonDifference / 2)
                        * Math.sin(lonDifference / 2);

        double boundedA = Math.max(0.0, Math.min(1.0, a));

        double centralAngle = 2 * Math.atan2(
                Math.sqrt(boundedA),
                Math.sqrt(1.0 - boundedA)
        );

        double straightLineKm = EARTH_RADIUS_KM * centralAngle;

        return BigDecimal.valueOf(straightLineKm)
                .multiply(ROAD_DISTANCE_FACTOR)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the fare for the selected vehicle class.
     * The distance must be the backend-calculated distance.
     */
    public BigDecimal calculateFare(
            BigDecimal distanceKm,
            VehicleType vehicleType) {

        if (distanceKm == null || distanceKm.signum() < 0) {
            throw new IllegalArgumentException(
                    "Distance must be zero or greater");
        }

        if (vehicleType == null) {
            throw new IllegalArgumentException(
                    "Vehicle type is required");
        }

        BigDecimal baseFare;
        BigDecimal ratePerKm;

        switch (vehicleType) {
            case BIKE -> {
                baseFare = BIKE_BASE_FARE;
                ratePerKm = BIKE_RATE_PER_KM;
            }
            case ECONOMY -> {
                baseFare = ECONOMY_BASE_FARE;
                ratePerKm = ECONOMY_RATE_PER_KM;
            }
            case COMFORT -> {
                baseFare = COMFORT_BASE_FARE;
                ratePerKm = COMFORT_RATE_PER_KM;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported vehicle type: " + vehicleType);
        }

        BigDecimal calculatedFare = baseFare.add(
                distanceKm.multiply(ratePerKm)
        );

        if (calculatedFare.compareTo(MINIMUM_FARE) < 0) {
            calculatedFare = MINIMUM_FARE;
        }

        return calculatedFare.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Backward-compatible fare calculation for existing callers.
     * Uses the previous Economy-like pricing configuration.
     */
    public BigDecimal calculateFare(BigDecimal distanceKm) {
        if (distanceKm == null || distanceKm.signum() < 0) {
            throw new IllegalArgumentException(
                    "Distance must be zero or greater");
        }

        BigDecimal calculatedFare =
                new BigDecimal("50.00")
                        .add(distanceKm.multiply(
                                new BigDecimal("15.00")));

        if (calculatedFare.compareTo(MINIMUM_FARE) < 0) {
            calculatedFare = MINIMUM_FARE;
        }

        return calculatedFare.setScale(0, RoundingMode.HALF_UP);
    }

    private void validateCoordinates(
            BigDecimal latitude,
            BigDecimal longitude) {

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException(
                    "Pickup and drop-off coordinates are required");
        }

        if (latitude.compareTo(new BigDecimal("-90")) < 0
                || latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90");
        }

        if (longitude.compareTo(new BigDecimal("-180")) < 0
                || longitude.compareTo(new BigDecimal("180")) > 0) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180");
        }
    }
}
