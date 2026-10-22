
package com.mayank.cabbooking.controller;

import com.mayank.cabbooking.dto.ApiResponse;
import com.mayank.cabbooking.dto.AuthResponse;
import com.mayank.cabbooking.dto.LoginRequest;
import com.mayank.cabbooking.dto.LoginResponse;
import com.mayank.cabbooking.dto.RegisterRequest;
import com.mayank.cabbooking.dto.ResetPasswordRequest;

import com.mayank.cabbooking.service.AuthService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(
                true,
                "Registration successful",
                response
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(apiResponse);
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        ApiResponse<LoginResponse> apiResponse = new ApiResponse<>(
                true,
                "Login successful",
                response
        );

        return ResponseEntity.ok(apiResponse);
    }

    // FORGOT PASSWORD
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestParam
            @NotBlank
            @Email
            String email) {

        authService.forgotPassword(email);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "If an account exists for this email, a password reset link will be sent.",
                null
        );

        return ResponseEntity.ok(response);
    }

    // RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);

        ApiResponse<Void> response = new ApiResponse<>(
                true,
                "Password reset successful",
                null
        );

        return ResponseEntity.ok(response);
    }
}
