package com.gymflow.service;

import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.dto.AuthDtos.LoginRequest;
import com.gymflow.api.dto.AuthDtos.LoginResponse;
import com.gymflow.api.dto.AuthDtos.RegisterOrganizationAdminRequest;
import com.gymflow.domain.User;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
import com.gymflow.repository.AppUserRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSettingRepository;
import com.gymflow.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtUtil jwtUtil;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public void registerOrganizationAdmin(RegisterOrganizationAdminRequest request) {
        if (organizationRepository.existsByNameAndDeletedFalse(request.organizationName())) {
            throw new BadRequestException("Organization already exists");
        }
        if (appUserRepository.findByEmailAndDeletedFalse(request.adminEmail()).isPresent()) {
            throw new BadRequestException("Admin email already exists");
        }

        Organization organization = new Organization();
        organization.setName(request.organizationName());
        OrganizationService.toOrgDto(organization, request.district(), request.logoUrl(), request.mapLocationLink(), request.address(), request.adminFullName(), request.adminPhoneNumber(), request.adminEmail(), request);
        organization.setDescription(request.organizationDescription());
        organization.setStatus(OrganizationStatus.ACTIVE);
        Organization savedOrg = organizationRepository.save(organization);

        OrganizationSetting setting = new OrganizationSetting();
        setting.setOrganization(savedOrg);
        settingRepository.save(setting);

        User admin = new User();
        admin.setOrganization(savedOrg);
        admin.setEmail(request.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ORG_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        appUserRepository.save(admin);
    }

    public LoginResponse login(LoginRequest request) {
        try {
            User user = appUserRepository.findByEmailAndDeletedFalse(request.email())
                    .orElseThrow(() -> new BadRequestException("Invalid credentials"));

            if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new BadRequestException("Invalid credentials");
            }

            String token = jwtUtil.generateToken(user.getEmail(), String.valueOf(user.getRole()));
            Long orgId = user.getOrganization() == null ? null : user.getOrganization().getId();
            return new LoginResponse(token, user.getRole().name(), orgId);

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }
}
