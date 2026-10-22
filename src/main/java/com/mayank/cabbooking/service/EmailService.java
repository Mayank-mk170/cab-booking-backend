
package com.mayank.cabbooking.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(
            String recipientEmail,
            String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(recipientEmail);
        message.setSubject("Cab Booking - Reset Your Password");

        message.setText(
                "Hello,\n\n"
                        + "We received a request to reset your Cab Booking "
                        + "account password.\n\n"
                        + "Open the following link to reset your password:\n"
                        + resetLink
                        + "\n\nThis link expires in 15 minutes and can only "
                        + "be used once.\n\n"
                        + "If you did not request this change, you can "
                        + "ignore this email.\n\n"
                        + "Cab Booking Team"
        );

        mailSender.send(message);
    }
}
