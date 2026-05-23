package com.gymflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.config.WhatsAppProperties;
import com.gymflow.domain.NotificationDeliveryLog;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.NotificationDeliveryStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.repository.NotificationDeliveryLogRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WhatsAppDeliveryService {

    private static final String PROVIDER = "WHATSAPP";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final List<Duration> RETRY_DELAYS = List.of(
        Duration.ofMinutes(5),
        Duration.ofMinutes(30),
        Duration.ofHours(1)
    );

    private final WhatsAppProperties properties;
    private final WhatsAppTemplateService templateService;
    private final WhatsAppUsageService usageService;
    private final NotificationDeliveryLogRepository deliveryLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WhatsAppSendResult sendTemplate(
        Organization organization,
        NotificationLog notification,
        String recipient,
        String templateName,
        Map<String, String> variables,
        NotificationType type
    ) {
        String normalizedRecipient = templateService.normalizeRecipient(recipient);
        String payload = templateService.buildTemplatePayload(normalizedRecipient, templateName, variables);
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setNotification(notification);
        log.setOrganization(organization);
        log.setType(type);
        log.setProvider(PROVIDER);
        log.setTemplateName(templateName);
        log.setRecipient(normalizedRecipient);
        log.setRequestPayload(payload);
        log.setStatus(NotificationDeliveryStatus.PENDING);
        log.setAttemptCount(0);
        deliveryLogRepository.save(log);
        return attemptDelivery(log);
    }

    @Transactional
    public WhatsAppSendResult sendText(
        Organization organization,
        NotificationLog notification,
        String recipient,
        String message,
        NotificationType type
    ) {
        String normalizedRecipient = templateService.normalizeRecipient(recipient);
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.setNotification(notification);
        log.setOrganization(organization);
        log.setType(type);
        log.setProvider(PROVIDER);
        log.setTemplateName("freeform_text");
        log.setRecipient(normalizedRecipient);
        log.setRequestPayload(buildTextPayload(normalizedRecipient, message));
        log.setStatus(NotificationDeliveryStatus.PENDING);
        log.setAttemptCount(0);
        deliveryLogRepository.save(log);
        return attemptDelivery(log);
    }

    @Transactional
    public WhatsAppSendResult sendOrganizationWelcome(Organization organization, String adminName, String dashboardLink) {
        if (!hasText(organization.getAdminPhoneNumber())) {
            return WhatsAppSendResult.skipped("Admin phone number is missing");
        }
        return sendTemplate(
            organization,
            null,
            organization.getAdminPhoneNumber(),
            WhatsAppTemplateService.TEMPLATE_ORGANIZATION_WELCOME,
            templateService.organizationWelcomeVariables(organization, adminName, dashboardLink),
            NotificationType.ORGANIZATION_WELCOME
        );
    }

    @Transactional
    public WhatsAppSendResult sendSubscriptionDue(
        Organization organization,
        BigDecimal amount,
        LocalDate dueDate,
        String paymentLink
    ) {
        if (!hasText(organization.getAdminPhoneNumber())) {
            return WhatsAppSendResult.skipped("Admin phone number is missing");
        }
        return sendTemplate(
            organization,
            null,
            organization.getAdminPhoneNumber(),
            WhatsAppTemplateService.TEMPLATE_SUBSCRIPTION_DUE,
            templateService.subscriptionDueVariables(organization, amount, dueDate, paymentLink),
            NotificationType.SUBSCRIPTION_REMINDER
        );
    }

    @Transactional
    public WhatsAppSendResult sendPaymentSuccess(Organization organization, String period) {
        if (!hasText(organization.getAdminPhoneNumber())) {
            return WhatsAppSendResult.skipped("Admin phone number is missing");
        }
        return sendTemplate(
            organization,
            null,
            organization.getAdminPhoneNumber(),
            WhatsAppTemplateService.TEMPLATE_PAYMENT_SUCCESS,
            templateService.paymentSuccessVariables(organization, period),
            NotificationType.SUBSCRIPTION_PAYMENT_SUCCESS
        );
    }

    @Transactional
    public WhatsAppSendResult sendMonthlyReport(
        Organization organization,
        MonthlyOrganizationReportService.MonthlyOrganizationReport report
    ) {
        if (!hasText(organization.getAdminPhoneNumber())) {
            return WhatsAppSendResult.skipped("Admin phone number is missing");
        }
        return sendTemplate(
            organization,
            null,
            organization.getAdminPhoneNumber(),
            WhatsAppTemplateService.TEMPLATE_MONTHLY_REPORT,
            templateService.monthlyReportVariables(organization, report),
            NotificationType.MONTHLY_ORGANIZATION_REPORT
        );
    }

    @Transactional
    public void retryDueDeliveries() {
        Instant now = Instant.now();
        deliveryLogRepository.findByProviderAndStatusInAndNextRetryAtLessThanEqualAndDeletedFalseOrderByNextRetryAtAsc(
            PROVIDER,
            List.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.RETRYING),
            now
        ).forEach(this::attemptDelivery);
    }

    private WhatsAppSendResult attemptDelivery(NotificationDeliveryLog log) {
        if (!hasText(log.getRecipient())) {
            return markFailed(log, "WhatsApp recipient is missing", false);
        }
        if (!hasText(log.getRequestPayload())) {
            return markFailed(log, "WhatsApp request payload is missing", false);
        }

        int nextAttempt = log.getAttemptCount() + 1;
        log.setAttemptCount(nextAttempt);
        log.setLastAttempt(Instant.now());
        log.setNextRetryAt(null);

        if (!properties.isEnabled()) {
            return markSent(log, "local_whatsapp_" + UUID.randomUUID());
        }
        if (!properties.isReadyForCloudApi()) {
            return markFailed(log, "WhatsApp Cloud API is enabled but access token or phone number id is missing", true);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.messagesEndpoint()))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + properties.getAccessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(log.getRequestPayload()))
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return markSent(log, extractProviderMessageId(response.body()));
            }
            return markFailed(log, extractErrorMessage(response.body(), response.statusCode()), true);
        } catch (Exception ex) {
            return markFailed(log, ex.getMessage(), true);
        }
    }

    private WhatsAppSendResult markSent(NotificationDeliveryLog log, String providerMessageId) {
        boolean alreadySent = log.getStatus() == NotificationDeliveryStatus.SENT;
        log.setStatus(NotificationDeliveryStatus.SENT);
        log.setProviderMessageId(providerMessageId);
        log.setErrorMessage(null);
        log.setNextRetryAt(null);
        deliveryLogRepository.save(log);
        if (!alreadySent) {
            usageService.recordSent(log.getOrganization());
        }
        return new WhatsAppSendResult(true, NotificationDeliveryStatus.SENT, log.getAttemptCount(), providerMessageId, null);
    }

    private WhatsAppSendResult markFailed(NotificationDeliveryLog log, String errorMessage, boolean retryable) {
        log.setErrorMessage(errorMessage);
        if (retryable && log.getAttemptCount() <= MAX_RETRY_ATTEMPTS) {
            log.setStatus(NotificationDeliveryStatus.RETRYING);
            log.setNextRetryAt(Instant.now().plus(RETRY_DELAYS.get(log.getAttemptCount() - 1)));
        } else {
            log.setStatus(NotificationDeliveryStatus.FAILED);
            log.setNextRetryAt(null);
        }
        deliveryLogRepository.save(log);
        return new WhatsAppSendResult(false, log.getStatus(), log.getAttemptCount(), log.getProviderMessageId(), errorMessage);
    }

    private String extractProviderMessageId(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String id = root.path("messages").path(0).path("id").asText("");
            return id.isBlank() ? null : id;
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractErrorMessage(String responseBody, int statusCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText("");
            if (!message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
        }
        return "WhatsApp Cloud API returned HTTP " + statusCode;
    }

    private String buildTextPayload(String recipient, String message) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "messaging_product", "whatsapp",
                "to", recipient,
                "type", "text",
                "text", Map.of(
                    "preview_url", false,
                    "body", message == null ? "" : message
                )
            ));
        } catch (Exception ex) {
            throw new com.gymflow.api.error.BadRequestException("Unable to build WhatsApp message payload");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record WhatsAppSendResult(
        boolean sent,
        NotificationDeliveryStatus status,
        int attemptCount,
        String providerMessageId,
        String errorMessage
    ) {
        static WhatsAppSendResult skipped(String reason) {
            return new WhatsAppSendResult(false, NotificationDeliveryStatus.FAILED, 0, null, reason);
        }
    }
}
