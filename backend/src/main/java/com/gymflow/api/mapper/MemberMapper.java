package com.gymflow.api.mapper;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.dto.MemberDtos.MemberResponse;
import com.gymflow.domain.Member;
import com.gymflow.domain.Organization;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public MemberResponse toResponse(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getOrganization().getId(),
            member.getFullName(),
            member.getPhoneNumber(),
            member.getEmail(),
            member.getJoiningDate(),
            member.getMembershipDuration(),
            member.getCustomDurationDays(),
            member.getMembershipStartDate(),
            member.getMembershipEndDate(),
            member.getMembershipStatus(),
            member.getNotes()
        );
    }

    public Member toNewEntity(MemberRequest request, Organization organization) {
        Member member = new Member();
        member.setOrganization(organization);
        apply(member, request);
        return member;
    }

    public void apply(Member member, MemberRequest request) {
        member.setFullName(request.fullName());
        member.setPhoneNumber(request.phoneNumber());
        member.setEmail(request.email());
        member.setJoiningDate(request.joiningDate());
        member.setMembershipDuration(request.membershipDuration());
        member.setCustomDurationDays(request.customDurationDays());
        member.setNotes(request.notes());
    }
}
