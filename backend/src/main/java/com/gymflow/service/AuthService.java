package com.gymflow.service;

import com.gymflow.api.dto.AuthDtos.GoogleLoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginResponse;
import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
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
import com.gymflow.security.GoogleTokenVerifierService;
import com.gymflow.security.GoogleTokenVerifierService.GoogleIdentity;
import com.gymflow.security.JwtService;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.audit.AuditLogService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationMapper organizationMapper;
    private final AuditLogService auditLogService;

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

    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdentity identity = googleTokenVerifierService.verify(request.idToken());
        User user = resolveActiveUser(identity.email());
        return buildLoginResponse(user, "GOOGLE_LOGIN");
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
        return new LoginResponse(token, user.getRole().name(), organizationId);
    }
}
