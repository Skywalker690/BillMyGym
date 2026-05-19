package com.gymflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderResponse;
import com.gymflow.api.dto.SubscriptionDtos.SubscriptionPaymentResponse;
import com.gymflow.api.dto.SubscriptionDtos.SubscriptionStatusResponse;
import com.gymflow.api.dto.SubscriptionDtos.VerifySubscriptionPaymentRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSubscription;
import com.gymflow.domain.OrganizationSubscriptionPayment;
import com.gymflow.domain.UsageMetric;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionPaymentRepository;
import com.gymflow.repository.OrganizationSubscriptionRepository;
import com.gymflow.repository.UsageMetricRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final BigDecimal MONTHLY_AMOUNT = new BigDecimal("299.00");
    private static final String CURRENCY = "INR";
    private static final int BILLING_DAYS = 30;

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UsageMetricRepository usageMetricRepository;
    private final MemberRepository memberRepository;
    private final AuditLogService auditLogService;
    private final OrganizationPaymentWebhookService organizationPaymentWebhookService;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Value("${gymflow.payments.enabled:false}")
    private boolean paymentsEnabled;

    @Transactional
    public CreateSubscriptionOrderResponse createOrder() {
        Organization organization = resolveCurrentOrganization();
        if (organization.getOrganizationType() == OrganizationType.PILOT) {
            throw new BadRequestException("Pilot organizations have billing bypass enabled until the pilot period ends");
        }
        LocalDate start = resolveNextSubscriptionStart(organization);
        LocalDate end = start.plusDays(BILLING_DAYS - 1L);
        OrganizationSubscriptionPayment existingPending = subscriptionPaymentRepository
            .findTopByOrganizationIdAndPaymentStatusInAndDeletedFalseOrderByCreatedAtDesc(
                organization.getId(),
                List.of(SubscriptionPaymentStatus.PENDING, SubscriptionPaymentStatus.FAILED)
            )
            .filter(payment -> payment.getSubscriptionStart().equals(start) && payment.getSubscriptionEnd().equals(end))
            .orElse(null);
        if (existingPending != null) {
            return new CreateSubscriptionOrderResponse(
                existingPending.getRazorpayOrderId(),
                razorpayKeyId,
                paymentsEnabled,
                existingPending.getAmount(),
                existingPending.getAmount().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact(),
                CURRENCY,
                existingPending.getSubscriptionStart(),
                existingPending.getSubscriptionEnd(),
                existingPending.getPaymentStatus()
            );
        }
        long amountInPaise = MONTHLY_AMOUNT.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String receipt = "gymflow_org_%d_%d".formatted(organization.getId(), System.currentTimeMillis());
        String razorpayOrderId = paymentsEnabled ? createRazorpayOrder(amountInPaise, receipt) : "order_local_" + receipt;

        OrganizationSubscriptionPayment payment = new OrganizationSubscriptionPayment();
        payment.setOrganization(organization);
        payment.setAmount(MONTHLY_AMOUNT);
        payment.setSubscriptionStart(start);
        payment.setSubscriptionEnd(end);
        payment.setPaymentStatus(SubscriptionPaymentStatus.PENDING);
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setNextRetryDate(LocalDate.now().plusDays(1));
        subscriptionPaymentRepository.save(payment);

        if (!paymentsEnabled) {
            organizationPaymentWebhookService.markPaymentPaid(payment, LocalDate.now());
        }

        auditLogService.log(
            "SUBSCRIPTION_ORDER_CREATED",
            "Created subscription order " + razorpayOrderId + " for organization " + organization.getName(),
            AuditCategory.ORGANIZATION,
            "SUBSCRIPTION"
        );

        return new CreateSubscriptionOrderResponse(
            razorpayOrderId,
            razorpayKeyId,
            paymentsEnabled,
            MONTHLY_AMOUNT,
            amountInPaise,
            CURRENCY,
            start,
            end,
            payment.getPaymentStatus()
        );
    }

    @Transactional
    public SubscriptionStatusResponse verifyPayment(VerifySubscriptionPaymentRequest request) {
        if (!paymentsEnabled) {
            return status();
        }
        organizationPaymentWebhookService.verifyClientPayment(request);
        return status();
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        organizationPaymentWebhookService.handleWebhook(payload, signature);
    }

    public SubscriptionStatusResponse status() {
        Organization organization = resolveCurrentOrganization();
        return toStatusResponse(organization);
    }

    public SubscriptionStatusResponse statusForOrganization(Long organizationId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN && !organizationId.equals(user.organizationId())) {
            throw new ForbiddenException("Forbidden subscription access");
        }
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        return toStatusResponse(organization);
    }

    private SubscriptionStatusResponse toStatusResponse(Organization organization) {
        YearMonth currentMonth = YearMonth.now();
        UsageMetric usage = usageMetricRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            organization.getId(),
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        ).orElseGet(() -> UsageMetric.empty(organization, currentMonth));
        OrganizationSubscription latestSubscription = subscriptionRepository
            .findTopByOrganizationIdAndDeletedFalseOrderByEndDateDesc(organization.getId())
            .orElse(null);

        List<SubscriptionPaymentResponse> payments = subscriptionPaymentRepository
            .findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(organization.getId())
            .stream()
            .map(this::toPaymentResponse)
            .toList();

        return new SubscriptionStatusResponse(
            organization.getId(),
            organization.getName(),
            organization.getOrganizationType(),
            organization.getStatus(),
            MONTHLY_AMOUNT,
            organization.getTrialEndDate(),
            organization.getPilotEndDate(),
            organization.getSubscriptionDueDate(),
            latestSubscription == null ? null : latestSubscription.getStartDate(),
            latestSubscription == null ? null : latestSubscription.getEndDate(),
            usage.getMonthlyWhatsappCount(),
            usage.getMonthlyEmailCount(),
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(
                organization.getId(),
                com.gymflow.domain.enums.MemberStatus.ACTIVE
            ),
            payments
        );
    }

    private SubscriptionPaymentResponse toPaymentResponse(OrganizationSubscriptionPayment payment) {
        return new SubscriptionPaymentResponse(
            payment.getId(),
            payment.getAmount(),
            payment.getPaymentDate(),
            payment.getSubscriptionStart(),
            payment.getSubscriptionEnd(),
            payment.getPaymentStatus(),
            payment.getGatewayTransactionId(),
            payment.getRazorpayOrderId(),
            payment.getRazorpayPaymentId(),
            payment.getCreatedAt()
        );
    }

    private LocalDate resolveNextSubscriptionStart(Organization organization) {
        LocalDate today = LocalDate.now();
        return subscriptionRepository.findTopByOrganizationIdAndDeletedFalseOrderByEndDateDesc(organization.getId())
            .map(subscription -> subscription.getEndDate().plusDays(1).isAfter(today)
                ? subscription.getEndDate().plusDays(1)
                : today)
            .orElse(today);
    }

    private Organization resolveCurrentOrganization() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.organizationId() == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationRepository.findByIdAndDeletedFalse(user.organizationId())
            .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private String createRazorpayOrder(long amountInPaise, String receipt) {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            return "order_local_" + receipt;
        }
        try {
            Long organizationId = SecurityUtils.currentUser().organizationId();
            String body = objectMapper.writeValueAsString(new RazorpayOrderRequest(
                amountInPaise,
                CURRENCY,
                receipt,
                Map.of("organization_id", organizationId == null ? "" : organizationId.toString())
            ));
            String credentials = Base64.getEncoder().encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/orders"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException("Unable to create Razorpay order");
            }
            return objectMapper.readTree(response.body()).path("id").asText();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to create Razorpay order");
        }
    }

    static boolean verifySignature(String payload, String signature, String secret) {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expected.equals(signature);
        } catch (Exception ex) {
            return false;
        }
    }

    private record RazorpayOrderRequest(long amount, String currency, String receipt, Map<String, String> notes) {
    }
}
