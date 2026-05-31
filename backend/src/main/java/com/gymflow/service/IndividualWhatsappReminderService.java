package com.gymflow.service;

import com.gymflow.api.dto.ReminderDtos.WhatsappReminderPreviewResponse;
import com.gymflow.api.dto.ReminderDtos.WhatsappReminderSendResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.NotificationLog;
import com.gymflow.domain.Organization;
import com.gymflow.domain.WhatsappReminderLog;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.NotificationChannel;
import com.gymflow.domain.enums.NotificationDeliveryStatus;
import com.gymflow.domain.enums.NotificationStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.WhatsappReminderStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.WhatsappReminderLogRepository;
import com.gymflow.security.SecurityUtils;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IndividualWhatsappReminderService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter MESSAGE_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final int CUSTOM_REMINDER_LIMIT = 50;

    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final WhatsappReminderLogRepository whatsappReminderLogRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final MemberMembershipService memberMembershipService;
    private final PlanAccessService planAccessService;
    private final WhatsAppTemplateService whatsAppTemplateService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;
    private final AuditLogService auditLogService;

    @Transactional
    public WhatsappReminderPreviewResponse preview(Long memberId) {
        ReminderContext context = resolveContext(memberId);
        SendEligibility eligibility = sendEligibility(context);
        return toPreview(context, eligibility);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public WhatsappReminderSendResponse send(Long memberId) {
        ReminderContext context = resolveContext(memberId);
        SendEligibility eligibility = sendEligibility(context);
        if (!eligibility.canSend()) {
            throw new BadRequestException(eligibility.reason());
        }

        WhatsappReminderLog reminderLog = new WhatsappReminderLog();
        reminderLog.setOrganization(context.organization());
        reminderLog.setMember(context.member());
        reminderLog.setMessage(context.message());
        reminderLog.setStatus(WhatsappReminderStatus.PENDING);
        reminderLog = whatsappReminderLogRepository.save(reminderLog);

        NotificationLog notification = buildNotificationLog(context, eligibility.normalizedPhone());
        notification = notificationLogRepository.save(notification);

        var result = whatsAppDeliveryService.sendText(
            context.organization(),
            notification,
            eligibility.normalizedPhone(),
            context.message(),
            NotificationType.INDIVIDUAL_WHATSAPP_REMINDER
        );

        OffsetDateTime now = OffsetDateTime.now(APP_ZONE);
        notification.setAttemptCount(result.attemptCount());
        notification.setFailureReason(result.errorMessage());

        if (result.sent()) {
            reminderLog.setStatus(WhatsappReminderStatus.SENT);
            reminderLog.setSentAt(now);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(now);
            audit(context, "WHATSAPP_MEMBER_REMINDER_SENT", AuditEventStatus.SUCCESS);
        } else {
            reminderLog.setStatus(WhatsappReminderStatus.FAILED);
            notification.setStatus(result.status() == NotificationDeliveryStatus.RETRYING
                ? NotificationStatus.RETRYING
                : NotificationStatus.FAILED);
            audit(context, "WHATSAPP_MEMBER_REMINDER_FAILED", AuditEventStatus.FAILED);
        }

        whatsappReminderLogRepository.save(reminderLog);
        notificationLogRepository.save(notification);

        return new WhatsappReminderSendResponse(
            reminderLog.getId(),
            context.member().getId(),
            reminderLog.getStatus(),
            reminderLog.getStatus() == WhatsappReminderStatus.SENT,
            reminderLog.getSentAt(),
            result.errorMessage()
        );
    }

    private ReminderContext resolveContext(Long memberId) {
        Long organizationId = requireOrganizationId();
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        memberMembershipService.refreshMembershipStatuses(organizationId);
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, organizationId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BadRequestException("Only active member profiles can receive WhatsApp reminders");
        }

        MemberMembership membership = latestMembership(memberId, organizationId);
        if (membership == null || membership.getStatus() != MembershipStatus.OVERDUE) {
            throw new BadRequestException("WhatsApp reminders can be sent only to overdue members");
        }

        LocalDate dueDate = membership.getEndDate().plusDays(1);
        BigDecimal pendingAmount = organization.getMonthlyMembershipFee() == null
            ? BigDecimal.ZERO
            : organization.getMonthlyMembershipFee();
        String message = buildMessage(member, organization, dueDate, pendingAmount);
        return new ReminderContext(organization, member, membership, dueDate, pendingAmount, message);
    }

    private MemberMembership latestMembership(Long memberId, Long organizationId) {
        return memberMembershipRepository
            .findByMemberIdAndOrganizationIdAndDeletedFalseOrderByStartDateDescCreatedAtDesc(memberId, organizationId)
            .stream()
            .max(
                Comparator.comparing(MemberMembership::getEndDate)
                    .thenComparing(MemberMembership::getStartDate)
                    .thenComparing(MemberMembership::getCreatedAt)
            )
            .orElse(null);
    }

    private SendEligibility sendEligibility(ReminderContext context) {
        PhoneCheck phoneCheck = normalizePhone(context.member().getPhone());
        if (!phoneCheck.valid()) {
            return new SendEligibility(false, phoneCheck.reason(), "");
        }

        int customReminderUsage = currentMonthCustomReminderUsage(context.organization());
        Integer quota = monthlyCustomReminderLimit(context.organization());
        if (quota != null && customReminderUsage >= quota) {
            return new SendEligibility(false, "Monthly custom reminder limit reached", phoneCheck.normalizedPhone());
        }

        return new SendEligibility(true, null, phoneCheck.normalizedPhone());
    }

    private PhoneCheck normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return new PhoneCheck(false, "", "No WhatsApp number found");
        }
        String digits = whatsAppTemplateService.normalizeRecipient(phone);
        if (digits.length() == 10) {
            digits = "91" + digits;
        }
        if (digits.length() < 10 || digits.length() > 15) {
            return new PhoneCheck(false, digits, "Invalid WhatsApp number");
        }
        return new PhoneCheck(true, digits, null);
    }

    public Integer monthlyCustomReminderLimit(Organization organization) {
        return CUSTOM_REMINDER_LIMIT;
    }

    public int currentMonthCustomReminderUsage(Organization organization) {
        YearMonth currentMonth = YearMonth.now(APP_ZONE);
        OffsetDateTime monthStart = currentMonth.atDay(1).atStartOfDay(APP_ZONE).toOffsetDateTime();
        OffsetDateTime nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay(APP_ZONE).toOffsetDateTime();
        return Math.toIntExact(whatsappReminderLogRepository.countByOrganizationIdAndStatusAndSentAtBetweenAndDeletedFalse(
            organization.getId(),
            WhatsappReminderStatus.SENT,
            monthStart,
            nextMonthStart
        ));
    }

    private String buildMessage(Member member, Organization organization, LocalDate dueDate, BigDecimal pendingAmount) {
        return """
            Hello %s,

            Your gym subscription payment is overdue.

            Due Date:
            %s

            Pending Amount:
            INR %s

            Please complete payment.

            Thank you,
            %s
            """.formatted(
                firstName(member.getFullName()),
                dueDate.format(MESSAGE_DATE),
                formatAmount(pendingAmount),
                organization.getName()
            ).trim();
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Member";
        }
        return fullName.trim().split("\\s+")[0];
    }

    private String formatAmount(BigDecimal amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        numberFormat.setMaximumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return numberFormat.format(amount);
    }

    private WhatsappReminderPreviewResponse toPreview(ReminderContext context, SendEligibility eligibility) {
        Integer monthlyLimit = monthlyCustomReminderLimit(context.organization());
        int used = currentMonthCustomReminderUsage(context.organization());
        int remaining = monthlyLimit == null ? 0 : Math.max(monthlyLimit - used, 0);
        return new WhatsappReminderPreviewResponse(
            context.member().getId(),
            context.member().getFullName(),
            context.member().getPhone(),
            context.dueDate(),
            context.pendingAmount(),
            context.organization().getName(),
            context.message(),
            eligibility.canSend(),
            eligibility.reason(),
            monthlyLimit,
            used,
            remaining
        );
    }

    private NotificationLog buildNotificationLog(ReminderContext context, String normalizedPhone) {
        NotificationLog notification = new NotificationLog();
        notification.setOrganization(context.organization());
        notification.setMember(context.member());
        notification.setMemberCount(1);
        notification.setType(NotificationType.INDIVIDUAL_WHATSAPP_REMINDER);
        notification.setChannel(NotificationChannel.WHATSAPP);
        notification.setRecipient(normalizedPhone);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setAttemptCount(0);
        notification.setNotificationDate(LocalDate.now(APP_ZONE));
        notification.setMessage(context.message());
        return notification;
    }

    private void audit(ReminderContext context, String event, AuditEventStatus status) {
        auditLogService.logOrganizationActivity(
            event,
            "Individual WhatsApp overdue reminder for " + context.member().getFullName(),
            AuditActivityScope.NOTIFICATION_ACTIVITY,
            status,
            "MEMBER",
            context.organization().getId(),
            context.member().getFullName()
        );
    }

    private Long requireOrganizationId() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationId;
    }

    private record ReminderContext(
        Organization organization,
        Member member,
        MemberMembership membership,
        LocalDate dueDate,
        BigDecimal pendingAmount,
        String message
    ) {
    }

    private record SendEligibility(boolean canSend, String reason, String normalizedPhone) {
    }

    private record PhoneCheck(boolean valid, String normalizedPhone, String reason) {
    }
}
