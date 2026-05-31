package com.gymflow.api;

import com.gymflow.api.dto.AuthDtos;
import com.gymflow.api.dto.AuthDtos.GoogleLoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginRequest;
import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
import com.gymflow.api.dto.AcceptPolicyRequest;
import com.gymflow.api.dto.ChangePasswordRequest;
import com.gymflow.api.dto.ForgotPasswordRequest;
import com.gymflow.api.dto.UpdatePasswordOtpRequest;
import com.gymflow.api.dto.UpdatePasswordWithOtpRequest;
import com.gymflow.api.dto.VerifyOtpRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping("/signup")
    public ResponseEntity<?> signupOrgAdmin(@Valid @RequestBody RegisterOrganizationAdminRequest request) {
        authService.publicRegisterOrganizationAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Signed up successfully and welcome email queued"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request, httpRequest)));
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Google login successful", authService.loginWithGoogle(request, httpRequest)));
    }

    @GetMapping("/legal-policy")
    public ResponseEntity<?> legalPolicyMetadata() {
        return ResponseEntity.ok(ApiResponse.success("Legal policy metadata fetched successfully", authService.legalPolicyMetadata()));
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

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser() {
        return ResponseEntity.ok(ApiResponse.success(
            "Current user fetched successfully",
            authService.getCurrentUser(SecurityUtils.currentUser().email())
        ));
    }

    @PostMapping("/consent")
    public ResponseEntity<?> acceptConsent(@Valid @RequestBody AcceptPolicyRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
            "Legal consent recorded successfully",
            authService.acceptPolicy(SecurityUtils.currentUser().email(), request, httpRequest)
        ));
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
