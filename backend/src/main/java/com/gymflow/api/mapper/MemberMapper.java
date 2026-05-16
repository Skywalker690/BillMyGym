package com.gymflow.api.mapper;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.dto.MemberDtos.MemberResponse;
import com.gymflow.domain.Member;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.MemberStatus;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public MemberResponse toResponse(Member member) {
        return new MemberResponse(
            member.getId(),
            member.getOrganization().getId(),
            member.getFullName(),
            member.getPhone(),
            member.getEmail(),
            member.getStatus(),
            member.getDeactivatedAt(),
            member.getDeactivationReason(),
            member.isDeleted(),
            member.getDeletedAt(),
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
        member.setPhone(request.phone());
        member.setEmail(request.email());
        member.setStatus(request.status() == null ? MemberStatus.ACTIVE : request.status());
        member.setDeactivationReason(request.deactivationReason());
        member.setNotes(request.notes());
    }
}
