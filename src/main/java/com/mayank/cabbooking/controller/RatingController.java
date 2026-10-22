
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.entity.Rating;
import com.mayank.cabbooking.service.RatingService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    // Submit a rating for a completed ride
    @PostMapping("/ride/{rideId}")
    public ResponseEntity<Rating> submitRating(
            @PathVariable Long rideId,
            @RequestBody RatingRequest request) {

        if (request == null || request.stars() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stars are required");
        }

        try {
            Rating rating = ratingService.submitRating(
                    rideId,
                    request.stars(),
                    request.comment()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(rating);

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage());

        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    ex.getMessage());
        }
    }

    // Get all reviews for a driver
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Rating>> getDriverReviews(
            @PathVariable Long driverId) {

        return ResponseEntity.ok(
                ratingService.getDriverReviews(driverId));
    }

    // Get average rating and review count
    @GetMapping("/driver/{driverId}/average")
    public ResponseEntity<Map<String, Object>> getDriverAverage(
            @PathVariable Long driverId) {

        double average =
                ratingService.getAverageRating(driverId);

        long totalReviews =
                ratingService.getTotalReviews(driverId);

        return ResponseEntity.ok(Map.of(
                "driverId", driverId,
                "averageRating",
                Math.round(average * 100.0) / 100.0,
                "totalReviews", totalReviews
        ));
    }

    // Get the driver leaderboard
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {

        return ResponseEntity.ok(
                ratingService.getDriverLeaderboard());
    }

    // Request body for submitting a rating
    public record RatingRequest(
            Integer stars,
            String comment
    ) {}
}
