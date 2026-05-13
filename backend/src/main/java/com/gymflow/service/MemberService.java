package com.gymflow.service;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.dto.MemberDtos.MemberResponse;
import com.gymflow.domain.Member;
import com.gymflow.domain.Organization;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final OrganizationRepository organizationRepository;

    public MemberService(MemberRepository memberRepository, OrganizationRepository organizationRepository) {
        this.memberRepository = memberRepository;
        this.organizationRepository = organizationRepository;
    }

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
            .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Member member = new Member();
        member.setOrganization(organization);
        member.setFullName(request.fullName());
        member.setPhoneNumber(request.phoneNumber());
        member.setEmail(request.email());
        member.setJoiningDate(request.joiningDate());
        member.setMembershipStartDate(request.membershipStartDate());
        member.setMembershipEndDate(request.membershipEndDate());
        member.setMembershipStatus(request.membershipStatus());
        member.setNotes(request.notes());

        return toResponse(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse updateMember(Long memberId, MemberRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        Long orgId = requireOrgId(user);

        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setFullName(request.fullName());
        member.setPhoneNumber(request.phoneNumber());
        member.setEmail(request.email());
        member.setJoiningDate(request.joiningDate());
        member.setMembershipStartDate(request.membershipStartDate());
        member.setMembershipEndDate(request.membershipEndDate());
        member.setMembershipStatus(request.membershipStatus());
        member.setNotes(request.notes());

        return toResponse(member);
    }

    @Transactional
    public void softDeleteMember(Long memberId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        Long orgId = requireOrgId(user);

        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setDeleted(true);
    }

    private Long requireOrgId(AuthenticatedUser user) {
        if (user.organizationId() == null) {
            throw new IllegalArgumentException("Organization context is required");
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
