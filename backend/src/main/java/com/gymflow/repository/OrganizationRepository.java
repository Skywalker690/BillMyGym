package com.gymflow.repository;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByIdAndDeletedFalse(Long id);

    List<Organization> findAllByDeletedFalse();

    List<Organization> findAllByDeletedFalseAndStatus(OrganizationStatus status);

    boolean existsByNameAndDeletedFalse(String name);
}
