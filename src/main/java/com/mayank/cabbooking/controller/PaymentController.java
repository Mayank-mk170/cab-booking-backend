
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Role;
import com.mayank.cabbooking.repository.AuthRepository;
import com.mayank.cabbooking.service.PaymentService;
import com.mayank.cabbooking.service.ReceiptEmailService;
import com.mayank.cabbooking.service.ReceiptPdfService;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthRepository authRepository;
    private final ReceiptPdfService receiptPdfService;
    private final ReceiptEmailService receiptEmailService;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    public PaymentController(
            PaymentService paymentService,
            AuthRepository authRepository,
            ReceiptPdfService receiptPdfService,
            ReceiptEmailService receiptEmailService) {

        this.paymentService = paymentService;
        this.authRepository = authRepository;
        this.receiptPdfService = receiptPdfService;
        this.receiptEmailService = receiptEmailService;
    }

    // =====================================================
    // 1. CREATE STRIPE PAYMENT INTENT
    // =====================================================

    @PostMapping("/ride/{rideId}/intent")
    public ResponseEntity<?> createPaymentIntent(
            @PathVariable Long rideId,
            Principal principal) {

        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        try {
            Auth rider = getAuthenticatedUser(principal);

            if (rider.getRole() != Role.RIDER) {
                return error(HttpStatus.FORBIDDEN,
                        "Only riders can initiate ride payments");
            }

            PaymentIntent intent =
                    paymentService.createPaymentIntent(
                            rideId, rider.getId());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message",
                    "Payment intent created successfully");
            response.put("paymentIntentId", intent.getId());
            response.put("clientSecret", intent.getClientSecret());

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            return error(e.getStatusCode(), safeReason(e));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.NOT_FOUND, safeMessage(e));
        } catch (IllegalStateException e) {
            return error(HttpStatus.BAD_REQUEST, safeMessage(e));
        } catch (StripeException e) {
            System.err.println("Stripe error: " + e.getMessage());
            return error(HttpStatus.BAD_GATEWAY,
                    "Unable to create payment with Stripe");
        }
    }

    // =====================================================
    // 2. DRIVER CONFIRMS CASH PAYMENT
    // =====================================================

    @PostMapping("/ride/{rideId}/cash/confirm")
    public ResponseEntity<?> confirmCashPayment(
            @PathVariable Long rideId,
            Principal principal) {

        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        try {
            Auth driverUser = getAuthenticatedUser(principal);

            if (driverUser.getRole() != Role.DRIVER) {
                return error(HttpStatus.FORBIDDEN,
                        "Only drivers can confirm cash payments");
            }

            Map<String, Object> receipt =
                    paymentService.confirmCashPayment(
                            rideId, driverUser.getEmail());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message",
                    "Cash payment confirmed successfully");
            response.put("receipt", receipt);

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            return error(e.getStatusCode(), safeReason(e));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.NOT_FOUND, safeMessage(e));
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, safeMessage(e));
        }
    }

    // =====================================================
    // 3. STRIPE WEBHOOK
    // =====================================================

    @PostMapping(
            value = "/webhook",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(
                    value = "Stripe-Signature",
                    required = false)
            String signature) {

        if (signature == null || signature.isBlank()) {
            return error(HttpStatus.BAD_REQUEST,
                    "Missing Stripe signature");
        }

        if (stripeWebhookSecret == null
                || stripeWebhookSecret.isBlank()) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Webhook configuration is missing");
        }

        final Event event;

        try {
            event = Webhook.constructEvent(
                    payload, signature, stripeWebhookSecret);

        } catch (SignatureVerificationException e) {
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid Stripe webhook signature");
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid webhook payload");
        }

        try {
            paymentService.processWebhookEvent(event);

            return ResponseEntity.ok(Map.of(
                    "received", true,
                    "message",
                    "Stripe webhook processed successfully"));

        } catch (Exception e) {
            System.err.println(
                    "Stripe webhook processing error: "
                            + e.getMessage());

            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Webhook processing failed; Stripe may retry");
        }
    }

    // =====================================================
    // 4. GET JSON RECEIPT
    // =====================================================

    @GetMapping("/ride/{rideId}/receipt")
    public ResponseEntity<?> getReceipt(
            @PathVariable Long rideId,
            Principal principal) {

        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        try {
            Auth rider = getAuthenticatedUser(principal);

            if (rider.getRole() != Role.RIDER) {
                return error(HttpStatus.FORBIDDEN,
                        "Only riders can access receipts");
            }

            Map<String, Object> receipt =
                    paymentService.getReceipt(
                            rideId, rider.getId());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message",
                    "Receipt retrieved successfully");
            response.put("receipt", receipt);

            return ResponseEntity.ok(response);

        } catch (ResponseStatusException e) {
            return error(e.getStatusCode(), safeReason(e));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.NOT_FOUND, safeMessage(e));
        } catch (IllegalStateException e) {
            return error(HttpStatus.CONFLICT, safeMessage(e));
        }
    }

    // =====================================================
    // 5. DOWNLOAD PDF RECEIPT
    // =====================================================

    @GetMapping("/ride/{rideId}/receipt/pdf")
    public ResponseEntity<?> downloadReceiptPdf(
            @PathVariable Long rideId,
            Principal principal) {

        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        try {
            Auth rider = getAuthenticatedUser(principal);

            if (rider.getRole() != Role.RIDER) {
                return error(HttpStatus.FORBIDDEN,
                        "Only riders can download receipts");
            }

            Map<String, Object> receipt =
                    paymentService.getReceipt(
                            rideId, rider.getId());

            // Must match the method name in ReceiptPdfService.
            byte[] pdfBytes =
                    receiptPdfService.generateReceiptPdf(receipt);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(
                                            "RideGo-Receipt-"
                                                    + rideId + ".pdf")
                                    .build()
                                    .toString())
                    .body(pdfBytes);

        } catch (ResponseStatusException e) {
            return error(e.getStatusCode(), safeReason(e));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.NOT_FOUND, safeMessage(e));
        } catch (IllegalStateException e) {
            System.err.println(
                    "PDF receipt error: " + e.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate PDF receipt");
        }
    }

    // =====================================================
    // 6. EMAIL PDF RECEIPT TO RIDER
    // =====================================================

    @PostMapping("/ride/{rideId}/receipt/email")
    public ResponseEntity<?> emailReceipt(
            @PathVariable Long rideId,
            Principal principal) {

        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        try {
            Auth rider = getAuthenticatedUser(principal);

            if (rider.getRole() != Role.RIDER) {
                return error(HttpStatus.FORBIDDEN,
                        "Only riders can email receipts");
            }

            Map<String, Object> receipt =
                    paymentService.getReceipt(
                            rideId, rider.getId());

            byte[] pdfBytes =
                    receiptPdfService.generateReceiptPdf(receipt);

            Object receiptNumberValue =
                    receipt.get("receiptNumber");

            String receiptNumber =
                    receiptNumberValue == null
                            ? "RIDEGO-" + rideId
                            : receiptNumberValue.toString();

            // Sends the PDF to the authenticated rider's email.
            receiptEmailService.sendReceiptEmail(
                    rider.getEmail(),
                    receiptNumber,
                    pdfBytes);

            return ResponseEntity.ok(Map.of(
                    "message", "Receipt emailed successfully",
                    "rideId", rideId,
                    "email", rider.getEmail(),
                    "receiptNumber", receiptNumber));

        } catch (ResponseStatusException e) {
            return error(e.getStatusCode(), safeReason(e));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.NOT_FOUND, safeMessage(e));
        } catch (IllegalStateException e) {
            System.err.println(
                    "Receipt email/PDF error: " + e.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate or email the receipt");
        }
    }

    // =====================================================
    // 7. AUTHENTICATED USER HELPER
    // =====================================================

    private Auth getAuthenticatedUser(Principal principal) {

        String email = principal.getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        return authRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated user not found"));
    }

    // =====================================================
    // 8. STANDARD ERROR RESPONSE
    // =====================================================

    private ResponseEntity<Map<String, String>> error(
            HttpStatusCode status,
            String message) {

        return ResponseEntity.status(status)
                .body(Map.of(
                        "message",
                        message == null || message.isBlank()
                                ? "Request failed"
                                : message));
    }

    private String safeReason(ResponseStatusException exception) {
        return exception.getReason() == null
                ? "Request failed"
                : exception.getReason();
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null
                ? "Request failed"
                : exception.getMessage();
    }
}
