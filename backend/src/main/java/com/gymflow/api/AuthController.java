package com.gymflow.api;

import com.gymflow.api.dto.AuthDtos;
import com.gymflow.api.dto.AuthDtos.GoogleLoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginRequest;
import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
import com.gymflow.api.dto.ChangePasswordRequest;
import com.gymflow.api.dto.ForgotPasswordRequest;
import com.gymflow.api.dto.UpdatePasswordOtpRequest;
import com.gymflow.api.dto.UpdatePasswordWithOtpRequest;
import com.gymflow.api.dto.VerifyOtpRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerOrgAdmin(@Valid @RequestBody RegisterOrganizationAdminRequest request) {
        authService.registerOrganizationAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Organization admin registered successfully and welcome email queued"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Google login successful", authService.loginWithGoogle(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(request));
    }

    @PostMapping("/password/request-otp")
    public ResponseEntity<?> requestPasswordUpdateOtp(@Valid @RequestBody UpdatePasswordOtpRequest request) {
        return ResponseEntity.ok(authService.requestPasswordUpdateOtp(SecurityUtils.currentUser().email(), request));
    }

    @PostMapping("/password/confirm")
    public ResponseEntity<?> confirmPasswordUpdate(@Valid @RequestBody UpdatePasswordWithOtpRequest request) {
        return ResponseEntity.ok(authService.confirmPasswordUpdate(SecurityUtils.currentUser().email(), request));
    }
}
