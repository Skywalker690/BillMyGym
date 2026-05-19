package com.gymflow.service;

import com.gymflow.domain.Organization;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    private static final String PLAN_NAME = "BillMyGym Launch";
    private static final String SENDER_NAME = "BillMyGym";
    private static final String APP_URL = "https://gymmax.com";
    private static final String FONT_STACK = "Arial,Helvetica,sans-serif";

    private final JavaMailSender mailSender;
    private final UsageMetricService usageMetricService;

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
            "BillMyGym trial ending - " + organization.getName(),
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
            "BillMyGym access paused - " + organization.getName(),
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
            "BillMyGym organization deactivated - " + organization.getName(),
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

    private void sendHtmlEmail(String toEmail, String subject, String html, boolean swallowFailure, Organization organization) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
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
            detailRow("Access Type", organization.getOrganizationType().name()),
            detailRow("Lifecycle Status", organization.getStatus().name()),
            detailRow("Free Access Ends", accessEndsAt == null ? "-" : accessEndsAt.toString()),
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
        if (zone != null && !zone.isBlank() && !"-".equals(zone.trim())) {
            details.append(detailRow("Assigned Zone", html(zone)));
        }

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
            "Trial ending",
            "BillMyGym Billing",
            "Your trial is ending soon. Complete payment to keep access uninterrupted.",
            organization,
            amountDue,
            dueDate,
            paymentLink,
            whatsappUsage,
            emailUsage,
            activeMembers,
            lastBillingSummary,
            "Trial ending notice"
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
            "Organization paused",
            "BillMyGym Billing",
            "Your organization access is paused because payment is overdue.",
            organization,
            amountDue,
            dueDate,
            paymentLink,
            whatsappUsage,
            emailUsage,
            activeMembers,
            lastBillingSummary,
            "Paused subscription notice"
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
            "Organization deactivated",
            "BillMyGym Billing",
            "Your organization was deactivated after extended non-payment. Contact support after clearing dues.",
            organization,
            amountDue,
            dueDate,
            paymentLink,
            whatsappUsage,
            emailUsage,
            activeMembers,
            lastBillingSummary,
            "Deactivated subscription notice"
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
            LocalDate.now().getYear()
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
}

