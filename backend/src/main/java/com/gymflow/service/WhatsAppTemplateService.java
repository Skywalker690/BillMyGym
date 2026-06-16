package com.gymflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Organization;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WhatsAppTemplateService {

    public static final String TEMPLATE_EXPIRY_DIGEST = "gym_expiry_digest_v1";
    public static final String TEMPLATE_ORGANIZATION_WELCOME = "organization_welcome_v1";
    public static final String TEMPLATE_SUBSCRIPTION_DUE = "subscription_due_v1";
    public static final String TEMPLATE_PAYMENT_SUCCESS = "payment_success_v1";
    public static final String TEMPLATE_MONTHLY_REPORT = "monthly_gym_report_v2";

    private static final String LANGUAGE_CODE = "en_US";

    private final ObjectMapper objectMapper;

    public String buildTemplatePayload(String recipient, String templateName, Map<String, String> variables) {
        List<String> orderedVariables = orderedVariables(templateName);
        List<Map<String, String>> parameters = orderedVariables.stream()
            .map(variable -> Map.of(
                "type", "text",
                "text", variables.getOrDefault(variable, "")
            ))
            .toList();

        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "to", normalizeRecipient(recipient),
            "type", "template",
            "template", Map.of(
                "name", templateName,
                "language", Map.of("code", LANGUAGE_CODE),
                "components", List.of(Map.of(
                    "type", "body",
                    "parameters", parameters
                ))
            )
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Unable to build WhatsApp template payload");
        }
    }

    public Map<String, String> expiryDigestVariables(
        Organization organization,
        OrganizationDigestBuilderService.DigestContent digestContent
    ) {
        return Map.of(
            "adminName", organization.getAdminFullName(),
            "organizationName", organization.getName(),
            "memberCount", String.valueOf(digestContent.memberCount()),
            "memberList", digestContent.memberList()
        );
    }

    public Map<String, String> organizationWelcomeVariables(
        Organization organization,
        String adminName,
        String dashboardLink
    ) {
        return Map.of(
            "adminName", adminName,
            "organizationName", organization.getName(),
            "dashboardLink", dashboardLink
        );
    }

    public Map<String, String> subscriptionDueVariables(
        Organization organization,
        BigDecimal amount,
        LocalDate dueDate,
        String paymentLink
    ) {
        return Map.of(
            "organizationName", organization.getName(),
            "amount", amount.toPlainString(),
            "dueDate", dueDate == null ? "" : dueDate.toString(),
            "paymentLink", paymentLink
        );
    }

    public Map<String, String> paymentSuccessVariables(Organization organization, String period) {
        return Map.of(
            "organizationName", organization.getName(),
            "period", period
        );
    }

    public Map<String, String> monthlyReportVariables(
        Organization organization,
        MonthlyOrganizationReportService.MonthlyOrganizationReport report
    ) {
        return Map.of(
            "reportMonth", report.label(),
            "newMembers", String.valueOf(report.membersJoined()),
            "activeMembers", String.valueOf(report.activeMembers()),
            "collectedRevenue", report.collectedRevenue().toPlainString()
        );
    }

    public String normalizeRecipient(String recipient) {
        if (recipient == null) {
            return "";
        }
        return recipient.replaceAll("[^0-9]", "");
    }

    private List<String> orderedVariables(String templateName) {
        return switch (templateName) {
            case TEMPLATE_EXPIRY_DIGEST -> List.of("adminName", "organizationName", "memberCount", "memberList");
            case TEMPLATE_ORGANIZATION_WELCOME -> List.of("adminName", "organizationName", "dashboardLink");
            case TEMPLATE_SUBSCRIPTION_DUE -> List.of("organizationName", "amount", "dueDate", "paymentLink");
            case TEMPLATE_PAYMENT_SUCCESS -> List.of("organizationName", "period");
            case TEMPLATE_MONTHLY_REPORT -> List.of(
                "reportMonth",
                "newMembers",
                "activeMembers",
                "collectedRevenue"
            );
            default -> throw new BadRequestException("Unsupported WhatsApp template: " + templateName);
        };
    }
}
