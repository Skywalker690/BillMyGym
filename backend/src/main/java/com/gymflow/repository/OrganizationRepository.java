package com.gymflow.repository;

import com.gymflow.domain.Organization;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByIdAndDeletedFalse(Long id);

    List<Organization> findAllByDeletedFalse();

    boolean existsByNameAndDeletedFalse(String name);
}
