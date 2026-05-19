package com.gymflow.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.gymflow.api.dto.*;
import com.gymflow.api.dto.AuthDtos.GoogleLoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginResponse;
import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.mapper.OrganizationMapper;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.User;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSettingRepository;
import com.gymflow.security.JwtService;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationMapper organizationMapper;
    private final AuditLogService auditLogService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final BillingLifecycleService billingLifecycleService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;

    @Value("${google.client.id}")
    private String googleClientId;

    @Transactional
    public void registerOrganizationAdmin(RegisterOrganizationAdminRequest request) {
        if (SecurityUtils.currentUser().role() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only SUPER_ADMIN can register organization admins");
        }
        if (organizationRepository.existsByNameAndDeletedFalse(request.organizationName())) {
            throw new BadRequestException("Organization already exists");
        }
        if (userRepository.findByEmailAndDeletedFalse(request.adminEmail()).isPresent()
            || organizationRepository.existsByAdminEmailAndDeletedFalse(request.adminEmail())) {
            throw new BadRequestException("Admin email already exists");
        }
        if (organizationRepository.existsByAdminPhoneNumberAndDeletedFalse(request.adminPhoneNumber())) {
            throw new BadRequestException("Admin phone number already exists");
        }

        LocalDate today = LocalDate.now();
        Organization organization = organizationMapper.toNewOrganization(request);
        if (organization.getOrganizationType() == OrganizationType.PILOT) {
            billingLifecycleService.startPilotAccess(organization, today);
        } else {
            billingLifecycleService.startRegularTrial(organization, today);
        }
        Organization savedOrganization = organizationRepository.save(organization);

        OrganizationSetting setting = new OrganizationSetting();
        setting.setOrganization(savedOrganization);
        setting.setBrandingName(savedOrganization.getName());
        settingRepository.save(setting);

        User admin = new User();
        admin.setOrganization(savedOrganization);
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ORG_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        emailService.sendOrganizationAdminWelcomeEmail(
            savedOrganization,
            request.adminEmail(),
            request.adminFullName(),
            request.password(),
            savedOrganization.getOrganizationType() == OrganizationType.PILOT
                ? savedOrganization.getPilotEndDate()
                : savedOrganization.getTrialEndDate()
        );
        whatsAppDeliveryService.sendOrganizationWelcome(
            savedOrganization,
            request.adminFullName(),
            "https://gymmax.com/login"
        );

        auditLogService.log(
            "ORG_ADMIN_REGISTERED",
            "Registered organization " + savedOrganization.getName() + " with admin " + admin.getEmail(),
            AuditCategory.ORGANIZATION,
            "AUTH"
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = resolveActiveUser(request.email());
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditLogService.logActor(
                "LOGIN_FAILED",
                "Password validation failed for email " + request.email(),
                AuditCategory.AUTHENTICATION,
                "AUTH",
                request.email()
            );
            throw new BadRequestException("Invalid credentials");
        }
        return buildLoginResponse(user, "PASSWORD_LOGIN");
    }

    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        try {
            if (googleClientId == null || googleClientId.trim().isEmpty()) {
                throw new BadRequestException("Google authentication is not configured");
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.idToken());
            if (idToken == null) {
                throw new BadRequestException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String pictureUrl = (String) payload.get("picture");

            User user = resolveActiveUser(email);

            boolean needsUpdate = false;
            if (user.getGoogleId() == null || !user.getGoogleId().equals(googleId)) {
                user.setGoogleId(googleId);
                needsUpdate = true;
            }
            if (pictureUrl != null && (user.getAvatarUrl() == null || !user.getAvatarUrl().equals(pictureUrl))) {
                user.setAvatarUrl(pictureUrl);
                needsUpdate = true;
            }
            if (needsUpdate) {
                userRepository.save(user);
            }

            return buildLoginResponse(user, "GOOGLE_LOGIN");

        } catch (GeneralSecurityException | IOException e) {
            log.debug("Full exception details:", e);
            throw new BadRequestException("Google authentication failed. Please try again.");
        }
    }

    private User resolveActiveUser(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
            .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User account is inactive");
        }
        return user;
    }

    private LoginResponse buildLoginResponse(User user, String auditEvent) {
        String token = jwtService.issueToken(user);
        Long organizationId = user.getOrganization() == null ? null : user.getOrganization().getId();
        auditLogService.logActor(
            auditEvent,
            "User " + user.getEmail() + " authenticated successfully",
            AuditCategory.AUTHENTICATION,
            "AUTH",
            user.getEmail()
        );
        return new LoginResponse(token, user.getRole().name(), organizationId, toUserDto(user));
    }

    public ApiResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailAndDeletedFalse(request.getEmail())
            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
            .ifPresent(user -> {
                otpService.generateAndSendOtp(user.getEmail());
                runAuditSafely(
                    () -> auditLogService.logActor(
                        "PASSWORD_RESET_OTP_REQUESTED",
                        "Password reset OTP requested",
                        AuditCategory.SECURITY,
                        "AUTH",
                        user.getEmail()
                    ),
                    "forgot-password"
                );
            });

        return ApiResponse.success("If an active account exists for that email, a reset code has been sent.");
    }

    public ApiResponse verifyOtp(VerifyOtpRequest request) {
        boolean isValid = otpService.isOtpValid(request.getEmail(), request.getOtp());

        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        return ApiResponse.success("OTP verified successfully");
    }

    @Transactional
    public ApiResponse changePassword(ChangePasswordRequest request) {
        boolean isValid = otpService.consumeOtp(request.getEmail(), request.getOtp());

        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        runAuditSafely(
            () -> auditLogService.logActor(
                "PASSWORD_RESET_COMPLETED",
                "Password reset completed successfully",
                AuditCategory.SECURITY,
                "AUTH",
                user.getEmail()
            ),
            "change-password"
        );

        return ApiResponse.success("Password changed successfully");
    }

    public ApiResponse requestPasswordUpdateOtp(String authenticatedEmail, UpdatePasswordOtpRequest request) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        otpService.generateAndSendOtp(authenticatedEmail);

        return ApiResponse.builder()
                .message("OTP sent to your email. Please verify to update password.")
                .build();
    }

    public UserDto getCurrentUser(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDto(user);
    }

    @Transactional
    public UserDto acceptPolicy(String authenticatedEmail, AcceptPolicyRequest request) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(request.getAccepted())) {
            throw new IllegalArgumentException("Policy acceptance is required");
        }

        String version = request.getVersion().trim();
        user.setTermsAccepted(true);
        user.setPolicyVersion(version);
        User savedUser = userRepository.save(user);

        return toUserDto(savedUser);
    }

    @Transactional
    public ApiResponse confirmPasswordUpdate(String authenticatedEmail, UpdatePasswordWithOtpRequest request) {
        boolean isValid = otpService.consumeOtp(authenticatedEmail, request.getOtp());
        if (!isValid) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.builder()
                .message("Password updated successfully")
                .build();
    }


    private void runAuditSafely(Runnable action, String context) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("Audit log failed during {}: {}", context, ex.getMessage());
        }
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .role(user.getRole() == null ? null : user.getRole().name())
            .googleId(user.getGoogleId())
            .avatarUrl(user.getAvatarUrl())
            .createdAt(user.getCreatedAt() == null ? null : user.getCreatedAt().toInstant())
            .termsAccepted(user.getTermsAccepted())
            .policyVersion(user.getPolicyVersion())
            .build();
    }
}

