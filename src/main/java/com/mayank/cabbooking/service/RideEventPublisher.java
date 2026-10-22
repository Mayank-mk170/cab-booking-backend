
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.dto.RideEvent;
import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.entity.RideStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Publishes an update for a specific ride.
     *
     * Topic:
     * /topic/rides/{rideId}
     */
    public void publishRideUpdate(Ride ride) {
        Objects.requireNonNull(ride, "Ride must not be null");
        Objects.requireNonNull(ride.getId(), "Ride ID must not be null");
        Objects.requireNonNull(ride.getStatus(), "Ride status must not be null");

        RideEvent event = createRideEvent(ride);

        // Notify clients tracking this particular ride.
        messagingTemplate.convertAndSend(
                "/topic/rides/" + ride.getId(),
                event
        );

        // Notify driver dashboards that a new ride is available.
        if (ride.getStatus() == RideStatus.REQUESTED) {
            publishNewRideRequest(event);
        }

        log.debug(
                "Published ride update: rideId={}, status={}",
                ride.getId(),
                ride.getStatus()
        );
    }

    /**
     * Publishes a new ride request to the general driver feed.
     *
     * Topic:
     * /topic/rides/new
     */
    private void publishNewRideRequest(RideEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/rides/new",
                event
        );

        log.debug(
                "Published new ride request: rideId={}",
                event.getRideId()
        );
    }

    /**
     * Creates the WebSocket event from the current ride.
     */
    private RideEvent createRideEvent(Ride ride) {
        return new RideEvent(
                ride.getId(),
                ride.getStatus(),
                ride.getPickupLocation(),
                ride.getDropoffLocation(),
                ride.getFare(),
                LocalDateTime.now()
        );
    }
}
