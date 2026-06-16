package com.gymflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderRequest;
import com.gymflow.api.dto.SubscriptionDtos.CreateSubscriptionOrderResponse;
import com.gymflow.api.dto.SubscriptionDtos.SubscriptionInvoiceResponse;
import com.gymflow.api.dto.SubscriptionDtos.MemberLimitResponse;
import com.gymflow.api.dto.SubscriptionDtos.PaymentHistoryPageResponse;
import com.gymflow.api.dto.SubscriptionDtos.PlanAccessResponse;
import com.gymflow.api.dto.SubscriptionDtos.SubscriptionPricingResponse;
import com.gymflow.api.dto.SubscriptionDtos.SubscriptionPaymentResponse;
import com.gymflow.api.dto.SubscriptionDtos.SubscriptionStatusResponse;
import com.gymflow.api.dto.SubscriptionDtos.VerifySubscriptionPaymentRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Organization;
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
import java.time.ZoneId;
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

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final String CURRENCY = "INR";
    private static final int DEFAULT_PAYMENT_HISTORY_SIZE = 5;
    private static final int MAX_PAYMENT_HISTORY_SIZE = 50;
    private static final SubscriptionPaymentStatus PAID_STATUS = SubscriptionPaymentStatus.PAID;

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UsageMetricRepository usageMetricRepository;
    private final MemberRepository memberRepository;
    private final PlanAccessService planAccessService;
    private final MemberLimitService memberLimitService;
    private final SubscriptionPricingService subscriptionPricingService;
    private final AuditLogService auditLogService;
    private final OrganizationPaymentWebhookService organizationPaymentWebhookService;
    private final BillingLifecycleService billingLifecycleService;
    private final OrganizationLifecycleService organizationLifecycleService;
    private final IndividualWhatsappReminderService individualWhatsappReminderService;
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
        if (organization.getStatus() == com.gymflow.domain.enums.OrganizationStatus.TRIAL
            && billingLifecycleService.hasCurrentAccessWindow(organization, LocalDate.now(APP_ZONE))) {
            throw new BadRequestException("Subscription payments are available after the free trial ends.");
        }
        if (billingLifecycleService.hasCurrentPaidAccess(organization, LocalDate.now(APP_ZONE))) {
            throw new BadRequestException("Subscription is already active through " + organization.getSubscriptionDueDate().minusDays(1));
        }
        SubscriptionPlan selectedPlan = resolveCheckoutPlan(request, organization);
        SubscriptionPricingService.SubscriptionPriceBreakdown pricing = subscriptionPricingService.priceFor(organization, selectedPlan);
        BigDecimal baseAmount = MoneyUtils.normalize(pricing.totalAmount());
        
        BigDecimal gymCoinsUsed = BigDecimal.ZERO;
        // GymCoins feature disabled for now
        // if (Boolean.TRUE.equals(request.useGymCoins()) && organization.getGymCoinBalance() != null) {
        //     gymCoinsUsed = organization.getGymCoinBalance().min(baseAmount);
        // }
        BigDecimal monthlyAmount = baseAmount.subtract(gymCoinsUsed);

        LocalDate start = resolveNextSubscriptionStart(organization);
        LocalDate end = billingLifecycleService.billingPeriodEnd(start);
        OrganizationSubscriptionPayment existingPending = subscriptionPaymentRepository
            .findTopByOrganizationIdAndPaymentStatusInAndDeletedFalseOrderByCreatedAtDesc(
                organization.getId(),
                List.of(SubscriptionPaymentStatus.PENDING)
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
                toPricingResponse(pricing),
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
        payment.setGymCoinsUsed(gymCoinsUsed);
        payment.setSubscriptionPlan(selectedPlan);
        applyInvoiceSnapshot(payment, pricing);
        payment.setSubscriptionStart(start);
        payment.setSubscriptionEnd(end);
        payment.setPaymentStatus(SubscriptionPaymentStatus.PENDING);
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setNextRetryDate(LocalDate.now(APP_ZONE).plusDays(1));
        subscriptionPaymentRepository.save(payment);

        if (!paymentsEnabled) {
            organizationPaymentWebhookService.markPaymentPaid(payment, LocalDate.now(APP_ZONE));
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
            toPricingResponse(pricing),
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
        organizationLifecycleService.normalize(organization, LocalDate.now(APP_ZONE));
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
        SubscriptionPlan paymentPlan = resolvePaymentPlan(payment, organization);
        return new CreateSubscriptionOrderResponse(
            payment.getRazorpayOrderId(),
            razorpayKeyId,
            paymentsEnabled,
            paymentPlan,
            payment.getAmount(),
            toPricingResponse(subscriptionPricingService.priceFor(organization, paymentPlan)),
            MoneyUtils.toMinorUnits(payment.getAmount()),
            CURRENCY,
            payment.getSubscriptionStart(),
            payment.getSubscriptionEnd(),
            payment.getPaymentStatus()
        );
    }

    private SubscriptionStatusResponse toStatusResponse(Organization organization) {
        YearMonth currentMonth = YearMonth.now(APP_ZONE);
        UsageMetric usage = usageMetricRepository.findByOrganizationIdAndYearAndMonthAndDeletedFalse(
            organization.getId(),
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        ).orElseGet(() -> UsageMetric.empty(organization, currentMonth));
        OrganizationSubscriptionPayment latestPaidPayment = subscriptionPaymentRepository
            .findTopByOrganizationIdAndPaymentStatusAndDeletedFalseOrderBySubscriptionEndDescCreatedAtDesc(
                organization.getId(),
                PAID_STATUS
            )
            .orElse(null);

        PaymentHistoryPageResponse paymentHistoryPage = loadPaymentHistory(
            organization.getId(),
            0,
            DEFAULT_PAYMENT_HISTORY_SIZE
        );
        SubscriptionPlan resolvedPlan = planAccessService.resolvePlan(organization);
        SubscriptionPricingService.SubscriptionPriceBreakdown pricing = subscriptionPricingService.priceFor(organization, resolvedPlan);
        BigDecimal monthlyAmount = organization.getOrganizationType() == OrganizationType.DEMO
            ? BigDecimal.ZERO
            : pricing.totalAmount();
        BigDecimal lifetimeSubscriptionRevenue = organization.getOrganizationType() == OrganizationType.DEMO
            ? BigDecimal.ZERO
            : subscriptionPaymentRepository.sumPaidForOrganization(organization.getId());
        int customReminderUsage = individualWhatsappReminderService.currentMonthCustomReminderUsage(organization);
        Integer customReminderLimit = individualWhatsappReminderService.monthlyCustomReminderLimit(organization);

        return new SubscriptionStatusResponse(
            organization.getId(),
            organization.getName(),
            organization.getOrganizationType(),
            organization.getStatus(),
            resolvedPlan,
            organization.getMonthlyMembershipFee(),
            organization.getAdmissionFee(),
            monthlyAmount,
            lifetimeSubscriptionRevenue,
            organization.getCreatedAt() == null ? null : organization.getCreatedAt().toInstant(),
            organization.getTrialEndDate(),
            organization.getSubscriptionDueDate(),
            latestPaidPayment == null ? null : latestPaidPayment.getSubscriptionStart(),
            latestPaidPayment == null ? null : latestPaidPayment.getSubscriptionEnd(),
            usage.getMonthlyWhatsappCount(),
            usage.getMonthlyEmailCount(),
            customReminderUsage,
            customReminderLimit,
            pricing.activeMembers(),
            toMemberLimitResponse(organization, memberLimitService.usage(organization)),
            toPricingResponse(pricing),
            toPlanAccessResponse(planAccessService.summary(organization)),
            paymentHistoryPage.items(),
            paymentHistoryPage,
            organization.getGymCoinBalance()
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
            summary.canUsePremiumFeatures(),
            summary.canUseSupportAutomation()
        );
    }

    private SubscriptionPricingResponse toPricingResponse(SubscriptionPricingService.SubscriptionPriceBreakdown pricing) {
        return new SubscriptionPricingResponse(
            pricing.baseAmount(),
            pricing.totalAmount(),
            pricing.activeMembers(),
            pricing.includedMembers(),
            pricing.overageMembers(),
            pricing.overageRate(),
            pricing.overageAmount()
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
            payment.getCreatedAt(),
            toInvoiceResponse(payment)
        );
    }

    private SubscriptionPlan resolveCheckoutPlan(CreateSubscriptionOrderRequest request, Organization organization) {
        if (request.subscriptionPlan() != null) {
            return request.subscriptionPlan();
        }
        return organization.getSubscriptionPlan() != null ? organization.getSubscriptionPlan() : SubscriptionPlan.BILLMYGYM;
    }

    private SubscriptionPlan resolvePaymentPlan(OrganizationSubscriptionPayment payment, Organization organization) {
        return payment.getSubscriptionPlan() == null ? SubscriptionPlan.BILLMYGYM : payment.getSubscriptionPlan();
    }

    private void applyInvoiceSnapshot(
        OrganizationSubscriptionPayment payment,
        SubscriptionPricingService.SubscriptionPriceBreakdown pricing
    ) {
        payment.setBaseAmount(MoneyUtils.normalize(pricing.baseAmount()));
        payment.setIncludedActiveMembers(pricing.includedMembers());
        payment.setBilledActiveMembers(pricing.activeMembers());
        payment.setAdditionalActiveMembers(pricing.overageMembers());
        payment.setOverageRate(MoneyUtils.normalize(pricing.overageRate()));
        payment.setOverageAmount(MoneyUtils.normalize(pricing.overageAmount()));
    }

    private SubscriptionInvoiceResponse toInvoiceResponse(OrganizationSubscriptionPayment payment) {
        SubscriptionPlan plan = payment.getSubscriptionPlan() != null ? payment.getSubscriptionPlan() : SubscriptionPlan.BILLMYGYM;
        BigDecimal baseAmount = payment.getBaseAmount() == null
            ? plan.monthlyAmount()
            : payment.getBaseAmount();
        Integer includedMembers = payment.getIncludedActiveMembers() == null
            ? (plan.memberLimit() == null ? Integer.MAX_VALUE : plan.memberLimit())
            : payment.getIncludedActiveMembers();
        BigDecimal overageRate = payment.getOverageRate() == null
            ? plan.overageRate()
            : payment.getOverageRate();
        BigDecimal overageAmount = payment.getOverageAmount() == null
            ? payment.getAmount().subtract(baseAmount).max(BigDecimal.ZERO)
            : payment.getOverageAmount();
        long additionalMembers = payment.getAdditionalActiveMembers() == null
            ? inferAdditionalMembers(overageAmount, overageRate)
            : payment.getAdditionalActiveMembers();
        long currentActiveMembers = payment.getBilledActiveMembers() == null
            ? includedMembers + additionalMembers
            : payment.getBilledActiveMembers();
        BigDecimal totalAmount = baseAmount.add(overageAmount);
        return new SubscriptionInvoiceResponse(
            baseAmount,
            includedMembers,
            currentActiveMembers,
            additionalMembers,
            overageRate,
            overageAmount,
            totalAmount
        );
    }

    private long inferAdditionalMembers(BigDecimal overageAmount, BigDecimal overageRate) {
        if (overageAmount == null || overageRate == null || overageRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }
        return overageAmount
            .divide(overageRate, 0, java.math.RoundingMode.CEILING)
            .longValue();
    }

    private LocalDate resolveNextSubscriptionStart(Organization organization) {
        return billingLifecycleService.nextChargeStartDate(organization, LocalDate.now(APP_ZONE));
    }

    private Organization resolveCurrentOrganization() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.organizationId() == null) {
            throw new BadRequestException("Organization context is required");
        }
        Organization organization = organizationRepository.findByIdAndDeletedFalse(user.organizationId())
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        organizationLifecycleService.normalize(organization, LocalDate.now(APP_ZONE));
        return organization;
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
