package com.gymflow.service;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.dto.MemberDtos.MemberResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.api.mapper.MemberMapper;
import com.gymflow.domain.Member;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.NotificationType;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberMapper memberMapper;
    private final MembershipLifecycleService membershipLifecycleService;
    private final OrganizationSettingService organizationSettingService;
    private final NotificationDispatcherService notificationDispatcherService;
    private final AuditLogService auditLogService;

    @Transactional
    public List<MemberResponse> listMembers() {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        int reminderDays = organizationSettingService.getOrCreateSettings(orgId).getReminderDaysBeforeExpiry();
        return memberRepository.findByOrganizationIdAndDeletedFalse(orgId).stream()
            .peek(member -> membershipLifecycleService.refreshMembership(member, reminderDays))
            .map(memberMapper::toResponse)
            .toList();
    }

    @Transactional
    public MemberResponse createMember(MemberRequest request) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        Organization organization = organizationRepository.findByIdAndDeletedFalse(orgId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        Member member = memberMapper.toNewEntity(request, organization);
        OrganizationSetting setting = organizationSettingService.getOrCreateSettings(orgId);
        membershipLifecycleService.refreshMembership(member, setting.getReminderDaysBeforeExpiry());
        member.setOverdueMarkedAt(null);
        member = memberRepository.save(member);
        notificationDispatcherService.dispatch(member, setting, NotificationType.WELCOME_MESSAGE);

        auditLogService.log(
            "MEMBER_CREATED",
            "Created member " + member.getFullName() + " for organization id=" + orgId,
            AuditCategory.MEMBER,
            "MEMBER"
        );
        return memberMapper.toResponse(member);
    }

    @Transactional
    public MemberResponse updateMember(Long memberId, MemberRequest request) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        memberMapper.apply(member, request);
        membershipLifecycleService.refreshMembership(
            member,
            organizationSettingService.getOrCreateSettings(orgId).getReminderDaysBeforeExpiry()
        );
        member.setOverdueMarkedAt(null);

        auditLogService.log(
            "MEMBER_UPDATED",
            "Updated member " + member.getFullName() + " (id=" + member.getId() + ")",
            AuditCategory.MEMBER,
            "MEMBER"
        );
        return memberMapper.toResponse(member);
    }

    @Transactional
    public void softDeleteMember(Long memberId) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        member.setDeleted(true);
        member.setDeletedAt(OffsetDateTime.now());

        auditLogService.log(
            "MEMBER_SOFT_DELETED",
            "Soft deleted member " + member.getFullName() + " (id=" + member.getId() + ")",
            AuditCategory.MEMBER,
            "MEMBER"
        );
    }

    private Long requireOrgId(AuthenticatedUser user) {
        if (user.organizationId() == null) {
            throw new BadRequestException("Organization context is required");
        }
        return user.organizationId();
    }
}
