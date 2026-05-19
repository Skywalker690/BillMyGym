package com.gymflow.api;

import com.gymflow.api.dto.SubscriptionDtos.VerifySubscriptionPaymentRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.service.ApiRequestLogService;
import com.gymflow.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final String CREATE_ENDPOINT = "POST:/api/subscription/create";
    private static final String VERIFY_ENDPOINT = "POST:/api/subscription/verify";

    private final SubscriptionService subscriptionService;
    private final ApiRequestLogService apiRequestLogService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
        @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var stored = apiRequestLogService.findStoredResponse(
                idempotencyKey,
                CREATE_ENDPOINT,
                com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderResponse.class
            );
            if (stored.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Subscription order created", stored.get()));
            }
            var response = subscriptionService.createOrder();
            return ResponseEntity.ok(ApiResponse.success(
                "Subscription order created",
                apiRequestLogService.storeOrLoad(
                    idempotencyKey,
                    CREATE_ENDPOINT,
                    response,
                    com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderResponse.class
                )
            ));
        }
        return ResponseEntity.ok(ApiResponse.success("Subscription order created", subscriptionService.createOrder()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
        @Valid @RequestBody VerifySubscriptionPaymentRequest request,
        @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var stored = apiRequestLogService.findStoredResponse(
                idempotencyKey,
                VERIFY_ENDPOINT,
                com.gymflow.api.dto.SubscriptionDtos.SubscriptionStatusResponse.class
            );
            if (stored.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Subscription payment verified", stored.get()));
            }
            var response = subscriptionService.verifyPayment(request);
            return ResponseEntity.ok(ApiResponse.success(
                "Subscription payment verified",
                apiRequestLogService.storeOrLoad(
                    idempotencyKey,
                    VERIFY_ENDPOINT,
                    response,
                    com.gymflow.api.dto.SubscriptionDtos.SubscriptionStatusResponse.class
                )
            ));
        }
        return ResponseEntity.ok(ApiResponse.success("Subscription payment verified", subscriptionService.verifyPayment(request)));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
        @RequestBody String payload,
        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        subscriptionService.handleWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(ApiResponse.success("Subscription status fetched", subscriptionService.status()));
    }

    @GetMapping("/organizations/{organizationId}/status")
    public ResponseEntity<?> organizationStatus(@PathVariable Long organizationId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Organization subscription status fetched",
            subscriptionService.statusForOrganization(organizationId)
        ));
    }
}
