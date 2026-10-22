
package com.mayank.cabbooking.repository;

import com.mayank.cabbooking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByRide_IdOrderByCreatedAtDesc(Long rideId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
