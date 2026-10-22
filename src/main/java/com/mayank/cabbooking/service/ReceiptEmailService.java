
package com.mayank.cabbooking.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class ReceiptEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public ReceiptEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReceiptEmail(
            String recipientEmail,
            String receiptNumber,
            byte[] pdfBytes) {

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Rider email address is missing");
        }

        if (receiptNumber == null || receiptNumber.isBlank()) {
            receiptNumber = "RIDEGO-RECEIPT";
        }

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException(
                    "Receipt PDF is empty");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject(
                    "RideGo Payment Receipt - " + receiptNumber);

            helper.setText(
                    "Hello,\n\n"
                            + "Thank you for riding with RideGo.\n"
                            + "Your payment receipt is attached as a PDF.\n\n"
                            + "Receipt Number: " + receiptNumber + "\n\n"
                            + "Regards,\n"
                            + "RideGo Team",
                    false);

            helper.addAttachment(
                    receiptNumber + ".pdf",
                    new ByteArrayResource(pdfBytes),
                    "application/pdf");

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new IllegalStateException(
                    "Unable to prepare the receipt email", e);
        }
    }
}
