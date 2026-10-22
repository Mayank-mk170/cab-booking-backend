
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.entity.Rating;
import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.repository.RatingRepository;
import com.mayank.cabbooking.repository.RideRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RideRepository rideRepository;

    public RatingService(
            RatingRepository ratingRepository,
            RideRepository rideRepository) {
        this.ratingRepository = ratingRepository;
        this.rideRepository = rideRepository;
    }

    // Submit a rating for a completed ride
    @Transactional
    public Rating submitRating(
            Long rideId,
            Integer stars,
            String comment) {

        if (stars == null || stars < 1 || stars > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5 stars");
        }

        if (comment != null && comment.trim().length() > 1000) {
            throw new IllegalArgumentException(
                    "Comment cannot exceed 1000 characters");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ride not found"));

        if (!"COMPLETED".equalsIgnoreCase(
                String.valueOf(ride.getStatus()))) {
            throw new IllegalStateException(
                    "Only completed rides can be rated");
        }

        if (ratingRepository.existsByRideId(rideId)) {
            throw new IllegalStateException(
                    "This ride has already been rated");
        }

        Rating rating = new Rating();
        rating.setRide(ride);
        rating.setStars(stars);
        rating.setComment(
                comment == null || comment.isBlank()
                        ? null
                        : comment.trim());

        return ratingRepository.save(rating);
    }

    // Calculate a driver's average rating
    @Transactional(readOnly = true)
    public double getAverageRating(Long driverId) {
        List<Rating> ratings =
                ratingRepository.findByRideDriverId(driverId);

        return ratings.stream()
                .mapToInt(Rating::getStars)
                .average()
                .orElse(0.0);
    }

    // Get all reviews for a driver
    @Transactional(readOnly = true)
    public List<Rating> getDriverReviews(Long driverId) {
        return ratingRepository.findByRideDriverId(driverId);
    }

    // Count reviews for a driver
    @Transactional(readOnly = true)
    public long getTotalReviews(Long driverId) {
        return ratingRepository.countByRideDriverId(driverId);
    }

    // Rank drivers with at least 3 reviews
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDriverLeaderboard() {

        List<Rating> allRatings = ratingRepository.findAll();

        Map<Long, List<Rating>> ratingsByDriver =
                allRatings.stream()
                        .collect(Collectors.groupingBy(
                                rating -> rating.getRide()
                                        .getDriver().getId()
                        ));

        return ratingsByDriver.entrySet().stream()
                .map(entry -> {
                    Long driverId = entry.getKey();
                    List<Rating> reviews = entry.getValue();

                    double average = reviews.stream()
                            .mapToInt(Rating::getStars)
                            .average()
                            .orElse(0.0);

                    return Map.<String, Object>of(
                            "driverId", driverId,
                            "averageRating",
                            Math.round(average * 100.0) / 100.0,
                            "totalReviews", reviews.size()
                    );
                })
                .filter(driver ->
                        ((Number) driver.get("totalReviews"))
                                .intValue() >= 3)
                .sorted(
                        Comparator
                                .<Map<String, Object>>
                                        comparingDouble(driver ->
                                        ((Number) driver.get(
                                                "averageRating"))
                                                .doubleValue())
                                .reversed()
                                .thenComparing(
                                        Comparator.comparingInt(
                                                (Map<String, Object> driver) ->
                                                        ((Number) driver.get(
                                                                "totalReviews"))
                                                                .intValue()
                                        ).reversed()
                                )
                )
                .limit(100)
                .toList();
    }
}
