package com.gymflow.api;

import com.gymflow.api.dto.AuthDtos;
import com.gymflow.api.dto.AuthDtos.GoogleLoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginRequest;
import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
import com.gymflow.api.response.ApiResponse;
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
            .body(ApiResponse.success("Organization admin registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/google-login")
    public ResponseEntity<AuthDtos.LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthDtos.LoginResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(response);
    }
}
