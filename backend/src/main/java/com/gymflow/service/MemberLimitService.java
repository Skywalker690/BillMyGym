package com.gymflow.service;

import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberLimitService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final PlanAccessService planAccessService;

    public void validateCanCreateMember(Organization organization, MemberStatus requestedStatus) {
        if (requestedStatus != null && requestedStatus != MemberStatus.ACTIVE) {
            return;
        }
        if (organization.getOrganizationType() != OrganizationType.DEMO) {
            return;
        }
        MemberLimitSummary usage = usage(organization);
        if (!usage.unlimited() && usage.usedMembers() >= usage.memberLimit()) {
            throw new BadRequestException(
                "Demo member sandbox limit reached (%d/%d members)."
                    .formatted(usage.usedMembers(), usage.memberLimit())
            );
        }
    }

    public void validateCanActivateMember(Organization organization, MemberStatus currentStatus, MemberStatus requestedStatus) {
        if (currentStatus == MemberStatus.ACTIVE || requestedStatus != MemberStatus.ACTIVE) {
            return;
        }
        validateCanCreateMember(organization, requestedStatus);
    }

    public MemberLimitSummary usage(Organization organization) {
        SubscriptionPlan plan = planAccessService.resolvePlan(organization);
        long usedMembers = organization.getOrganizationType() == OrganizationType.DEMO
            ? memberRepository.countByOrganizationIdAndDeletedFalse(organization.getId())
            : memberMembershipRepository.countBillableActiveMembers(organization.getId(), LocalDate.now(APP_ZONE));
        Integer limit;
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            limit = Integer.valueOf(50);
        } else {
            limit = SubscriptionPricingService.INCLUDED_ACTIVE_MEMBERS;
        }
        boolean unlimited = false;
        long remaining = Math.max(limit - usedMembers, 0);
        int usagePercent = limit == 0
            ? 0
            : (int) Math.min(Math.round((usedMembers * 100.0) / limit), 100);
        return new MemberLimitSummary(plan, usedMembers, limit, unlimited, remaining, usagePercent);
    }

    public record MemberLimitSummary(
        SubscriptionPlan plan,
        long usedMembers,
        Integer memberLimit,
        boolean unlimited,
        long remainingSlots,
        int usagePercent
    ) {
    }
}
