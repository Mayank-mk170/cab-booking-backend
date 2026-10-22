
package com.mayank.cabbooking.service;

import com.mayank.cabbooking.dto.AuthResponse;
import com.mayank.cabbooking.dto.LoginRequest;
import com.mayank.cabbooking.dto.LoginResponse;
import com.mayank.cabbooking.dto.RegisterRequest;
import com.mayank.cabbooking.dto.ResetPasswordRequest;

import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Role;
import com.mayank.cabbooking.entity.PasswordResetToken;

import com.mayank.cabbooking.repository.AuthRepository;
import com.mayank.cabbooking.repository.PasswordResetTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public AuthService(
            AuthRepository authRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService) {

        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    // REGISTER
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (authRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists");
        }

        String phone = request.getPhone();

        if (phone != null && !phone.isBlank()
                && authRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException(
                    "An account with this phone number already exists");
        }

        Role role = request.getRole();

        if (role == null || role == Role.ADMIN) {
            throw new IllegalArgumentException(
                    "A valid non-admin role is required");
        }

        Auth user = new Auth(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                phone,
                role
        );

        Auth savedUser = authRepository.save(user);

        return new AuthResponse(savedUser);
    }

    // LOGIN
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        Auth user = authRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException(
                    "Invalid email or password");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException(
                    "Your account is inactive");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(token, new AuthResponse(user));
    }

    // FORGOT PASSWORD
    @Transactional
    public void forgotPassword(String email) {

        String normalizedEmail = email.trim().toLowerCase();

        Auth user = authRepository.findByEmail(normalizedEmail)
                .orElse(null);

        // Do not reveal whether an account exists.
        if (user == null || !user.isActive()) {
            return;
        }

        // Invalidate previously issued reset tokens.
        passwordResetTokenRepository.deleteAllByUser_Id(user.getId());

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken(
                tokenHash,
                user,
                LocalDateTime.now().plusMinutes(15)
        );

        passwordResetTokenRepository.save(resetToken);

        String resetLink = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();

        // Send the token only through email, never in the API response.
        emailService.sendPasswordResetEmail(
                user.getEmail(),
                resetLink
        );
    }

    // RESET PASSWORD
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        String tokenHash = hashToken(request.getToken());

        PasswordResetToken resetToken =
                passwordResetTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Invalid or expired reset token"));

        if (resetToken.isUsed()
                || !resetToken.getExpiresAt()
                .isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Invalid or expired reset token");
        }

        Auth user = resetToken.getUser();

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is inactive");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        // Delete reset tokens after a successful password change.
        passwordResetTokenRepository.deleteAllByUser_Id(user.getId());

        authRepository.save(user);
    }

    // HASH RESET TOKEN
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Unable to process reset token", exception);
        }
    }
}
