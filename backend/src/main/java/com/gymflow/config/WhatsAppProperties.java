package com.gymflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gymflow.whatsapp")
public class WhatsAppProperties {

    private boolean enabled;
    private String apiUrl = "https://graph.facebook.com";
    private String accessToken = "";
    private String phoneNumberId = "";
    private String businessAccountId = "";
    private String version = "v23.0";
    private String webhookVerifyToken = "";
    private String appSecret = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getBusinessAccountId() {
        return businessAccountId;
    }

    public void setBusinessAccountId(String businessAccountId) {
        this.businessAccountId = businessAccountId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getWebhookVerifyToken() {
        return webhookVerifyToken;
    }

    public void setWebhookVerifyToken(String webhookVerifyToken) {
        this.webhookVerifyToken = webhookVerifyToken;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean isReadyForCloudApi() {
        return enabled && hasText(accessToken) && hasText(phoneNumberId);
    }

    public boolean hasWebhookVerifyToken() {
        return hasText(webhookVerifyToken);
    }

    public boolean hasWebhookAppSecret() {
        return hasText(appSecret);
    }

    public String messagesEndpoint() {
        String baseUrl = stripTrailingSlash(hasText(apiUrl) ? apiUrl : "https://graph.facebook.com");
        String apiVersion = trimSlashes(hasText(version) ? version : "v23.0");
        return "%s/%s/%s/messages".formatted(baseUrl, apiVersion, phoneNumberId);
    }

    private String stripTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String trimSlashes(String value) {
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
