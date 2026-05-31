package com.gymflow.service;

import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSubscriptionPayment;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.UserStatus;
import com.gymflow.repository.AppUserRepository;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String PLAN_NAME = "BillMyGym Plan";
    private static final String SENDER_NAME = "BillMyGym";
    private static final String APP_URL = "https://gymmax.com";
    private static final String FONT_STACK = "Arial,Helvetica,sans-serif";
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final JavaMailSender mailSender;
    private final UsageMetricService usageMetricService;
    private final AppUserRepository appUserRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOrganizationAdminWelcomeEmail(
        Organization organization,
        String toEmail,
        String adminName,
        String temporaryPassword,
        LocalDate accessEndsAt
    ) {
        sendHtmlEmail(
            toEmail,
            "Welcome to BillMyGym - Your organization is ready",
            buildOrganizationAdminWelcomeEmail(
                organization,
                adminName,
                toEmail,
                temporaryPassword,
                accessEndsAt
            ),
            true,
            organization
        );
    }

    @Async
    public void sendUserWelcomeEmail(String toEmail, String userName, String role, String zone) {
        sendHtmlEmail(
            toEmail,
            "Welcome to BillMyGym",
            buildGenericUserWelcomeEmail(toEmail, userName, role, zone),
            true
        );
    }

    @Async
    public void sendUserAccessRevokedEmail(String toEmail) {
        sendHtmlEmail(
            toEmail,
            "BillMyGym access update",
            buildUserAccessRevokedEmail(),
            true
        );
    }

    public void sendOtpEmail(String toEmail, String otp) {
        sendHtmlEmail(
            toEmail,
            "Your BillMyGym password reset code",
            buildOtpEmail(otp),
            false
        );
    }

    @Async
    public void sendSubscriptionReminderEmail(
        Organization organization,
        String toEmail,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        sendHtmlEmail(
            toEmail,
            "BillMyGym subscription reminder - " + organization.getName(),
            buildSubscriptionReminderEmail(
                organization,
                amountDue,
                dueDate,
                paymentLink,
                whatsappUsage,
                emailUsage,
                activeMembers,
                lastBillingSummary
            ),
            true,
            organization
        );
    }

    @Async
    public void sendTrialEndingEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        sendHtmlEmail(
            organization.getAdminEmail(),
            "BillMyGym free trial access ending - " + organization.getName(),
            buildTrialEndingEmail(organization, amountDue, dueDate, paymentLink, whatsappUsage, emailUsage, activeMembers, lastBillingSummary),
            true,
            organization
        );
    }

    @Async
    public void sendPaymentSuccessEmail(
        Organization organization,
        BigDecimal amountPaid,
        LocalDate paidOn,
        LocalDate accessThrough,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        sendHtmlEmail(
            organization.getAdminEmail(),
            "BillMyGym payment successful - " + organization.getName(),
            buildPaymentSuccessEmail(
                organization,
                amountPaid,
                paidOn,
                accessThrough,
                whatsappUsage,
                emailUsage,
                activeMembers,
                lastBillingSummary
            ),
            true,
            organization
        );
    }

    @Async
    public void sendPaymentFailedEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        String failureReason,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        sendHtmlEmail(
            organization.getAdminEmail(),
            "BillMyGym payment failed - " + organization.getName(),
            buildPaymentFailedEmail(
                organization,
                amountDue,
                dueDate,
                paymentLink,
                failureReason,
                whatsappUsage,
                emailUsage,
                activeMembers,
                lastBillingSummary
            ),
            true,
            organization
        );
    }

    @Async
    public void sendSuperAdminSubscriptionPaymentEmail(OrganizationSubscriptionPayment payment) {
        appUserRepository.findByRoleAndStatusAndDeletedFalse(Role.SUPER_ADMIN, UserStatus.ACTIVE)
            .forEach(superAdmin -> sendHtmlEmail(
                superAdmin.getEmail(),
                "BillMyGym subscription " + payment.getPaymentStatus().name().toLowerCase(Locale.ENGLISH)
                    + " - " + payment.getOrganization().getName(),
                buildSuperAdminSubscriptionPaymentEmail(payment),
                true
            ));
    }

    @Async
    public void sendPausedNotificationEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        sendHtmlEmail(
            organization.getAdminEmail(),
            "BillMyGym subscription renewal required - " + organization.getName(),
            buildPausedNotificationEmail(
                organization,
                amountDue,
                dueDate,
                paymentLink,
                whatsappUsage,
                emailUsage,
                activeMembers,
                lastBillingSummary
            ),
            true,
            organization
        );
    }

    @Async
    public void sendDeactivatedNotificationEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        sendHtmlEmail(
            organization.getAdminEmail(),
            "BillMyGym account inactive - " + organization.getName(),
            buildDeactivatedNotificationEmail(
                organization,
                amountDue,
                dueDate,
                paymentLink,
                whatsappUsage,
                emailUsage,
                activeMembers,
                lastBillingSummary
            ),
            true,
            organization
        );
    }

    @Async
    public void sendMonthlyOrganizationReport(
        Organization organization,
        MonthlyOrganizationReportService.MonthlyOrganizationReport report
    ) {
        sendHtmlEmail(
            organization.getAdminEmail(),
            "Your Monthly GymFlow Report",
            buildMonthlyOrganizationReportEmail(organization, report),
            true,
            organization
        );
    }

    private void sendHtmlEmail(String toEmail, String subject, String html, boolean swallowFailure, Organization organization) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, SENDER_NAME);
            helper.setReplyTo(fromEmail, SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(toPlainText(html), html);
            message.setSentDate(new Date());
            message.setHeader("X-Auto-Response-Suppress", "All");
            mailSender.send(message);
            if (organization != null) {
                usageMetricService.recordEmailSent(organization);
            }
            log.info("Email '{}' sent to {}", subject, toEmail);
        } catch (Exception ex) {
            log.error("Failed to send email '{}' to {}", subject, toEmail, ex);
            if (!swallowFailure) {
                throw new RuntimeException("Failed to send email", ex);
            }
        }
    }

    private void sendHtmlEmail(String toEmail, String subject, String html, boolean swallowFailure) {
        sendHtmlEmail(toEmail, subject, html, swallowFailure, null);
    }

    private String buildOrganizationAdminWelcomeEmail(
        Organization organization,
        String adminName,
        String toEmail,
        String temporaryPassword,
        LocalDate accessEndsAt
    ) {
        String accessEndLabel = organization.getOrganizationType() == OrganizationType.DEMO
            ? "Demo Workspace"
            : "Free Trial Access";
        String accessType = organization.getOrganizationType() == OrganizationType.DEMO
            ? "Demo Workspace"
            : "Regular Workspace";
        String accessWindow = organization.getOrganizationType() == OrganizationType.DEMO
            ? "Unlimited showcase access"
            : "Ends: " + (accessEndsAt == null ? "-" : accessEndsAt.toString());
        String content = """
            <p style="margin:0 0 16px 0;">Hello <strong>%s</strong>,</p>
            <p style="margin:0 0 16px 0;">Your organization workspace on <strong>BillMyGym</strong> is now ready. You can sign in and start managing memberships, payments, reminders, and reports from one place.</p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;border:1px solid #d7e3dc;border-radius:16px;overflow:hidden;background:#f8fcfa;margin:24px 0;">
              <tr>
                <td colspan="2" style="padding:14px 18px;background:#e7f6ee;color:#166534;font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;">Organization Access</td>
              </tr>
              %s
              %s
              %s
              %s
              %s
              %s
              %s
              %s
            </table>
            <div style="background:#fff7ed;border:1px solid #fed7aa;border-radius:16px;padding:18px 20px;margin:0 0 22px 0;">
              <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#c2410c;margin-bottom:10px;">Important First Step</div>
              <p style="margin:0;color:#9a3412;line-height:1.7;">Please sign in using the temporary password below and update it immediately after your first login. Keep this email secure and do not share your credentials.</p>
            </div>
            <p style="margin:0 0 12px 0;">Once you sign in, you can start onboarding members, recording payments, reviewing reports, and managing renewal reminders.</p>
            <p style="margin:0;">If you need help, contact the BillMyGym support team and include your organization name so we can assist faster.</p>
            """.formatted(
            html(adminName),
            detailRow("Organization", organization.getName()),
            detailRow("District", organization.getDistrict()),
            detailRow("Access Type", accessType),
            detailRow("Lifecycle Status", organizationStatusLabel(organization)),
            detailRow(accessEndLabel, accessWindow),
            detailRow("Admin Email", toEmail),
            detailRow("Temporary Password", temporaryPassword),
            detailRow("Sign-in URL", "<a href=\"" + html(appUrl("/login")) + "\" style=\"color:#166534;font-weight:700;text-decoration:none;\">"
                + html(appUrl("/login")) + "</a>")
        );

        return buildShell(
            "Organization ready",
            "Welcome to BillMyGym",
            "Your admin account has been created successfully.",
            content,
            "Open BillMyGym",
            "/login",
            "This email was sent because a BillMyGym super admin created your organization workspace."
        );
    }

    private String buildGenericUserWelcomeEmail(String toEmail, String userName, String role, String zone) {
        StringBuilder details = new StringBuilder()
            .append(detailRow("Email", html(toEmail)))
            .append(detailRow("Role", html(formatRole(role))));

        String content = """
            <p style="margin:0 0 16px 0;">Hello <strong>%s</strong>,</p>
            <p style="margin:0 0 16px 0;">Your BillMyGym access is active. You can now sign in and continue with your assigned responsibilities.</p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;border:1px solid #d7e3dc;border-radius:16px;overflow:hidden;background:#f8fcfa;margin:24px 0;">
              <tr>
                <td colspan="2" style="padding:14px 18px;background:#e7f6ee;color:#166534;font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;">Account Details</td>
              </tr>
              %s
            </table>
            <p style="margin:0;">Use the same email shown above to sign in to BillMyGym.</p>
            """.formatted(html(userName), details);

        return buildShell(
            "Account ready",
            "Welcome to BillMyGym",
            "Your access has been provisioned successfully.",
            content,
            "Open BillMyGym",
            "/login",
            "Please do not share your credentials with anyone."
        );
    }

    private String buildUserAccessRevokedEmail() {
        String content = """
            <p style="margin:0 0 16px 0;">Your BillMyGym access is no longer active.</p>
            <p style="margin:0;">If you believe this change was made in error, please contact your administrator or the BillMyGym support team.</p>
            """;
        return buildShell(
            "Access updated",
            "BillMyGym",
            "Your account permissions have changed.",
            content,
            "Visit BillMyGym",
            "/login",
            "This is an automated security notification."
        );
    }

    private String buildOtpEmail(String otp) {
        String content = """
            <p style="margin:0 0 16px 0;">We received a request to reset your BillMyGym password.</p>
            <p style="margin:0 0 22px 0;">Use the one-time code below to continue. The code remains valid for <strong>10 minutes</strong>.</p>
            <div style="margin:0 0 24px 0;text-align:center;">
              <span style="display:inline-block;background:#0f172a;color:#f8fafc;padding:16px 26px;border-radius:18px;font-size:30px;font-weight:700;letter-spacing:8px;">%s</span>
            </div>
            <div style="background:#fff7ed;border:1px solid #fed7aa;border-radius:16px;padding:18px 20px;margin:0 0 22px 0;">
              <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#c2410c;margin-bottom:10px;">Security Note</div>
              <p style="margin:0;color:#9a3412;line-height:1.7;">If you did not request this password reset, you can ignore this email. Your current password will stay unchanged.</p>
            </div>
            <p style="margin:0;">For your safety, never share this code with anyone.</p>
            """.formatted(html(otp));

        return buildShell(
            "Password reset",
            "BillMyGym Security",
            "Use this code to complete your password reset.",
            content,
            "Open BillMyGym",
            "/login",
            "This code is intended only for the owner of the account."
        );
    }

    private String buildSubscriptionReminderEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        String content = """
            <p style="margin:0 0 16px 0;">Hello <strong>%s</strong>,</p>
            <p style="margin:0 0 16px 0;">Your BillMyGym launch plan subscription is coming due. Please complete payment to keep dashboards, member operations, reminders, email, and WhatsApp workflows available without interruption.</p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;border:1px solid #d7e3dc;border-radius:16px;overflow:hidden;background:#f8fcfa;margin:24px 0;">
              <tr>
                <td colspan="2" style="padding:14px 18px;background:#e7f6ee;color:#166534;font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;">Subscription Summary</td>
              </tr>
              %s
              %s
              %s
              %s
              %s
              %s
            </table>
            <div style="background:#fff7ed;border:1px solid #fed7aa;border-radius:16px;padding:18px 20px;margin:0 0 22px 0;">
              <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#c2410c;margin-bottom:10px;">Last Billing Period</div>
              <p style="margin:0;color:#9a3412;line-height:1.7;">%s</p>
            </div>
            <p style="margin:0;">Thank you for running your gym on BillMyGym.</p>
            """.formatted(
            html(organization.getName()),
            detailRow("Plan", PLAN_NAME + " - INR 299/month"),
            detailRow("Amount Due", "INR " + amountDue),
            detailRow("Due Date", dueDate == null ? "-" : dueDate.toString()),
            detailRow("WhatsApp Usage", String.valueOf(whatsappUsage)),
            detailRow("Email Usage", String.valueOf(emailUsage)),
            detailRow("Active Members", String.valueOf(activeMembers)),
            html(lastBillingSummary)
        );

        return buildShell(
            "Subscription reminder",
            "BillMyGym Billing",
            "Your monthly SaaS subscription is approaching its due date.",
            content,
            "Pay Subscription",
            paymentLink == null || paymentLink.isBlank() ? "/app/subscription" : paymentLink,
            "This email is personalized for your organization subscription."
        );
    }

    private String buildTrialEndingEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        return buildBillingTemplate(
            "Free Trial Access",
            "BillMyGym Billing",
            "Your free trial access is ending soon. Complete payment to keep access uninterrupted.",
            organization,
            amountDue,
            dueDate,
            paymentLink,
            whatsappUsage,
            emailUsage,
            activeMembers,
            lastBillingSummary,
            "Free trial access ending notice"
        );
    }

    private String buildPaymentSuccessEmail(
        Organization organization,
        BigDecimal amountPaid,
        LocalDate paidOn,
        LocalDate accessThrough,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        return buildBillingTemplate(
            "Payment successful",
            "BillMyGym Billing",
            "Your subscription payment was received successfully.",
            organization,
            amountPaid,
            accessThrough,
            "/app/subscription",
            whatsappUsage,
            emailUsage,
            activeMembers,
            "Paid on %s. %s".formatted(paidOn, lastBillingSummary),
            "Payment confirmation"
        );
    }

    private String buildPaymentFailedEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        String failureReason,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        return buildBillingTemplate(
            "Payment failed",
            "BillMyGym Billing",
            "We could not collect your subscription payment. Please retry to avoid access interruption.",
            organization,
            amountDue,
            dueDate,
            paymentLink,
            whatsappUsage,
            emailUsage,
            activeMembers,
            "Failure reason: %s. %s".formatted(failureReason == null ? "Unavailable" : failureReason, lastBillingSummary),
            "Payment failure notice"
        );
    }

    private String buildSuperAdminSubscriptionPaymentEmail(OrganizationSubscriptionPayment payment) {
        Organization organization = payment.getOrganization();
        String status = payment.getPaymentStatus() == null ? "-" : payment.getPaymentStatus().name();
        String title = "Subscription " + status.toLowerCase(Locale.ENGLISH);
        String content = """
            <p style="margin:0 0 16px 0;">A BillMyGym organization subscription payment changed state.</p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;border:1px solid #d7e3dc;border-radius:16px;overflow:hidden;background:#f8fcfa;margin:24px 0;">
              <tr>
                <td colspan="2" style="padding:14px 18px;background:#e7f6ee;color:#166534;font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;">Subscription Event</td>
              </tr>
              %s
              %s
              %s
              %s
              %s
              %s
              %s
              %s
            </table>
            """.formatted(
            detailRow("Organization", organization == null ? "-" : organization.getName()),
            detailRow("Plan", payment.getSubscriptionPlan() == null ? "-" : payment.getSubscriptionPlan().name()),
            detailRow("Status", status),
            detailRow("Amount", currency(payment.getAmount())),
            detailRow("Payment Date", payment.getPaymentDate() == null ? "-" : payment.getPaymentDate().toString()),
            detailRow("Period", "%s to %s".formatted(payment.getSubscriptionStart(), payment.getSubscriptionEnd())),
            detailRow("Gateway Payment", payment.getRazorpayPaymentId() == null ? "-" : payment.getRazorpayPaymentId()),
            detailRow("Failure Reason", payment.getFailureReason() == null ? "-" : payment.getFailureReason())
        );

        return buildShell(
            title,
            "Platform subscription alert",
            "A subscription payment event was recorded for an organization.",
            content,
            "Open Subscription Audit",
            "/app/audit-logs",
            "This email was sent to active BillMyGym super admins."
        );
    }

    private String buildPausedNotificationEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        return buildBillingTemplate(
            "Subscription Renewal Required",
            "BillMyGym Billing",
            "Your subscription renewal is required because payment is overdue.",
            organization,
            amountDue,
            dueDate,
            paymentLink,
            whatsappUsage,
            emailUsage,
            activeMembers,
            lastBillingSummary,
            "Subscription renewal required notice"
        );
    }

    private String buildDeactivatedNotificationEmail(
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary
    ) {
        return buildBillingTemplate(
            "Account Inactive",
            "BillMyGym Billing",
            "Your account is inactive after extended non-payment. Contact support after clearing dues.",
            organization,
            amountDue,
            dueDate,
            paymentLink,
            whatsappUsage,
            emailUsage,
            activeMembers,
            lastBillingSummary,
            "Account inactive notice"
        );
    }

    private String buildMonthlyOrganizationReportEmail(
        Organization organization,
        MonthlyOrganizationReportService.MonthlyOrganizationReport report
    ) {
        long revenueTotal = report.collectedRevenue().add(report.pendingRevenue()).longValue();
        int collectedPercent = revenueTotal <= 0
            ? 0
            : (int) Math.min(Math.round((report.collectedRevenue().longValue() * 100.0) / revenueTotal), 100);
        String content = """
            <p style="margin:0 0 18px 0;">Hello <strong>%s</strong>,</p>
            <p style="margin:0 0 22px 0;">Here is your monthly GymFlow report for <strong>%s</strong>.</p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:separate;border-spacing:12px;margin:0 -12px 20px -12px;">
              <tr>
                %s
                %s
                %s
              </tr>
              <tr>
                %s
                %s
                %s
              </tr>
            </table>
            <div style="border:1px solid #d7e3dc;border-radius:18px;padding:20px;background:#f8fcfa;margin:0 0 22px 0;">
              <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#166534;margin-bottom:14px;">Revenue Trend</div>
              <div style="height:12px;background:#e2e8f0;border-radius:999px;overflow:hidden;">
                <div style="height:12px;width:%d%%;background:#16a34a;border-radius:999px;"></div>
              </div>
              <p style="margin:12px 0 0 0;color:#475569;line-height:1.7;">Collected revenue compared with overdue revenue exposure at month end.</p>
            </div>
            <p style="margin:0;">Keep growing, and keep your renewal follow-ups steady.</p>
            """.formatted(
            html(organization.getAdminFullName()),
            html(report.label()),
            summaryCard("New Members", String.valueOf(report.membersJoined()), "#e0f2fe", "#075985"),
            summaryCard("Active Members", String.valueOf(report.activeMembers()), "#dcfce7", "#166534"),
            summaryCard("Deactivated Members", String.valueOf(report.deactivatedMembers()), "#fff7ed", "#c2410c"),
            summaryCard("Collected Revenue", currency(report.collectedRevenue()), "#ecfdf5", "#047857"),
            summaryCard("Overdue Revenue", currency(report.pendingRevenue()), "#fef3c7", "#92400e"),
            summaryCard("Usage", report.whatsappSentCount() + " WhatsApp / " + report.emailSentCount() + " emails | " + report.overdueMembers() + " overdue", "#f8fafc", "#334155"),
            collectedPercent
        );

        return buildShell(
            "Monthly GymFlow Report",
            "Monthly report",
            "Summary cards, usage details, revenue, and trend signals for your gym.",
            content,
            "Open Dashboard",
            "/app/dashboard",
            "Monthly reports are included with every BillMyGym subscription."
        );
    }

    private String buildBillingTemplate(
        String title,
        String eyebrow,
        String subtitle,
        Organization organization,
        BigDecimal amountDue,
        LocalDate dueDate,
        String paymentLink,
        long whatsappUsage,
        long emailUsage,
        long activeMembers,
        String lastBillingSummary,
        String footerNote
    ) {
        String content = """
            <p style="margin:0 0 16px 0;">Hello <strong>%s</strong>,</p>
            <p style="margin:0 0 16px 0;">%s</p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;border:1px solid #d7e3dc;border-radius:16px;overflow:hidden;background:#f8fcfa;margin:24px 0;">
              <tr>
                <td colspan="2" style="padding:14px 18px;background:#e7f6ee;color:#166534;font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;">Billing Summary</td>
              </tr>
              %s
              %s
              %s
              %s
              %s
              %s
              %s
            </table>
            <div style="background:#fff7ed;border:1px solid #fed7aa;border-radius:16px;padding:18px 20px;margin:0 0 22px 0;">
              <div style="font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:#c2410c;margin-bottom:10px;">Last Month Summary</div>
              <p style="margin:0;color:#9a3412;line-height:1.7;">%s</p>
            </div>
            """.formatted(
            html(organization.getName()),
            html(subtitle),
            detailRow("Organization", organization.getName()),
            detailRow("Plan", PLAN_NAME),
            detailRow("Amount Due", "INR " + amountDue),
            detailRow("Due Date", dueDate == null ? "-" : dueDate.toString()),
            detailRow("Active Members", String.valueOf(activeMembers)),
            detailRow("WhatsApp Usage", String.valueOf(whatsappUsage)),
            detailRow("Email Usage", String.valueOf(emailUsage)),
            html(lastBillingSummary)
        );

        return buildShell(
            title,
            eyebrow,
            subtitle,
            content,
            "Open Billing",
            paymentLink == null || paymentLink.isBlank() ? "/app/subscription" : paymentLink,
            footerNote
        );
    }

    private String buildShell(
        String title,
        String eyebrow,
        String subtitle,
        String content,
        String ctaLabel,
        String ctaPath,
        String footerNote
    ) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background:#eef4f0;font-family:%s;color:#1f2937;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#eef4f0;padding:24px 12px;">
                <tr>
                  <td align="center">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:640px;background:#ffffff;border-radius:28px;overflow:hidden;box-shadow:0 24px 60px rgba(15,23,42,0.12);">
                      <tr>
                        <td style="padding:34px 36px;background:linear-gradient(135deg,#0f172a 0%%,#14532d 100%%);">
                          <div style="font-size:12px;font-weight:700;letter-spacing:0.16em;text-transform:uppercase;color:#bbf7d0;margin-bottom:14px;">%s</div>
                          <div style="font-size:32px;line-height:1.18;font-weight:800;color:#f8fafc;margin-bottom:12px;">%s</div>
                          <div style="font-size:15px;line-height:1.8;color:#d1fae5;">%s</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:34px 36px 28px 36px;font-size:15px;line-height:1.8;color:#334155;">
                          %s
                        </td>
                      </tr>
                      <tr>
                        <td align="center" style="padding:0 36px 30px 36px;">
                          <a href="%s" style="display:inline-block;background:#16a34a;color:#ffffff;text-decoration:none;padding:14px 26px;border-radius:999px;font-size:14px;font-weight:700;box-shadow:0 12px 28px rgba(22,163,74,0.28);">%s</a>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:22px 36px 30px 36px;border-top:1px solid #e5e7eb;font-size:12px;line-height:1.8;color:#64748b;text-align:center;">
                          <div style="margin-bottom:8px;font-weight:700;color:#0f172a;">BillMyGym</div>
                          <div>%s</div>
                          <div style="margin-top:8px;">&copy; %d BillMyGym. All rights reserved.</div>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
            html(title),
            FONT_STACK,
            html(eyebrow),
            html(title),
            html(subtitle),
            content,
            html(appUrl(ctaPath)),
            html(ctaLabel),
            html(footerNote),
            LocalDate.now(APP_ZONE).getYear()
        );
    }

    private String detailRow(String label, String value) {
        return """
            <tr>
              <td style="width:36%%;padding:14px 18px;border-top:1px solid #d7e3dc;font-size:14px;font-weight:700;color:#0f172a;">%s</td>
              <td style="padding:14px 18px;border-top:1px solid #d7e3dc;font-size:14px;color:#334155;">%s</td>
            </tr>
            """.formatted(html(label), value);
    }

    private String organizationStatusLabel(Organization organization) {
        if (organization == null || organization.getStatus() == null) {
            return "-";
        }
        return switch (organization.getStatus()) {
            case TRIAL -> "Free Trial Access";
            case ACTIVE -> "Active Subscription";
            case PAUSED -> "Subscription Renewal Required";
            case DEACTIVATED -> "Account Inactive";
        };
    }

    private String summaryCard(String label, String value, String background, String color) {
        return """
            <td width="33.33%%" style="background:%s;border-radius:16px;padding:18px;vertical-align:top;">
              <div style="font-size:11px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;color:%s;margin-bottom:10px;">%s</div>
              <div style="font-size:22px;line-height:1.2;font-weight:800;color:#0f172a;">%s</div>
            </td>
            """.formatted(background, color, html(label), html(value));
    }

    private String currency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value == null ? BigDecimal.ZERO : value);
    }

    private String appUrl(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return APP_URL;
        }
        return APP_URL + (path.startsWith("/") ? path : "/" + path);
    }

    private String formatRole(String role) {
        if (role == null || role.isBlank()) {
            return "Team member";
        }
        String[] parts = role.replace('_', ' ').toLowerCase().trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? "Team member" : builder.toString();
    }

    private String html(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("(?i)</p>", "\n\n")
            .replaceAll("(?i)</li>", "\n")
            .replaceAll("(?i)<li>", "- ")
            .replaceAll("<[^>]+>", " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replaceAll("[ \\t]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }
}

