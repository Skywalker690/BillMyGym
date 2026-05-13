package com.gymflow.bootstrap;

import com.gymflow.domain.AppUser;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
import com.gymflow.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminBootstrap implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${gymflow.super-admin.email:admin@gymflow.local}")
    private String superAdminEmail;

    @Value("${gymflow.super-admin.password:SuperAdmin@123}")
    private String superAdminPassword;

    public SuperAdminBootstrap(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.findByEmailAndDeletedFalse(superAdminEmail).isPresent()) {
            return;
        }

        AppUser superAdmin = new AppUser();
        superAdmin.setEmail(superAdminEmail);
        superAdmin.setPasswordHash(passwordEncoder.encode(superAdminPassword));
        superAdmin.setRole(Role.SUPER_ADMIN);
        superAdmin.setStatus(UserStatus.ACTIVE);
        appUserRepository.save(superAdmin);
    }
}
