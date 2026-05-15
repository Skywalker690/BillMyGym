package com.gymflow.service;

import com.gymflow.api.dto.MemberDtos;
import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.dto.MemberDtos.MemberResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Member;
import com.gymflow.domain.Organization;
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

    public List<MemberResponse> listMembers() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        Long orgId = requireOrgId(user);
        return memberRepository.findByOrganizationIdAndDeletedFalse(orgId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MemberResponse createMember(MemberRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        Long orgId = requireOrgId(user);

        Organization organization = organizationRepository.findByIdAndDeletedFalse(orgId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        Member member = new Member();
        member.setOrganization(organization);
        toMemberDto(request, member);

        return toResponse(memberRepository.save(member));
    }

    private void toMemberDto(MemberRequest request, Member member) {
        member.setFullName(request.fullName());
        member.setPhoneNumber(request.phoneNumber());
        member.setEmail(request.email());
        member.setJoiningDate(request.joiningDate());
        member.setMembershipStartDate(request.membershipStartDate());
        member.setMembershipEndDate(request.membershipEndDate());
        member.setMembershipStatus(request.membershipStatus());
        member.setNotes(request.notes());
    }

    @Transactional
    public MemberResponse updateMember(Long memberId, MemberRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        Long orgId = requireOrgId(user);

        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new NotFoundException("Member not found"));

        toMemberDto(request, member);

        return toResponse(member);
    }

    @Transactional
    public void softDeleteMember(Long memberId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        Long orgId = requireOrgId(user);

        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        member.setDeleted(true);
    }

    private Long requireOrgId(AuthenticatedUser user) {
        if (user.organizationId() == null) {
            throw new BadRequestException("Organization context is required");
        }
        return user.organizationId();
    }

    private MemberResponse toResponse(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getOrganization().getId(),
            member.getFullName(),
            member.getPhoneNumber(),
            member.getEmail(),
            member.getJoiningDate(),
            member.getMembershipStartDate(),
            member.getMembershipEndDate(),
            member.getMembershipStatus(),
            member.getNotes()
        );
    }
}
