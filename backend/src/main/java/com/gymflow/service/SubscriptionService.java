package com.gymflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderRequest;
import com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderResponse;
import com.gymflow.api.dto.SubscriptionDtos.MemberLimitResponse;
import com.gymflow.api.dto.SubscriptionDtos.PaymentHistoryPageResponse;
import com.gymflow.api.dto.SubscriptionDtos.PlanAccessResponse;
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
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionPaymentRepository;
import com.gymflow.repository.OrganizationSubscriptionRepository;
import com.gymflow.repository.UsageMetricRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import com.gymflow.shared.money.MoneyUtils;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String CURRENCY = "INR";
    private static final int DEFAULT_PAYMENT_HISTORY_SIZE = 5;
    private static final int MAX_PAYMENT_HISTORY_SIZE = 50;

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UsageMetricRepository usageMetricRepository;
    private final MemberRepository memberRepository;
    private final PlanAccessService planAccessService;
    private final MemberLimitService memberLimitService;
    private final AuditLogService auditLogService;
    private final OrganizationPaymentWebhookService organizationPaymentWebhookService;
    private final BillingLifecycleService billingLifecycleService;
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
    public CreateSubscriptionOrderResponse createOrder(CreateSubscriptionOrderRequest request) {
        Organization organization = resolveCurrentOrganization();
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            throw new BadRequestException("Demo organizations have billing bypass enabled.");
        }
        if (billingLifecycleService.hasCurrentPaidAccess(organization, LocalDate.now())) {
            throw new BadRequestException("Subscription is already active through " + organization.getSubscriptionDueDate().minusDays(1));
        }
        SubscriptionPlan selectedPlan = resolveCheckoutPlan(request, organization);
        BigDecimal monthlyAmount = MoneyUtils.normalize(selectedPlan.monthlyAmount());
        LocalDate start = resolveNextSubscriptionStart(organization);
        LocalDate end = billingLifecycleService.billingPeriodEnd(start);
        OrganizationSubscriptionPayment existingPending = subscriptionPaymentRepository
            .findTopByOrganizationIdAndPaymentStatusInAndDeletedFalseOrderByCreatedAtDesc(
                organization.getId(),
                List.of(SubscriptionPaymentStatus.PENDING, SubscriptionPaymentStatus.FAILED)
            )
            .filter(payment -> payment.getSubscriptionStart().equals(start)
                && payment.getSubscriptionEnd().equals(end)
                && selectedPlan == resolvePaymentPlan(payment, organization)
                && payment.getAmount().compareTo(monthlyAmount) == 0)
            .orElse(null);
        if (existingPending != null) {
            return new CreateSubscriptionOrderResponse(
                existingPending.getRazorpayOrderId(),
                razorpayKeyId,
                paymentsEnabled,
                selectedPlan,
                existingPending.getAmount(),
                MoneyUtils.toMinorUnits(existingPending.getAmount()),
                CURRENCY,
                existingPending.getSubscriptionStart(),
                existingPending.getSubscriptionEnd(),
                existingPending.getPaymentStatus()
            );
        }
        long amountInPaise = MoneyUtils.toMinorUnits(monthlyAmount);
        String receipt = "gymflow_org_%d_%d".formatted(organization.getId(), System.currentTimeMillis());
        String razorpayOrderId = paymentsEnabled ? createRazorpayOrder(amountInPaise, receipt, selectedPlan) : "order_local_" + receipt;

        OrganizationSubscriptionPayment payment = new OrganizationSubscriptionPayment();
        payment.setOrganization(organization);
        payment.setAmount(monthlyAmount);
        payment.setSubscriptionPlan(selectedPlan);
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
            "Created " + selectedPlan + " subscription order " + razorpayOrderId + " for organization " + organization.getName(),
            AuditCategory.ORGANIZATION,
            "SUBSCRIPTION"
        );

        return new CreateSubscriptionOrderResponse(
            razorpayOrderId,
            razorpayKeyId,
            paymentsEnabled,
            selectedPlan,
            monthlyAmount,
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

    public PaymentHistoryPageResponse paymentHistory(int page, int size) {
        Organization organization = resolveCurrentOrganization();
        return loadPaymentHistory(organization.getId(), page, size);
    }

    public PaymentHistoryPageResponse paymentHistoryForOrganization(Long organizationId, int page, int size) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.role() != Role.SUPER_ADMIN && !organizationId.equals(user.organizationId())) {
            throw new ForbiddenException("Forbidden subscription access");
        }
        organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        return loadPaymentHistory(organizationId, page, size);
    }

    @Transactional(readOnly = true)
    public CreateSubscriptionOrderResponse createOrderResponse(Long paymentId) {
        Organization organization = resolveCurrentOrganization();
        OrganizationSubscriptionPayment payment = subscriptionPaymentRepository.findById(paymentId)
            .filter(existing -> existing.getOrganization().getId().equals(organization.getId()))
            .orElseThrow(() -> new NotFoundException("Subscription payment not found"));
        return new CreateSubscriptionOrderResponse(
            payment.getRazorpayOrderId(),
            razorpayKeyId,
            paymentsEnabled,
            resolvePaymentPlan(payment, organization),
            payment.getAmount(),
            MoneyUtils.toMinorUnits(payment.getAmount()),
            CURRENCY,
            payment.getSubscriptionStart(),
            payment.getSubscriptionEnd(),
            payment.getPaymentStatus()
        );
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

        PaymentHistoryPageResponse paymentHistoryPage = loadPaymentHistory(
            organization.getId(),
            0,
            DEFAULT_PAYMENT_HISTORY_SIZE
        );
        SubscriptionPlan resolvedPlan = planAccessService.resolvePlan(organization);
        BigDecimal monthlyAmount = organization.getOrganizationType() == OrganizationType.DEMO
            ? BigDecimal.ZERO
            : resolvedPlan.monthlyAmount();
        BigDecimal lifetimeSubscriptionRevenue = organization.getOrganizationType() == OrganizationType.DEMO
            ? BigDecimal.ZERO
            : subscriptionPaymentRepository.sumPaidForOrganization(organization.getId());

        return new SubscriptionStatusResponse(
            organization.getId(),
            organization.getName(),
            organization.getOrganizationType(),
            organization.getStatus(),
            resolvedPlan,
            monthlyAmount,
            lifetimeSubscriptionRevenue,
            organization.getCreatedAt() == null ? null : organization.getCreatedAt().toInstant(),
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
            toMemberLimitResponse(organization, memberLimitService.usage(organization)),
            toPlanAccessResponse(planAccessService.summary(organization)),
            paymentHistoryPage.items(),
            paymentHistoryPage
        );
    }

    private PaymentHistoryPageResponse loadPaymentHistory(Long organizationId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAYMENT_HISTORY_SIZE);
        Page<OrganizationSubscriptionPayment> payments = subscriptionPaymentRepository
            .findByOrganizationIdAndDeletedFalseOrderByCreatedAtDesc(
                organizationId,
                PageRequest.of(safePage, safeSize)
            );
        return new PaymentHistoryPageResponse(
            payments.stream().map(this::toPaymentResponse).toList(),
            payments.getNumber(),
            payments.getSize(),
            payments.getTotalElements(),
            payments.getTotalPages(),
            payments.hasNext()
        );
    }

    private MemberLimitResponse toMemberLimitResponse(Organization organization, MemberLimitService.MemberLimitSummary summary) {
        return new MemberLimitResponse(
            summary.plan(),
            summary.usedMembers(),
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(
                organization.getId(),
                com.gymflow.domain.enums.MemberStatus.DEACTIVATED
            ),
            summary.memberLimit(),
            summary.unlimited(),
            summary.remainingSlots(),
            summary.usagePercent()
        );
    }

    private PlanAccessResponse toPlanAccessResponse(PlanAccessService.PlanAccessSummary summary) {
        return new PlanAccessResponse(
            summary.canCreateMembers(),
            summary.canViewAdvancedAnalytics(),
            summary.canReceiveMonthlyReports(),
            summary.canExportReports(),
            summary.canUsePremiumFeatures()
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

    private SubscriptionPlan resolveCheckoutPlan(CreateSubscriptionOrderRequest request, Organization organization) {
        SubscriptionPlan selectedPlan = request == null || request.subscriptionPlan() == null
            ? planAccessService.resolvePlan(organization)
            : request.subscriptionPlan();
        if (selectedPlan == SubscriptionPlan.PILOT && organization.getOrganizationType() != OrganizationType.PILOT) {
            throw new BadRequestException("Pilot is an invitation-only evaluation plan and cannot be purchased from checkout");
        }
        return selectedPlan;
    }

    private SubscriptionPlan resolvePaymentPlan(OrganizationSubscriptionPayment payment, Organization organization) {
        return payment.getSubscriptionPlan() == null ? planAccessService.resolvePlan(organization) : payment.getSubscriptionPlan();
    }

    private LocalDate resolveNextSubscriptionStart(Organization organization) {
        return LocalDate.now();
    }

    private Organization resolveCurrentOrganization() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.organizationId() == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationRepository.findByIdAndDeletedFalse(user.organizationId())
            .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private String createRazorpayOrder(long amountInPaise, String receipt, SubscriptionPlan selectedPlan) {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new BadRequestException("Razorpay credentials are not configured");
        }
        try {
            Long organizationId = SecurityUtils.currentUser().organizationId();
            String body = objectMapper.writeValueAsString(new RazorpayOrderRequest(
                amountInPaise,
                CURRENCY,
                receipt,
                Map.of(
                    "organization_id", organizationId == null ? "" : organizationId.toString(),
                    "subscription_plan", selectedPlan.name()
                )
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
            return false;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of()
                .formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)))
                .getBytes(StandardCharsets.UTF_8);
            byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception ex) {
            return false;
        }
    }

    private record RazorpayOrderRequest(long amount, String currency, String receipt, Map<String, String> notes) {
    }
}
