package com.gymflow.service;

import jakarta.mail.internet.MimeMessage;
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

    private static final String SENDER_NAME = "GymFlow";
    private static final String APP_URL = "https://gymmax.com";
    private static final String FONT_STACK = "Arial,Helvetica,sans-serif";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOrganizationAdminWelcomeEmail(
        String toEmail,
        String adminName,
        String organizationName,
        String temporaryPassword,
        String district
    ) {
        sendHtmlEmail(
            toEmail,
            "Welcome to GymFlow - Your organization is ready",
            buildOrganizationAdminWelcomeEmail(toEmail, adminName, organizationName, temporaryPassword, district),
            true
        );
    }

    @Async
    public void sendUserWelcomeEmail(String toEmail, String userName, String role, String zone) {
        sendHtmlEmail(
            toEmail,
            "Welcome to GymFlow",
            buildGenericUserWelcomeEmail(toEmail, userName, role, zone),
            true
        );
    }

    @Async
    public void sendUserAccessRevokedEmail(String toEmail) {
        sendHtmlEmail(
            toEmail,
            "GymFlow access update",
            buildUserAccessRevokedEmail(),
            true
        );
    }

    public void sendOtpEmail(String toEmail, String otp) {
        sendHtmlEmail(
            toEmail,
            "Your GymFlow password reset code",
            buildOtpEmail(otp),
            false
        );
    }

    private void sendHtmlEmail(String toEmail, String subject, String html, boolean swallowFailure) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email '{}' sent to {}", subject, toEmail);
        } catch (Exception ex) {
            log.error("Failed to send email '{}' to {}", subject, toEmail, ex);
            if (!swallowFailure) {
                throw new RuntimeException("Failed to send email", ex);
            }
        }
    }

    private String buildOrganizationAdminWelcomeEmail(
        String toEmail,
        String adminName,
        String organizationName,
        String temporaryPassword,
        String district
    ) {
        String content = """
            <p style="margin:0 0 16px 0;">Hello <strong>%s</strong>,</p>
            <p style="margin:0 0 16px 0;">Your organization workspace on <strong>GymFlow</strong> is now ready. You can sign in and start managing memberships, payments, reminders, and reports from one place.</p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;border:1px solid #d7e3dc;border-radius:16px;overflow:hidden;background:#f8fcfa;margin:24px 0;">
              <tr>
                <td colspan="2" style="padding:14px 18px;background:#e7f6ee;color:#166534;font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;">Organization Access</td>
              </tr>
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
            <p style="margin:0;">If you need help, contact the GymFlow support team and include your organization name so we can assist faster.</p>
            """.formatted(
            html(adminName),
            detailRow("Organization", organizationName),
            detailRow("District", district),
            detailRow("Admin Email", toEmail),
            detailRow("Temporary Password", temporaryPassword),
            detailRow("Sign-in URL", "<a href=\"" + html(appUrl("/login")) + "\" style=\"color:#166534;font-weight:700;text-decoration:none;\">"
                + html(appUrl("/login")) + "</a>")
        );

        return buildShell(
            "Organization ready",
            "Welcome to GymFlow",
            "Your admin account has been created successfully.",
            content,
            "Open GymFlow",
            "/login",
            "This email was sent because a GymFlow super admin created your organization workspace."
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
            <p style="margin:0 0 16px 0;">Your GymFlow access is active. You can now sign in and continue with your assigned responsibilities.</p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;border:1px solid #d7e3dc;border-radius:16px;overflow:hidden;background:#f8fcfa;margin:24px 0;">
              <tr>
                <td colspan="2" style="padding:14px 18px;background:#e7f6ee;color:#166534;font-size:12px;font-weight:700;letter-spacing:0.12em;text-transform:uppercase;">Account Details</td>
              </tr>
              %s
            </table>
            <p style="margin:0;">Use the same email shown above to sign in to GymFlow.</p>
            """.formatted(html(userName), details);

        return buildShell(
            "Account ready",
            "Welcome to GymFlow",
            "Your access has been provisioned successfully.",
            content,
            "Open GymFlow",
            "/login",
            "Please do not share your credentials with anyone."
        );
    }

    private String buildUserAccessRevokedEmail() {
        String content = """
            <p style="margin:0 0 16px 0;">Your GymFlow access is no longer active.</p>
            <p style="margin:0;">If you believe this change was made in error, please contact your administrator or the GymFlow support team.</p>
            """;
        return buildShell(
            "Access updated",
            "GymFlow",
            "Your account permissions have changed.",
            content,
            "Visit GymFlow",
            "/login",
            "This is an automated security notification."
        );
    }

    private String buildOtpEmail(String otp) {
        String content = """
            <p style="margin:0 0 16px 0;">We received a request to reset your GymFlow password.</p>
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
            "GymFlow Security",
            "Use this code to complete your password reset.",
            content,
            "Open GymFlow",
            "/login",
            "This code is intended only for the owner of the account."
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
                          <div style="margin-bottom:8px;font-weight:700;color:#0f172a;">GymFlow</div>
                          <div>%s</div>
                          <div style="margin-top:8px;">&copy; %d GymFlow. All rights reserved.</div>
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
