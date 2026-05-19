package com.gymflow.service;

import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberLimitService {

    private final MemberRepository memberRepository;
    private final PlanAccessService planAccessService;

    public void validateCanCreateMember(Organization organization, MemberStatus requestedStatus) {
        if (requestedStatus != null && requestedStatus != MemberStatus.ACTIVE) {
            return;
        }
        MemberLimitSummary usage = usage(organization);
        if (!usage.unlimited() && usage.usedMembers() >= usage.memberLimit()) {
            throw new BadRequestException(
                "Member limit reached for the %s plan (%d/%d active members)."
                    .formatted(usage.plan().name(), usage.usedMembers(), usage.memberLimit())
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
        long usedMembers = memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(
            organization.getId(),
            MemberStatus.ACTIVE
        );
        Integer limit = plan.memberLimit();
        boolean unlimited = limit == null;
        long remaining = unlimited ? Long.MAX_VALUE : Math.max(limit - usedMembers, 0);
        int usagePercent = unlimited || limit == 0
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
