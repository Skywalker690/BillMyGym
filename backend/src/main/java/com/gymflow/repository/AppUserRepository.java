package com.gymflow.repository;

import com.gymflow.domain.User;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "organization")
    Optional<User> findByEmailAndDeletedFalse(String email);

    long countByOrganizationIdAndRoleAndDeletedFalse(Long organizationId, Role role);

    List<User> findByOrganizationIdAndRoleAndDeletedFalse(Long organizationId, Role role);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalseAndIdNot(String email, Long id);

    boolean existsByOrganizationIdAndRoleAndStatusAndDeletedFalse(Long organizationId, Role role, UserStatus status);

    Optional<User> findByEmail(@NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email);
}
