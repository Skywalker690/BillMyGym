package com.gymflow.repository;

import com.gymflow.domain.User;
import com.gymflow.domain.enums.Role;
import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    long countByOrganizationIdAndRoleAndDeletedFalse(Long organizationId, Role role);

    Optional<User> findByEmail(@NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email);
}
