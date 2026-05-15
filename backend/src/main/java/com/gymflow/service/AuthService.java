package com.gymflow.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.gymflow.api.dto.AuthDtos.GoogleLoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginResponse;
import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
import com.gymflow.api.dto.UserDto;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.mapper.OrganizationMapper;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.User;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.OrganizationStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationMapper organizationMapper;
    private final AuditLogService auditLogService;

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
        if (appUserRepository.findByEmailAndDeletedFalse(request.adminEmail()).isPresent()) {
            throw new BadRequestException("Admin email already exists");
        }

        Organization organization = organizationMapper.toNewOrganization(request);
        organization.setStatus(OrganizationStatus.ACTIVE);
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
        appUserRepository.save(admin);

        auditLogService.log(
            "ORG_ADMIN_REGISTERED",
            "Registered organization " + savedOrganization.getName() + " with admin " + admin.getEmail(),
            AuditCategory.ORGANIZATION,
            "AUTH"
        );
    }

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
                appUserRepository.save(user);
            }

            return buildLoginResponse(user, "GOOGLE_LOGIN");

        } catch (GeneralSecurityException | IOException e) {
            log.debug("Full exception details:", e);
            throw new BadRequestException("Google authentication failed. Please try again.");
        }
    }

    private User resolveActiveUser(String email) {
        User user = appUserRepository.findByEmailAndDeletedFalse(email)
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

    private UserDto toUserDto(User user) {
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .role(user.getRole() == null ? null : user.getRole().name())
            .googleId(user.getGoogleId())
            .avatarUrl(user.getAvatarUrl())
            .createdAt(user.getCreatedAt() == null ? null : user.getCreatedAt().toLocalDateTime())
            .build();
    }
}
