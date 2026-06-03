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
import com.gymflow.domain.enums.AuditEventStatus;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSettingRepository;
import com.gymflow.security.JwtService;
import com.gymflow.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

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
    private final ReferralService referralService;
    private final PlatformSettingService platformSettingService;
    private final LegalPolicyService legalPolicyService;

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

        LocalDate today = LocalDate.now(APP_ZONE);
        Organization organization = organizationMapper.toNewOrganization(request);
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            billingLifecycleService.startDemoAccess(organization, today);
        } else {
            billingLifecycleService.startRegularTrial(organization, today);
        }
        Organization savedOrganization = organizationRepository.save(organization);

        OrganizationSetting setting = new OrganizationSetting();
        setting.setOrganization(savedOrganization);
        setting.setBrandingName(savedOrganization.getName());
        settingRepository.save(setting);

        referralService.generateReferralCodeForOrganization(savedOrganization);
        referralService.processReferralSignup(savedOrganization, request.referralCodeUsed());

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
            savedOrganization.getTrialEndDate()
        );
        whatsAppDeliveryService.sendOrganizationWelcome(
            savedOrganization,
            request.adminFullName(),
            "https://gymmax.com/login"
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditLogService.log(
                    "ORG_ADMIN_REGISTERED",
                    "Registered organization " + savedOrganization.getName() + " with admin " + admin.getEmail(),
                    AuditCategory.ORGANIZATION,
                    "AUTH"
                );
            }
        });
    }

    @Transactional
    public void publicRegisterOrganizationAdmin(RegisterOrganizationAdminRequest request) {
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

        LocalDate today = LocalDate.now(APP_ZONE);
        Organization organization = organizationMapper.toNewOrganization(request);
        organization.setOrganizationType(OrganizationType.REGULAR); // Force REGULAR for public signups
        
        billingLifecycleService.startRegularTrial(organization, today);
        Organization savedOrganization = organizationRepository.save(organization);

        OrganizationSetting setting = new OrganizationSetting();
        setting.setOrganization(savedOrganization);
        setting.setBrandingName(savedOrganization.getName());
        settingRepository.save(setting);

        referralService.generateReferralCodeForOrganization(savedOrganization);
        
        if (request.referralCodeUsed() != null && !request.referralCodeUsed().trim().isEmpty()) {
            referralService.processReferralSignup(savedOrganization, request.referralCodeUsed());
        }

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
            savedOrganization.getTrialEndDate()
        );
        whatsAppDeliveryService.sendOrganizationWelcome(
            savedOrganization,
            request.adminFullName(),
            "https://gymmax.com/login"
        );

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                auditLogService.logActor(
                    "ORG_ADMIN_PUBLIC_REGISTERED",
                    "Publicly registered organization " + savedOrganization.getName() + " with admin " + admin.getEmail(),
                    AuditCategory.ORGANIZATION,
                    "AUTH",
                    admin.getEmail()
                );
            }
        });
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        AuditLogService.RequestAuditContext requestContext = requestAuditContext(httpRequest);
        User user = userRepository.findByEmailAndDeletedFalse(request.email()).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            if (user != null && user.getRole() == Role.ORG_ADMIN) {
                auditLogService.logLoginActivity(
                    "LOGIN_SUSPICIOUS",
                    "Suspicious login attempt on inactive account " + request.email(),
                    AuditEventStatus.SUSPICIOUS,
                    user,
                    requestContext
                );
            }
            throw new BadRequestException("Invalid credentials");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            if (user.getRole() == Role.ORG_ADMIN) {
                auditLogService.logLoginActivity(
                    "LOGIN_FAILED",
                    "Password validation failed for email " + request.email(),
                    AuditEventStatus.FAILED,
                    user,
                    requestContext
                );
            }
            throw new BadRequestException("Invalid credentials");
        }
        return buildLoginResponse(user, "PASSWORD_LOGIN", requestContext);
    }

    @Transactional
    public LoginResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        AuditLogService.RequestAuditContext requestContext = requestAuditContext(httpRequest);
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

            User user = resolveActiveUser(email, requestContext);

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

            return buildLoginResponse(user, "GOOGLE_LOGIN", requestContext);

        } catch (GeneralSecurityException | IOException e) {
            log.debug("Full exception details:", e);
            throw new BadRequestException("Google authentication failed. Please try again.");
        }
    }

    private User resolveActiveUser(String email, AuditLogService.RequestAuditContext requestContext) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
            .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            if (user.getRole() == Role.ORG_ADMIN) {
                auditLogService.logLoginActivity(
                    "LOGIN_SUSPICIOUS",
                    "Suspicious login attempt on inactive account " + email,
                    AuditEventStatus.SUSPICIOUS,
                    user,
                    requestContext
                );
            }
            throw new BadRequestException("User account is inactive");
        }
        return user;
    }

    private LoginResponse buildLoginResponse(
        User user,
        String auditEvent,
        AuditLogService.RequestAuditContext requestContext
    ) {
        String token = jwtService.issueToken(user);
        Long organizationId = user.getOrganization() == null ? null : user.getOrganization().getId();
        if (user.getRole() == Role.ORG_ADMIN) {
            auditLogService.logLoginActivity(
                auditEvent,
                "User " + user.getEmail() + " authenticated successfully",
                AuditEventStatus.SUCCESS,
                user,
                requestContext
            );
        } else {
            auditLogService.logActor(
                auditEvent,
                "User " + user.getEmail() + " authenticated successfully",
                AuditCategory.AUTHENTICATION,
                "AUTH",
                user.getEmail()
            );
        }
        boolean referralsEnabled = platformSettingService.getSettings().isReferralsEnabled();
        return new LoginResponse(token, user.getRole().name(), organizationId, toUserDto(user), referralsEnabled);
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
        User user = userRepository.findByEmailAndDeletedFalse(authenticatedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDto(user);
    }

    @Transactional
    public UserDto acceptPolicy(String authenticatedEmail, AcceptPolicyRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmailAndDeletedFalse(authenticatedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(request.getTermsAccepted()) || !Boolean.TRUE.equals(request.getPrivacyAccepted())) {
            throw new IllegalArgumentException("Terms & Conditions and Privacy Policy acceptance are required");
        }

        String version = request.getVersion().trim();
        if (!LegalPolicyService.CURRENT_POLICY_VERSION.equals(version)) {
            throw new BadRequestException("Please accept the latest legal policy version before continuing");
        }
        AuditLogService.RequestAuditContext requestContext = requestAuditContext(httpRequest);
        user.setTermsAccepted(true);
        user.setPrivacyAccepted(true);
        user.setPolicyVersion(version);
        user.setPolicyAcceptedAt(OffsetDateTime.now(APP_ZONE));
        user.setPolicyIpAddress(requestContext.ipAddress());
        user.setPolicyDeviceInfo(buildPolicyDeviceInfo(requestContext));
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
            .privacyAccepted(user.getPrivacyAccepted())
            .policyVersion(user.getPolicyVersion())
            .policyAcceptedAt(user.getPolicyAcceptedAt() == null ? null : user.getPolicyAcceptedAt().toInstant())
            .consentRequired(legalPolicyService.requiresConsent(user))
            .consentMessage(legalPolicyService.consentMessage(user))
            .build();
    }

    public AuthDtos.LegalPolicyMetadataResponse legalPolicyMetadata() {
        return legalPolicyService.metadata();
    }

    private AuditLogService.RequestAuditContext requestAuditContext(HttpServletRequest request) {
        if (request == null) {
            return new AuditLogService.RequestAuditContext(null, null, null);
        }
        String userAgent = request.getHeader("User-Agent");
        return new AuditLogService.RequestAuditContext(
            extractIpAddress(request),
            detectDevice(userAgent),
            detectBrowser(userAgent)
        );
    }

    private String extractIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] values = forwarded.split(",");
            return values.length == 0 ? request.getRemoteAddr() : values[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String detectDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String normalized = userAgent.toLowerCase();
        if (normalized.contains("ipad") || normalized.contains("tablet")) {
            return "Tablet";
        }
        if (normalized.contains("mobile") || normalized.contains("android") || normalized.contains("iphone")) {
            return "Mobile";
        }
        return "Desktop";
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        String normalized = userAgent.toLowerCase();
        if (normalized.contains("edg")) return "Edge";
        if (normalized.contains("opr") || normalized.contains("opera")) return "Opera";
        if (normalized.contains("chrome")) return "Chrome";
        if (normalized.contains("firefox")) return "Firefox";
        if (normalized.contains("safari")) return "Safari";
        return "Unknown";
    }

    private String buildPolicyDeviceInfo(AuditLogService.RequestAuditContext requestContext) {
        if (requestContext == null) {
            return null;
        }
        String device = requestContext.device() == null ? "Unknown device" : requestContext.device();
        String browser = requestContext.browser() == null ? "Unknown browser" : requestContext.browser();
        return device + " / " + browser;
    }
}

