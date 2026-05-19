package com.gymflow.service;

import com.gymflow.api.dto.AnalyticsDtos.OrganizationAnalyticsResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformAnalyticsResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.domain.enums.OrganizationStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.Role;
import com.gymflow.domain.enums.SubscriptionPaymentStatus;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionPaymentRepository;
import com.gymflow.repository.OrganizationSubscriptionRepository;
import com.gymflow.repository.UsageMetricRepository;
import com.gymflow.security.SecurityUtils;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationSubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UsageMetricRepository usageMetricRepository;
    private final MemberRepository memberRepository;
    private final NotificationLogRepository notificationLogRepository;

    public PlatformAnalyticsResponse platformAnalytics() {
        if (SecurityUtils.currentUser().role() != Role.SUPER_ADMIN) {
            throw new ForbiddenException("Only SUPER_ADMIN can access platform analytics");
        }
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();
        var usage = usageMetricRepository.findByYearAndMonthAndDeletedFalse(
            currentMonth.getYear(),
            currentMonth.getMonthValue()
        );
        return new PlatformAnalyticsResponse(
            organizationRepository.findAllByDeletedFalse().size(),
            organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.ACTIVE),
            organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.TRIAL),
            organizationRepository.countByDeletedFalseAndOrganizationType(OrganizationType.PILOT),
            organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.PAUSED),
            organizationRepository.countByDeletedFalseAndStatus(OrganizationStatus.DEACTIVATED),
            subscriptionPaymentRepository.sumPaidBetween(monthStart, monthEnd),
            subscriptionPaymentRepository.countByPaymentStatusInAndDeletedFalse(
                List.of(SubscriptionPaymentStatus.FAILED, SubscriptionPaymentStatus.PENDING)
            ),
            usage.stream().mapToLong(metric -> metric.getMonthlyWhatsappCount()).sum(),
            usage.stream().mapToLong(metric -> metric.getMonthlyEmailCount()).sum()
        );
    }

    public OrganizationAnalyticsResponse organizationAnalytics() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return new OrganizationAnalyticsResponse(
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(organizationId, MemberStatus.ACTIVE),
            notificationLogRepository.countByOrganizationIdAndTypeAndDeletedFalse(
                organizationId,
                NotificationType.ORGANIZATION_RENEWAL_DIGEST
            )
        );
    }
}
