package com.gymflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.api.dto.SubscriptionDtos.VerifySubscriptionPaymentRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSubscription;
import com.gymflow.domain.OrganizationSubscriptionPayment;
import com.gymflow.domain.ProcessedWebhookEvent;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import com.gymflow.domain.enums.SubscriptionStatus;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionPaymentRepository;
import com.gymflow.repository.OrganizationSubscriptionRepository;
import com.gymflow.repository.ProcessedWebhookEventRepository;
import com.gymflow.security.SecurityUtils;
import com.gymflow.shared.money.MoneyUtils;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationPaymentWebhookService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final OrganizationSubscriptionPaymentRepository paymentRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final UsageMetricService usageMetricService;
    private final AuditLogService auditLogService;
    private final BillingLifecycleService billingLifecycleService;
    private final PlanAccessService planAccessService;
    private final SubscriptionPricingService subscriptionPricingService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Transactional
    public void verifyClientPayment(VerifySubscriptionPaymentRequest request) {
        if (!SubscriptionService.verifySignature(
            request.razorpayOrderId() + "|" + request.razorpayPaymentId(),
            request.razorpaySignature(),
            razorpayKeySecret
        )) {
            throw new BadRequestException("Invalid Razorpay signature");
        }
        OrganizationSubscriptionPayment payment = paymentRepository.findByRazorpayOrderIdAndDeletedFalse(request.razorpayOrderId())
            .orElseThrow(() -> new NotFoundException("Subscription order not found"));
        Long currentOrganizationId = SecurityUtils.currentUser().organizationId();
        if (currentOrganizationId == null || !currentOrganizationId.equals(payment.getOrganization().getId())) {
            throw new ForbiddenException("Forbidden payment verification");
        }
        ensureUniqueGatewayTransaction(request.razorpayPaymentId(), payment);
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setGatewayTransactionId(request.razorpayPaymentId());
        markPaymentPaid(payment, LocalDate.now(APP_ZONE));
    }

    @Transactional
    public OrganizationSubscriptionPayment findPaymentByOrderId(String orderId) {
        return paymentRepository.findByRazorpayOrderIdAndDeletedFalse(orderId)
            .orElseThrow(() -> new NotFoundException("Subscription order not found"));
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            throw new BadRequestException("Razorpay webhook secret is not configured");
        }
        if (!SubscriptionService.verifySignature(payload, signature, razorpayWebhookSecret)) {
            throw new BadRequestException("Invalid Razorpay webhook signature");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText("");
            JsonNode paymentNode = root.path("payload").path("payment").path("entity");
            JsonNode subscriptionNode = root.path("payload").path("subscription").path("entity");
            JsonNode orderNode = root.path("payload").path("order").path("entity");
            String orderId = text(paymentNode, "order_id");
            String paymentId = text(paymentNode, "id");
            String subscriptionId = text(subscriptionNode, "id");
            String eventId = text(root, "id");
            if (eventId == null) {
                eventId = buildEventId(event, orderId, paymentId, subscriptionId);
            }
            if (eventId != null && processedWebhookEventRepository.findByEventId(eventId).isPresent()) {
                return;
            }

            OrganizationSubscriptionPayment payment = resolvePayment(root, orderId, paymentId, subscriptionId, eventId);
            if (payment == null) {
                return;
            }

            if (isSuccessEvent(event)) {
                validateGatewayAmount(payment, paymentNode, orderNode);
                payment.setWebhookEventId(eventId);
                if (paymentId != null && !paymentId.isBlank()) {
                    ensureUniqueGatewayTransaction(paymentId, payment);
                    payment.setRazorpayPaymentId(paymentId);
                    payment.setGatewayTransactionId(paymentId);
                }
                if (subscriptionId != null && !subscriptionId.isBlank()) {
                    payment.setRazorpaySubscriptionId(subscriptionId);
                }
                markPaymentPaid(payment, resolvePaymentDate(paymentNode, orderNode));
                recordProcessedEvent(eventId, event);
                return;
            }

            if (isFailureEvent(event)) {
                validateGatewayAmount(payment, paymentNode, orderNode);
                payment.setWebhookEventId(eventId);
                if (paymentId != null && !paymentId.isBlank()) {
                    ensureUniqueGatewayTransaction(paymentId, payment);
                    payment.setRazorpayPaymentId(paymentId);
                    payment.setGatewayTransactionId(paymentId);
                }
                if (subscriptionId != null && !subscriptionId.isBlank()) {
                    payment.setRazorpaySubscriptionId(subscriptionId);
                }
                payment.setPaymentStatus(SubscriptionPaymentStatus.FAILED);
                payment.setRetryCount(payment.getRetryCount() + 1);
                payment.setNextRetryDate(LocalDate.now(APP_ZONE).plusDays(Math.min(payment.getRetryCount(), 3) + 1L));
                payment.setFailureReason(resolveFailureReason(root));
                notifyPaymentFailure(payment);
                recordProcessedEvent(eventId, event);
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to process Razorpay webhook");
        }
    }

    @Transactional
    public void markPaymentPaid(OrganizationSubscriptionPayment payment, LocalDate paymentDate) {
        if (payment.getPaymentStatus() == SubscriptionPaymentStatus.PAID) {
            return;
        }
        LocalDate periodStart = paymentDate == null ? LocalDate.now(APP_ZONE) : paymentDate;
        LocalDate periodEnd = billingLifecycleService.billingPeriodEnd(periodStart);
        payment.setPaymentDate(periodStart);
        payment.setSubscriptionStart(periodStart);
        payment.setSubscriptionEnd(periodEnd);
        payment.setPaymentStatus(SubscriptionPaymentStatus.PAID);
        payment.setFailureReason(null);
        payment.setNextRetryDate(null);

        Organization organization = payment.getOrganization();
        if (payment.getSubscriptionPlan() != null) {
            organization.setSubscriptionPlan(payment.getSubscriptionPlan());
        }
        billingLifecycleService.activatePaidPeriod(organization, periodStart, periodEnd);

        OrganizationSubscription subscription = subscriptionRepository
            .findTopByOrganizationIdAndDeletedFalseOrderByEndDateDesc(organization.getId())
            .filter(existing -> existing.getStartDate().equals(periodStart)
                && existing.getEndDate().equals(periodEnd))
            .orElseGet(OrganizationSubscription::new);
        subscription.setOrganization(organization);
        subscription.setStartDate(periodStart);
        subscription.setEndDate(periodEnd);
        subscription.setAmount(payment.getAmount());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setRazorpaySubscriptionId(payment.getRazorpaySubscriptionId());
        subscriptionRepository.save(subscription);

        UsageMetric usage = usageMetricService.refreshMonthlySnapshot(organization);
        applicationEventPublisher.publishEvent(new com.gymflow.subscription.event.SubscriptionPaymentPaidEvent(
            organization,
            payment,
            usage,
            periodStart,
            periodEnd,
            billingSummary(usage)
        ));
        auditLogService.log(
            "SUBSCRIPTION_PAYMENT_PAID",
            "Subscription paid for organization " + organization.getName() + " through " + periodEnd,
            AuditCategory.ORGANIZATION,
            "SUBSCRIPTION"
        );
    }

    private OrganizationSubscriptionPayment resolvePayment(
        JsonNode root,
        String orderId,
        String paymentId,
        String subscriptionId,
        String eventId
    ) {
        if (orderId != null && !orderId.isBlank()) {
            return paymentRepository.findByRazorpayOrderIdAndDeletedFalse(orderId).orElse(null);
        }
        if (paymentId != null && !paymentId.isBlank()) {
            OrganizationSubscriptionPayment existing = paymentRepository.findByRazorpayPaymentIdAndDeletedFalse(paymentId).orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        if (subscriptionId != null && !subscriptionId.isBlank()) {
            OrganizationSubscriptionPayment existing = paymentRepository
                .findTopByRazorpaySubscriptionIdAndDeletedFalseOrderByCreatedAtDesc(subscriptionId)
                .orElse(null);
            if (existing != null && existing.getPaymentStatus() != SubscriptionPaymentStatus.PAID) {
                return existing;
            }
        }

        Long organizationId = resolveOrganizationId(root);
        if (organizationId == null) {
            return null;
        }
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId).orElse(null);
        if (organization == null) {
            return null;
        }
        SubscriptionPlan selectedPlan = resolveSubscriptionPlan(root);
        if (selectedPlan == null) {
            selectedPlan = planAccessService.resolvePlan(organization);
        }

        LocalDate today = LocalDate.now(APP_ZONE);
        OrganizationSubscriptionPayment payment = new OrganizationSubscriptionPayment();
        payment.setOrganization(organization);
        payment.setAmount(subscriptionPricingService.priceFor(organization, selectedPlan).totalAmount());
        payment.setSubscriptionPlan(selectedPlan);
        payment.setSubscriptionStart(today);
        payment.setSubscriptionEnd(billingLifecycleService.billingPeriodEnd(today));
        payment.setPaymentStatus(SubscriptionPaymentStatus.PENDING);
        payment.setWebhookEventId(eventId);
        payment.setRazorpayOrderId(orderId);
        payment.setRazorpayPaymentId(paymentId);
        payment.setRazorpaySubscriptionId(subscriptionId);
        if (paymentId != null && !paymentId.isBlank()) {
            payment.setGatewayTransactionId(paymentId);
        }
        return paymentRepository.save(payment);
    }

    private void ensureUniqueGatewayTransaction(String transactionId, OrganizationSubscriptionPayment payment) {
        paymentRepository.findByGatewayTransactionIdAndDeletedFalse(transactionId)
            .filter(existing -> !existing.getId().equals(payment.getId()))
            .ifPresent(existing -> {
                throw new BadRequestException("Gateway transaction has already been processed");
            });
    }

    private void validateGatewayAmount(OrganizationSubscriptionPayment payment, JsonNode paymentNode, JsonNode orderNode) {
        JsonNode amountSource = hasValue(paymentNode, "amount") ? paymentNode : orderNode;
        if (hasValue(amountSource, "amount")) {
            long expectedAmount = MoneyUtils.toMinorUnits(payment.getAmount());
            long gatewayAmount = amountSource.path("amount").asLong(-1L);
            if (gatewayAmount != expectedAmount) {
                throw new BadRequestException("Razorpay webhook amount does not match subscription order");
            }
        }
        String currency = text(amountSource, "currency");
        if (currency != null && !"INR".equalsIgnoreCase(currency)) {
            throw new BadRequestException("Unsupported Razorpay webhook currency");
        }
    }

    private boolean hasValue(JsonNode node, String fieldName) {
        return node != null
            && !node.isMissingNode()
            && !node.isNull()
            && node.has(fieldName)
            && !node.path(fieldName).isMissingNode()
            && !node.path(fieldName).isNull();
    }

    private boolean isSuccessEvent(String event) {
        return "payment.captured".equals(event) || "order.paid".equals(event) || "subscription.charged".equals(event);
    }

    private boolean isFailureEvent(String event) {
        return "payment.failed".equals(event) || "subscription.pending".equals(event) || "subscription.halted".equals(event);
    }

    private LocalDate resolvePaymentDate(JsonNode paymentNode, JsonNode orderNode) {
        long createdAt = paymentNode.path("created_at").asLong(0L);
        if (createdAt == 0L) {
            createdAt = orderNode.path("created_at").asLong(0L);
        }
        if (createdAt == 0L) {
            return LocalDate.now(APP_ZONE);
        }
        return Instant.ofEpochSecond(createdAt).atZone(java.time.ZoneOffset.UTC).toLocalDate();
    }

    private String resolveFailureReason(JsonNode root) {
        String paymentDescription = text(root.path("payload").path("payment").path("entity"), "error_description");
        if (paymentDescription != null) {
            return paymentDescription;
        }
        return text(root.path("payload").path("subscription").path("entity"), "status");
    }

    private Long resolveOrganizationId(JsonNode root) {
        String raw = text(root.path("payload").path("payment").path("entity").path("notes"), "organization_id");
        if (raw == null) {
            raw = text(root.path("payload").path("subscription").path("entity").path("notes"), "organization_id");
        }
        if (raw == null) {
            raw = text(root.path("payload").path("order").path("entity").path("notes"), "organization_id");
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private SubscriptionPlan resolveSubscriptionPlan(JsonNode root) {
        String raw = text(root.path("payload").path("payment").path("entity").path("notes"), "subscription_plan");
        if (raw == null) {
            raw = text(root.path("payload").path("order").path("entity").path("notes"), "subscription_plan");
        }
        if (raw == null) {
            raw = text(root.path("payload").path("subscription").path("entity").path("notes"), "subscription_plan");
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SubscriptionPlan.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void notifyPaymentFailure(OrganizationSubscriptionPayment payment) {
        UsageMetric usage = usageMetricService.refreshMonthlySnapshot(payment.getOrganization());
        applicationEventPublisher.publishEvent(new com.gymflow.subscription.event.SubscriptionPaymentFailedEvent(
            payment,
            usage,
            billingSummary(usage)
        ));
    }

    private String billingSummary(UsageMetric usage) {
        return "WhatsApp sent: %d, emails sent: %d, renewal digests: %d."
            .formatted(usage.getMonthlyWhatsappCount(), usage.getMonthlyEmailCount(), usage.getMonthlyDigestCount());
    }

    private String buildEventId(String event, String orderId, String paymentId, String subscriptionId) {
        if ((event == null || event.isBlank()) && orderId == null && paymentId == null && subscriptionId == null) {
            return null;
        }
        return "%s:%s:%s:%s".formatted(event, blankToDash(orderId), blankToDash(paymentId), blankToDash(subscriptionId));
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode child = node.path(fieldName);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        String value = child.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private void recordProcessedEvent(String eventId, String eventType) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        ProcessedWebhookEvent processedEvent = new ProcessedWebhookEvent();
        processedEvent.setEventId(eventId);
        processedEvent.setEventType(eventType == null || eventType.isBlank() ? "UNKNOWN" : eventType);
        processedEvent.setProcessedAt(Instant.now());
        processedWebhookEventRepository.save(processedEvent);
    }
}
