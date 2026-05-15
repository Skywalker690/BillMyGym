package com.gymflow.service;

import com.gymflow.api.dto.AnalyticsDtos.OrganizationAnalyticsResponse;
import com.gymflow.api.dto.AnalyticsDtos.PlatformAnalyticsResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.NotificationLogRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSubscriptionRepository;
import com.gymflow.security.SecurityUtils;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final NotificationLogRepository notificationLogRepository;

    public PlatformAnalyticsResponse platformAnalytics() {
        return new PlatformAnalyticsResponse(
            organizationRepository.findAllByDeletedFalse().size(),
            subscriptionRepository.countByDeletedFalseAndEndDateGreaterThanEqual(LocalDate.now())
        );
    }

    public OrganizationAnalyticsResponse organizationAnalytics() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }

        return new OrganizationAnalyticsResponse(
            memberRepository.countByOrganizationIdAndDeletedFalse(organizationId),
            notificationLogRepository.countByOrganizationIdAndDeletedFalse(organizationId)
        );
    }
}
