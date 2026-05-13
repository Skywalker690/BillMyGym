package com.gymflow.service;

import com.gymflow.api.dto.AuthDtos.LoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginResponse;
import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
import com.gymflow.domain.AppUser;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSettingRepository;
import com.gymflow.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
        AppUserRepository appUserRepository,
        OrganizationRepository organizationRepository,
        OrganizationSettingRepository settingRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.organizationRepository = organizationRepository;
        this.settingRepository = settingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void registerOrganizationAdmin(RegisterOrganizationAdminRequest request) {
        if (organizationRepository.existsByNameAndDeletedFalse(request.organizationName())) {
            throw new IllegalArgumentException("Organization already exists");
        }
        if (appUserRepository.findByEmailAndDeletedFalse(request.adminEmail()).isPresent()) {
            throw new IllegalArgumentException("Admin email already exists");
        }

        Organization organization = new Organization();
        organization.setName(request.organizationName());
        organization.setDistrict(request.district());
        organization.setLogoUrl(request.logoUrl());
        organization.setMapLocationLink(request.mapLocationLink());
        organization.setAddress(request.address());
        organization.setAdminFullName(request.adminFullName());
        organization.setAdminPhoneNumber(request.adminPhoneNumber());
        organization.setAdminEmail(request.adminEmail());
        organization.setDescription(request.organizationDescription());
        organization.setStatus(OrganizationStatus.ACTIVE);
        Organization savedOrg = organizationRepository.save(organization);

        OrganizationSetting setting = new OrganizationSetting();
        setting.setOrganization(savedOrg);
        settingRepository.save(setting);

        AppUser admin = new AppUser();
        admin.setOrganization(savedOrg);
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ORG_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        appUserRepository.save(admin);
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailAndDeletedFalse(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.issueToken(user);
        Long orgId = user.getOrganization() == null ? null : user.getOrganization().getId();
        return new LoginResponse(token, user.getRole().name(), orgId);
    }
}
