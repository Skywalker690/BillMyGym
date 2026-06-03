package com.gymflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.domain.ApiRequestLog;
import com.gymflow.repository.ApiRequestLogRepository;
import com.gymflow.security.SecurityUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiRequestLogService {

    private final ApiRequestLogRepository apiRequestLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public <T> Optional<T> findStoredResponse(String requestId, String endpoint, Class<T> responseType) {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        return apiRequestLogRepository.findByOrganizationIdAndRequestIdAndEndpoint(organizationId, requestId, endpoint)
            .map(ApiRequestLog::getResponseBody)
            .map(body -> deserialize(body, responseType));
    }

    @Transactional
    public <T> T storeOrLoad(String requestId, String endpoint, T response, Class<T> responseType) {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        String responseBody = serialize(response);

        ApiRequestLog log = new ApiRequestLog();
        log.setOrganizationId(organizationId);
        log.setRequestId(requestId);
        log.setEndpoint(endpoint);
        log.setResponseBody(responseBody);
        log.setResponseHash(sha256(responseBody));
        try {
            apiRequestLogRepository.save(log);
            return response;
        } catch (DataIntegrityViolationException ex) {
            return apiRequestLogRepository.findByOrganizationIdAndRequestIdAndEndpoint(organizationId, requestId, endpoint)
                .map(ApiRequestLog::getResponseBody)
                .map(body -> deserialize(body, responseType))
                .orElse(response);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize idempotent response", ex);
        }
    }

    private <T> T deserialize(String body, Class<T> responseType) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deserialize idempotent response", ex);
        }
    }

    private String sha256(String body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash idempotent response", ex);
        }
    }
}
