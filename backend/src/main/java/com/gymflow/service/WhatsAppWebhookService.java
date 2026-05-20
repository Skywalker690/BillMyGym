package com.gymflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.config.WhatsAppProperties;
import com.gymflow.domain.NotificationDeliveryLog;
import com.gymflow.domain.enums.NotificationDeliveryStatus;
import com.gymflow.repository.NotificationDeliveryLogRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsAppWebhookService {

    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final ObjectMapper objectMapper;
    private final WhatsAppProperties whatsAppProperties;

    @Transactional
    public void handleWebhook(String payload, String signature) {
        validateSignature(payload, signature);
        try {
            JsonNode root = objectMapper.readTree(payload);
            for (JsonNode entry : root.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    JsonNode value = change.path("value");
                    for (JsonNode statusNode : value.path("statuses")) {
                        updateDeliveryStatus(statusNode);
                    }
                }
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to process WhatsApp webhook");
        }
    }

    private void validateSignature(String payload, String signature) {
        if (!whatsAppProperties.hasWebhookAppSecret()) {
            throw new BadRequestException("WhatsApp webhook app secret is not configured");
        }
        if (signature == null || !signature.startsWith("sha256=")) {
            throw new BadRequestException("Invalid WhatsApp webhook signature");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(whatsAppProperties.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = ("sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))))
                .getBytes(StandardCharsets.UTF_8);
            byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new BadRequestException("Invalid WhatsApp webhook signature");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid WhatsApp webhook signature");
        }
    }

    private void updateDeliveryStatus(JsonNode statusNode) {
        String messageId = statusNode.path("id").asText("");
        if (messageId.isBlank()) {
            return;
        }
        deliveryLogRepository.findTopByProviderMessageIdAndDeletedFalseOrderByCreatedAtDesc(messageId)
            .ifPresent(log -> applyProviderStatus(log, statusNode));
    }

    private void applyProviderStatus(NotificationDeliveryLog log, JsonNode statusNode) {
        String providerStatus = statusNode.path("status").asText("");
        log.setLastAttempt(Instant.now());
        if ("failed".equalsIgnoreCase(providerStatus)) {
            log.setStatus(NotificationDeliveryStatus.FAILED);
            log.setErrorMessage(extractFailure(statusNode));
            log.setNextRetryAt(null);
            return;
        }
        if ("sent".equalsIgnoreCase(providerStatus)
            || "delivered".equalsIgnoreCase(providerStatus)
            || "read".equalsIgnoreCase(providerStatus)) {
            log.setStatus(NotificationDeliveryStatus.SENT);
            log.setErrorMessage(null);
            log.setNextRetryAt(null);
        }
    }

    private String extractFailure(JsonNode statusNode) {
        JsonNode firstError = statusNode.path("errors").path(0);
        String message = firstError.path("message").asText("");
        if (!message.isBlank()) {
            return message;
        }
        String title = firstError.path("title").asText("");
        return title.isBlank() ? "WhatsApp delivery failed" : title;
    }
}
