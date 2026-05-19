package com.gymflow.repository;

import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByIdAndDeletedFalse(Long id);

    List<Organization> findAllByDeletedFalse();

    List<Organization> findAllByDeletedFalseAndStatus(OrganizationStatus status);

    long countByDeletedFalseAndStatus(OrganizationStatus status);

    long countByDeletedFalseAndOrganizationType(OrganizationType organizationType);

    List<Organization> findAllByDeletedFalseAndStatusIn(List<OrganizationStatus> statuses);

    List<Organization> findAllByDeletedFalseAndOrganizationTypeAndPilotEndDateBefore(
        OrganizationType organizationType,
        LocalDate date
    );

    List<Organization> findAllByDeletedFalseAndOrganizationTypeAndSubscriptionDueDateIn(
        OrganizationType organizationType,
        List<LocalDate> dueDates
    );

    List<Organization> findAllByDeletedFalseAndOrganizationTypeAndStatusAndSubscriptionDueDateBefore(
        OrganizationType organizationType,
        OrganizationStatus status,
        LocalDate date
    );

    boolean existsByNameAndDeletedFalse(String name);

    boolean existsByAdminEmailAndDeletedFalse(String adminEmail);

    boolean existsByAdminEmailAndDeletedFalseAndIdNot(String adminEmail, Long id);

    boolean existsByAdminPhoneNumberAndDeletedFalse(String adminPhoneNumber);

    boolean existsByAdminPhoneNumberAndDeletedFalseAndIdNot(String adminPhoneNumber, Long id);
}
