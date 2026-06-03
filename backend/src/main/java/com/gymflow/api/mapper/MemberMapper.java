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
            member.getStatus(),
            member.getDeactivatedAt(),
            member.getDeactivationReason(),
            member.isDeleted(),
            member.getDeletedAt()
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
        member.setPhone(normalizePhone(request.phone()));
        member.setStatus(request.status() == null ? MemberStatus.ACTIVE : request.status());
        member.setDeactivationReason(request.deactivationReason());
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
