package com.gymflow.api;

import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService whatsAppWebhookService;

    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name = "hub.challenge", required = false) String challenge) {
        return ResponseEntity.ok(challenge == null ? "" : challenge);
    }

    @PostMapping
    public ResponseEntity<?> webhook(@RequestBody String payload) {
        whatsAppWebhookService.handleWebhook(payload);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp webhook processed"));
    }
}
