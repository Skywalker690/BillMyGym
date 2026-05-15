package com.gymflow.service;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.dto.MemberDtos.MemberResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.api.mapper.MemberMapper;
import com.gymflow.domain.Member;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberMapper memberMapper;
    private final AuditLogService auditLogService;

    public List<MemberResponse> listMembers() {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        return memberRepository.findByOrganizationIdAndDeletedFalse(orgId).stream().map(memberMapper::toResponse).toList();
    }

    @Transactional
    public MemberResponse createMember(MemberRequest request) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        Organization organization = organizationRepository.findByIdAndDeletedFalse(orgId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        Member member = memberRepository.save(memberMapper.toNewEntity(request, organization));

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
