package com.gymflow.repository;

import com.gymflow.domain.ApiRequestLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, Long> {

    Optional<ApiRequestLog> findByOrganizationIdAndRequestIdAndEndpoint(Long organizationId, String requestId, String endpoint);
}
