package com.gymflow.api;

import com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderRequest;
import com.gymflow.api.dto.SubscriptionDtos.VerifySubscriptionPaymentRequest;
import com.gymflow.api.response.ApiResponse;
import com.gymflow.domain.enums.IdempotencyResourceType;
import com.gymflow.domain.enums.MembershipOperationType;
import com.gymflow.security.SecurityUtils;
import com.gymflow.service.OrganizationPaymentWebhookService;
import com.gymflow.service.SubscriptionService;
import com.gymflow.shared.idempotency.IdempotencyService;
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

    private final SubscriptionService subscriptionService;
    private final OrganizationPaymentWebhookService organizationPaymentWebhookService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
        @RequestBody(required = false) CreateSubscriptionOrderRequest request,
        @RequestHeader("X-Idempotency-Key") String idempotencyKey
    ) {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        String requiredKey = idempotencyService.requireKey(idempotencyKey);
        var existing = idempotencyService.findExistingForOrganization(
            organizationId,
            MembershipOperationType.SUBSCRIPTION_CREATE,
            requiredKey
        ).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(ApiResponse.success(
                "Subscription order created",
                subscriptionService.createOrderResponse(existing.getResourceId())
            ));
        }
        var response = subscriptionService.createOrder(request);
        var payment = organizationPaymentWebhookService.findPaymentByOrderId(response.razorpayOrderId());
        idempotencyService.recordForOrganization(
            organizationId,
            MembershipOperationType.SUBSCRIPTION_CREATE,
            requiredKey,
            IdempotencyResourceType.SUBSCRIPTION_PAYMENT,
            payment.getId()
        );
        return ResponseEntity.ok(ApiResponse.success("Subscription order created", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
        @Valid @RequestBody VerifySubscriptionPaymentRequest request,
        @RequestHeader("X-Idempotency-Key") String idempotencyKey
    ) {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        String requiredKey = idempotencyService.requireKey(idempotencyKey);
        var existing = idempotencyService.findExistingForOrganization(
            organizationId,
            MembershipOperationType.SUBSCRIPTION_VERIFY,
            requiredKey
        ).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(ApiResponse.success("Subscription payment verified", subscriptionService.status()));
        }
        var response = subscriptionService.verifyPayment(request);
        var payment = organizationPaymentWebhookService.findPaymentByOrderId(request.razorpayOrderId());
        idempotencyService.recordForOrganization(
            organizationId,
            MembershipOperationType.SUBSCRIPTION_VERIFY,
            requiredKey,
            IdempotencyResourceType.SUBSCRIPTION_PAYMENT,
            payment.getId()
        );
        return ResponseEntity.ok(ApiResponse.success("Subscription payment verified", response));
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
