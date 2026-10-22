
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.entity.Driver;
import com.mayank.cabbooking.entity.Payment;
import com.mayank.cabbooking.entity.PaymentStatus;
import com.mayank.cabbooking.entity.Ride;
import com.mayank.cabbooking.entity.RideStatus;
import com.mayank.cabbooking.repository.DriverRepository;
import com.mayank.cabbooking.repository.PaymentRepository;
import com.mayank.cabbooking.repository.RideRepository;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public PaymentService(
            PaymentRepository paymentRepository,
            RideRepository rideRepository,
            DriverRepository driverRepository) {

        this.paymentRepository = paymentRepository;
        this.rideRepository = rideRepository;
        this.driverRepository = driverRepository;
    }

    // =====================================================
    // 1. INITIALIZE STRIPE
    // =====================================================

    @PostConstruct
    public void initializeStripe() {

        if (stripeSecretKey == null
                || stripeSecretKey.isBlank()
                || !stripeSecretKey.startsWith("sk_")) {

            throw new IllegalStateException(
                    "A valid Stripe secret API key must be configured.");
        }

        Stripe.apiKey = stripeSecretKey;
    }

    // =====================================================
    // 2. CREATE ONLINE PAYMENT INTENT
    // =====================================================

    @Transactional
    public PaymentIntent createPaymentIntent(
            Long rideId,
            Long riderId) throws StripeException {

        Ride ride = rideRepository
                .findByIdAndRider_Id(rideId, riderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ride not found or access denied"));

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Payment is available only for completed rides");
        }

        BigDecimal fare = ride.getFare();

        if (fare == null || fare.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Ride fare is missing or invalid");
        }

        boolean alreadyPaid = paymentRepository
                .findByRide_IdOrderByCreatedAtDesc(rideId)
                .stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.SUCCEEDED);

        if (alreadyPaid) {
            throw new IllegalStateException(
                    "This ride has already been paid");
        }

        long amountInPaise = fare
                .movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(amountInPaise)
                        .setCurrency("inr")
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams
                                        .AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build())
                        .putMetadata("rideId", rideId.toString())
                        .putMetadata("riderId", riderId.toString())
                        .build();

        PaymentIntent intent = PaymentIntent.create(params);

        Payment payment = new Payment();
        payment.setRide(ride);
        payment.setAmount(fare);
        payment.setCurrency("INR");
        payment.setPaymentMethod("ONLINE");
        payment.setStripePaymentIntentId(intent.getId());
        payment.setStatus(PaymentStatus.PENDING);

        paymentRepository.save(payment);

        return intent;
    }

    // =====================================================
    // 3. PROCESS VERIFIED STRIPE WEBHOOK EVENTS
    // =====================================================

    @Transactional
    public void processWebhookEvent(Event event) {

        String eventType = event.getType();

        if (!"payment_intent.succeeded".equals(eventType)
                && !"payment_intent.payment_failed".equals(eventType)
                && !"payment_intent.canceled".equals(eventType)) {
            return;
        }

        StripeObject stripeObject = event
                .getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException(
                        "Unable to deserialize Stripe PaymentIntent"));

        if (!(stripeObject instanceof PaymentIntent intent)) {
            throw new IllegalStateException(
                    "Webhook object is not a PaymentIntent");
        }

        Payment payment = paymentRepository
                .findByStripePaymentIntentId(intent.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment record not found for Stripe PaymentIntent"));

        // Never downgrade a successful payment.
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }

        switch (eventType) {

            case "payment_intent.succeeded" -> {

                BigDecimal storedAmount = payment.getAmount();

                if (storedAmount == null) {
                    throw new IllegalStateException(
                            "Stored payment amount is missing");
                }

                long expectedAmountInPaise = storedAmount
                        .movePointRight(2)
                        .setScale(0, RoundingMode.UNNECESSARY)
                        .longValueExact();

                Long receivedAmount = intent.getAmountReceived();

                if (receivedAmount == null
                        || receivedAmount.longValue()
                        != expectedAmountInPaise
                        || !"inr".equalsIgnoreCase(intent.getCurrency())
                        || !"INR".equalsIgnoreCase(payment.getCurrency())
                        || !"ONLINE".equalsIgnoreCase(
                        payment.getPaymentMethod())) {

                    throw new IllegalStateException(
                            "Stripe payment amount, currency, or method mismatch");
                }

                payment.setStatus(PaymentStatus.SUCCEEDED);
            }

            case "payment_intent.payment_failed" ->
                    payment.setStatus(PaymentStatus.FAILED);

            case "payment_intent.canceled" ->
                    payment.setStatus(PaymentStatus.CANCELLED);

            default -> {
                return;
            }
        }

        paymentRepository.save(payment);
    }

    // =====================================================
    // 4. CONFIRM CASH PAYMENT
    // =====================================================

    @Transactional
    public Map<String, Object> confirmCashPayment(
            Long rideId,
            String driverEmail) {

        if (driverEmail == null || driverEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated driver email is required");
        }

        Driver driver = driverRepository
                .findByUser_EmailIgnoreCase(driverEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Driver profile not found"));

        Ride ride = rideRepository
                .findByIdAndDriver_Id(rideId, driver.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ride not found for this driver"));

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cash can be confirmed only after the ride is completed");
        }

        var ridePayments = paymentRepository
                .findByRide_IdOrderByCreatedAtDesc(rideId);

        Payment successfulPayment = ridePayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .findFirst()
                .orElse(null);

        if (successfulPayment != null) {

            if ("CASH".equalsIgnoreCase(
                    successfulPayment.getPaymentMethod())) {
                return receiptMap(successfulPayment);
            }

            throw new IllegalStateException(
                    "A successful payment already exists for this ride");
        }

        boolean pendingOnlinePaymentExists = ridePayments.stream()
                .anyMatch(p ->
                        "ONLINE".equalsIgnoreCase(p.getPaymentMethod())
                                && p.getStatus() == PaymentStatus.PENDING);

        if (pendingOnlinePaymentExists) {
            throw new IllegalStateException(
                    "An online payment is pending. Resolve it before confirming cash.");
        }

        BigDecimal fare = ride.getFare();

        if (fare == null || fare.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Ride fare is missing or invalid");
        }

        Payment payment = new Payment();
        payment.setRide(ride);
        payment.setAmount(fare);
        payment.setCurrency("INR");
        payment.setPaymentMethod("CASH");
        payment.setStatus(PaymentStatus.SUCCEEDED);

        Payment savedPayment = paymentRepository.save(payment);

        return receiptMap(savedPayment);
    }

    // =====================================================
    // 5. GET RECEIPT FOR AN AUTHORIZED RIDER
    // =====================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getReceipt(
            Long rideId,
            Long riderId) {

        Payment payment = paymentRepository
                .findByRide_IdOrderByCreatedAtDesc(rideId)
                .stream()
                .filter(p -> p.getRide() != null
                        && p.getRide().getRider() != null
                        && riderId.equals(
                        p.getRide().getRider().getId()))
                .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Successful payment not found for this ride"));

        return receiptMap(payment);
    }

    // =====================================================
    // 6. BUILD RECEIPT DATA FOR JSON, PDF AND EMAIL
    // =====================================================

    private Map<String, Object> receiptMap(Payment payment) {

        Map<String, Object> receipt = new LinkedHashMap<>();

        receipt.put(
                "message",
                "Receipt generated successfully");

        receipt.put(
                "receiptNumber",
                "RIDEGO-" + payment.getId());

        receipt.put(
                "rideId",
                payment.getRide().getId());

        receipt.put(
                "amount",
                payment.getAmount());

        receipt.put(
                "currency",
                payment.getCurrency());

        receipt.put(
                "paymentMethod",
                payment.getPaymentMethod());

        receipt.put(
                "paymentStatus",
                payment.getStatus().name());

        receipt.put(
                "stripePaymentIntentId",
                payment.getStripePaymentIntentId());

        receipt.put(
                "paidAt",
                payment.getUpdatedAt());

        return receipt;
    }
}
