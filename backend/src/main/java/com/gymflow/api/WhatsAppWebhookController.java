package com.gymflow.api;

import com.gymflow.api.response.ApiResponse;
import com.gymflow.config.WhatsAppProperties;
import com.gymflow.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService whatsAppWebhookService;
    private final WhatsAppProperties whatsAppProperties;

    @GetMapping
    public ResponseEntity<String> verify(
        @RequestParam(name = "hub.mode", required = false) String mode,
        @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
        @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if (!"subscribe".equals(mode)
            || !whatsAppProperties.hasWebhookVerifyToken()
            || !whatsAppProperties.getWebhookVerifyToken().equals(verifyToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("");
        }
        return ResponseEntity.ok(challenge == null ? "" : challenge);
    }

    @PostMapping
    public ResponseEntity<?> webhook(
        @RequestBody String payload,
        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature
    ) {
        whatsAppWebhookService.handleWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp webhook processed"));
    }
}
