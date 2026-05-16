package com.gymflow.service;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.dto.MemberDtos.CurrentMembershipSummary;
import com.gymflow.api.dto.MemberDtos.MemberDirectoryItem;
import com.gymflow.api.dto.MemberDtos.MemberDirectorySummary;
import com.gymflow.api.dto.MemberDtos.MemberPageResponse;
import com.gymflow.api.dto.MemberDtos.MemberResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.api.mapper.MemberMapper;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberMapper memberMapper;
    private final MembershipLifecycleService membershipLifecycleService;
    private final OrganizationSettingService organizationSettingService;
    private final AuditLogService auditLogService;

    @Transactional
    public MemberPageResponse listMembers(int page, int size, String search, String membershipStatus, MemberStatus memberStatus) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        int normalizedSize = Math.min(Math.max(size, 1), 40);
        int reminderDays = organizationSettingService.getOrCreateSettings(orgId).getReminderDaysBeforeExpiry();
        LocalDate today = LocalDate.now();
        String normalizedSearch = hasText(search) ? search.trim() : null;
        String normalizedMembershipStatus = hasText(membershipStatus) && !"ALL".equalsIgnoreCase(membershipStatus)
            ? membershipStatus.trim().toUpperCase()
            : null;
        Page<Member> memberPage = memberRepository.searchDirectory(
            orgId,
            normalizedSearch,
            memberStatus,
            normalizedMembershipStatus,
            today,
            today.plusDays(reminderDays),
            PageRequest.of(Math.max(page, 0), normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<MemberDirectoryItem> items = toDirectoryItems(memberPage.getContent(), reminderDays);
        return new MemberPageResponse(
            items,
            memberDirectorySummary(orgId, reminderDays),
            memberPage.getNumber(),
            memberPage.getSize(),
            memberPage.getTotalElements(),
            memberPage.getTotalPages(),
            memberPage.isFirst(),
            memberPage.isLast()
        );
    }

    @Transactional
    public MemberResponse getMember(Long memberId) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        return memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .map(memberMapper::toResponse)
            .orElseThrow(() -> new NotFoundException("Member not found"));
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public MemberResponse createMember(MemberRequest request) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        Organization organization = organizationRepository.findByIdAndDeletedFalse(orgId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        ensurePhoneUnique(orgId, request.phone(), null);

        Member member = memberMapper.toNewEntity(request, organization);
        applyMemberStatusSideEffects(member);
        member = memberRepository.save(member);

        auditLogService.log(
            "MEMBER_CREATED",
            "Created member " + member.getFullName() + " for organization id=" + orgId,
            AuditCategory.MEMBER,
            "MEMBER"
        );
        return memberMapper.toResponse(member);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public MemberResponse updateMember(Long memberId, MemberRequest request) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        ensurePhoneUnique(orgId, request.phone(), memberId);
        memberMapper.apply(member, request);
        applyMemberStatusSideEffects(member);

        auditLogService.log(
            "MEMBER_UPDATED",
            "Updated member " + member.getFullName() + " (id=" + member.getId() + ")",
            AuditCategory.MEMBER,
            "MEMBER"
        );
        return memberMapper.toResponse(member);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
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

    private void ensurePhoneUnique(Long organizationId, String phone, Long currentMemberId) {
        memberRepository.findByOrganizationIdAndPhoneAndDeletedFalse(organizationId, phone)
            .filter(existing -> !existing.getId().equals(currentMemberId))
            .ifPresent(existing -> {
                throw new BadRequestException("A member with this phone already exists in the organization");
            });
    }

    public List<MemberDirectoryItem> toDirectoryItems(Collection<Member> members, int reminderDays) {
        if (members.isEmpty()) {
            return List.of();
        }
        Long orgId = members.iterator().next().getOrganization().getId();
        List<Long> memberIds = members.stream().map(Member::getId).toList();
        Map<Long, MemberMembership> currentMembershipByMemberId = memberMembershipRepository
            .findByOrganizationIdAndMemberIdInAndDeletedFalseOrderByMemberIdAscStartDateDescCreatedAtDesc(orgId, memberIds)
            .stream()
            .collect(Collectors.groupingBy(membership -> membership.getMember().getId()))
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> selectCurrentMembership(entry.getValue())
            ));
        List<Long> membershipIds = currentMembershipByMemberId.values().stream()
            .filter(Objects::nonNull)
            .map(MemberMembership::getId)
            .toList();
        Map<Long, List<MemberPayment>> paymentsByMembershipId = membershipIds.isEmpty()
            ? Map.of()
            : memberPaymentRepository.findByMembershipIdInAndDeletedFalse(membershipIds).stream()
                .collect(Collectors.groupingBy(payment -> payment.getMembership().getId()));

        return members.stream()
            .map(member -> toDirectoryItem(
                member,
                currentMembershipByMemberId.get(member.getId()),
                paymentsByMembershipId,
                reminderDays
            ))
            .toList();
    }

    public MemberDirectorySummary memberDirectorySummary(Long orgId, int reminderDays) {
        LocalDate today = LocalDate.now();
        return new MemberDirectorySummary(
            memberRepository.countByOrganizationIdAndDeletedFalse(orgId),
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(orgId, MemberStatus.ACTIVE),
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(orgId, MemberStatus.DEACTIVATED),
            memberMembershipRepository.countByOrganizationIdAndDeletedFalseAndStatus(orgId, MembershipStatus.ACTIVE),
            memberMembershipRepository.countExpiringSoon(orgId, today, today.plusDays(reminderDays)),
            memberMembershipRepository.countByOrganizationIdAndDeletedFalseAndStatus(orgId, MembershipStatus.EXPIRED),
            memberMembershipRepository.countByOrganizationIdAndDeletedFalseAndStatus(orgId, MembershipStatus.OVERDUE)
        );
    }

    public MemberDirectoryItem toDirectoryItem(
        Member member,
        MemberMembership membership,
        Map<Long, List<MemberPayment>> paymentsByMembershipId,
        int reminderDays
    ) {
        return new MemberDirectoryItem(
            member.getId(),
            member.getOrganization().getId(),
            member.getFullName(),
            member.getPhone(),
            member.getEmail(),
            member.getStatus(),
            member.getNotes(),
            membership == null ? null : toCurrentMembershipSummary(
                membership,
                paymentsByMembershipId.getOrDefault(membership.getId(), List.of()),
                reminderDays
            )
        );
    }

    private CurrentMembershipSummary toCurrentMembershipSummary(
        MemberMembership membership,
        List<MemberPayment> payments,
        int reminderDays
    ) {
        BigDecimal paidAmount = membershipLifecycleService.successfulPaymentTotal(payments);
        BigDecimal pendingAmount = membership.getPlanAmount().subtract(paidAmount).max(BigDecimal.ZERO);
        PaymentStatus paymentStatus = membershipLifecycleService.resolveCoverageStatus(membership.getPlanAmount(), paidAmount);
        return new CurrentMembershipSummary(
            membership.getId(),
            membership.getPlanName(),
            membership.getDurationMonths(),
            membership.getPlanAmount(),
            membership.getStartDate(),
            membership.getEndDate(),
            membership.getStatus(),
            membershipLifecycleService.isExpiringSoon(LocalDate.now(), membership.getEndDate(), reminderDays),
            paymentStatus,
            paidAmount,
            pendingAmount
        );
    }

    private MemberMembership selectCurrentMembership(List<MemberMembership> memberships) {
        return memberships.stream()
            .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
            .findFirst()
            .orElseGet(() -> memberships.stream()
                .max(Comparator.comparing(MemberMembership::getEndDate))
                .orElse(null));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long requireOrgId(AuthenticatedUser user) {
        if (user.organizationId() == null) {
            throw new BadRequestException("Organization context is required");
        }
        return user.organizationId();
    }

    private void applyMemberStatusSideEffects(Member member) {
        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            if (member.getDeactivatedAt() == null) {
                member.setDeactivatedAt(OffsetDateTime.now());
            }
        } else {
            member.setDeactivatedAt(null);
            if (member.getStatus() != MemberStatus.DEACTIVATED) {
                member.setDeactivationReason(null);
            }
        }
    }
}
