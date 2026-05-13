package com.gymflow.repository;

import com.gymflow.domain.AppUser;
import com.gymflow.domain.enums.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailAndDeletedFalse(String email);

    long countByOrganizationIdAndRoleAndDeletedFalse(Long organizationId, Role role);
}
