package com.gymflow.service;

import com.gymflow.api.dto.MemberDtos.MemberRequest;
import com.gymflow.api.dto.MemberDtos.CurrentMembershipSummary;
import com.gymflow.api.dto.MemberDtos.BulkMemberImportIssue;
import com.gymflow.api.dto.MemberDtos.BulkMemberImportResponse;
import com.gymflow.api.dto.MemberDtos.MemberDirectoryItem;
import com.gymflow.api.dto.MemberDtos.MemberDirectorySummary;
import com.gymflow.api.dto.MemberDtos.MemberPageResponse;
import com.gymflow.api.dto.MemberDtos.MemberResponse;
import com.gymflow.api.dto.SubscriptionDtos.MemberLimitResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.api.mapper.MemberMapper;
import com.gymflow.domain.Member;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.domain.enums.AuditActivityScope;
import com.gymflow.domain.enums.AuditEventStatus;
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.MembershipStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.Role;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.MemberRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DUE_TODAY_WINDOW_DAYS = 0;
    private static final int BULK_IMPORT_LIMIT = 200;
    private static final Set<MemberStatus> INACTIVE_MEMBER_STATUSES = Set.of(
        MemberStatus.DEACTIVATED
    );

    private final MemberRepository memberRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final OrganizationRepository organizationRepository;
    private final MemberMapper memberMapper;
    private final MembershipLifecycleService membershipLifecycleService;
    private final MemberMembershipService memberMembershipService;
    private final MemberCreditLedgerService memberCreditLedgerService;
    private final OrganizationSettingService organizationSettingService;
    private final MemberLimitService memberLimitService;
    private final AuditLogService auditLogService;

    @Transactional
    public MemberPageResponse listMembers(int page, int size, String search, String membershipStatus, MemberStatus memberStatus) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        int normalizedSize = Math.min(Math.max(size, 1), 40);
        LocalDate today = LocalDate.now(APP_ZONE);
        memberMembershipService.refreshMembershipStatuses(orgId);
        String searchPattern = hasText(search) ? "%" + search.trim().toLowerCase() + "%" : null;
        String normalizedMembershipStatus = hasText(membershipStatus) && !"ALL".equalsIgnoreCase(membershipStatus)
            ? membershipStatus.trim().toUpperCase()
            : null;
        PageRequest pageRequest = PageRequest.of(
            Math.max(page, 0),
            normalizedSize,
            Sort.by(Sort.Direction.ASC, "status").and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Page<Member> memberPage = memberStatus == MemberStatus.DEACTIVATED
            ? memberRepository.searchDirectoryByStatuses(
                orgId,
                searchPattern,
                INACTIVE_MEMBER_STATUSES,
                normalizedMembershipStatus,
                today,
                pageRequest
            )
            : memberRepository.searchDirectory(
                orgId,
                searchPattern,
                memberStatus,
                normalizedMembershipStatus,
                today,
                pageRequest
            );
        List<MemberDirectoryItem> items = toDirectoryItems(memberPage.getContent(), DUE_TODAY_WINDOW_DAYS);
        return new MemberPageResponse(
            items,
            memberDirectorySummary(orgId, DUE_TODAY_WINDOW_DAYS),
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
        memberLimitService.validateCanCreateMember(
            organization,
            request.status() == null ? MemberStatus.ACTIVE : request.status()
        );

        Member member = memberMapper.toNewEntity(request, organization);
        applyMemberStatusSideEffects(member);
        member = memberRepository.save(member);

        auditLogService.log(
            "MEMBER_CREATED",
            "Created member " + member.getFullName() + " for organization id=" + orgId,
            AuditCategory.MEMBER,
            "MEMBER"
        );
        auditLogService.logOrganizationActivity(
            "ADMIN_CREATED_MEMBER",
            "Admin created member " + member.getFullName(),
            AuditActivityScope.ADMIN_ACTIVITY,
            AuditEventStatus.INFO,
            "MEMBER",
            orgId,
            member.getFullName()
        );
        return memberMapper.toResponse(member);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public MemberResponse updateMember(Long memberId, MemberRequest request) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        memberLimitService.validateCanActivateMember(
            member.getOrganization(),
            member.getStatus(),
            request.status() == null ? MemberStatus.ACTIVE : request.status()
        );
        MemberStatus previousStatus = member.getStatus();
        memberMapper.apply(member, request);
        applyMemberStatusSideEffects(member);
        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            memberMembershipService.deactivateLatestMembershipForMember(member);
        }

        auditLogService.log(
            "MEMBER_UPDATED",
            "Updated member " + member.getFullName() + " (id=" + member.getId() + ")",
            AuditCategory.MEMBER,
            "MEMBER"
        );
        auditLogService.logOrganizationActivity(
            "ADMIN_UPDATED_MEMBER",
            "Admin updated member " + member.getFullName(),
            AuditActivityScope.ADMIN_ACTIVITY,
            AuditEventStatus.INFO,
            "MEMBER",
            orgId,
            member.getFullName()
        );
        if (previousStatus != member.getStatus()) {
            String statusEvent = member.getStatus() == MemberStatus.DEACTIVATED
                ? "MEMBER_DEACTIVATED"
                : previousStatus == MemberStatus.DEACTIVATED && member.getStatus() == MemberStatus.ACTIVE
                    ? "MEMBER_REACTIVATED"
                    : "MEMBER_STATUS_CHANGED";
            auditLogService.logOrganizationActivity(
                statusEvent,
                "Member status changed for " + member.getFullName() + " from " + previousStatus + " to " + member.getStatus(),
                AuditActivityScope.MEMBER_STATUS_ACTIVITY,
                AuditEventStatus.INFO,
                "MEMBER",
                orgId,
                member.getFullName()
            );
        }
        return memberMapper.toResponse(member);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public BulkMemberImportResponse bulkImportMembers(Long organizationId, MultipartFile file) {
        SecurityUtils.requireRole(Role.SUPER_ADMIN);
        Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        List<BulkMemberImportIssue> issues = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            issues.add(new BulkMemberImportIssue(null, "file", "Upload a CSV file with member full name and phone number."));
            return new BulkMemberImportResponse(0, 0, BULK_IMPORT_LIMIT, issues);
        }

        String content;
        try {
            content = stripBom(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the uploaded CSV file");
        }

        CsvImportParseResult parseResult = parseImportRows(content);
        issues.addAll(parseResult.issues());

        if (parseResult.totalRows() == 0) {
            issues.add(new BulkMemberImportIssue(null, "file", "The CSV does not contain any member rows."));
            return new BulkMemberImportResponse(0, 0, BULK_IMPORT_LIMIT, issues);
        }
        if (parseResult.totalRows() > BULK_IMPORT_LIMIT) {
            issues.add(new BulkMemberImportIssue(
                null,
                "file",
                "Only 200 members can be imported at a time. Your file has " + parseResult.totalRows() + " rows."
            ));
        }

        Map<String, List<Integer>> duplicateRowsByPhone = new HashMap<>();
        for (ParsedImportRow row : parseResult.rows()) {
            if (row.fullName() == null || row.fullName().isBlank()) {
                issues.add(new BulkMemberImportIssue(row.rowNumber(), "fullName", "Full name is required."));
            }
            if (row.phone() == null || row.phone().isBlank()) {
                issues.add(new BulkMemberImportIssue(row.rowNumber(), "phone", "Phone number is required for bulk import."));
                continue;
            }
            duplicateRowsByPhone.computeIfAbsent(row.normalizedPhone(), ignored -> new ArrayList<>()).add(row.rowNumber());
        }

        duplicateRowsByPhone.forEach((phone, rowNumbers) -> {
            if (rowNumbers.size() <= 1) {
                return;
            }
            rowNumbers.forEach(rowNumber -> issues.add(new BulkMemberImportIssue(
                rowNumber,
                "phone",
                "Phone number is repeated inside this CSV upload."
            )));
        });

        Set<String> normalizedExistingPhones = memberRepository.findPhonesByOrganizationIdAndDeletedFalse(organizationId).stream()
            .map(this::normalizePhone)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        for (ParsedImportRow row : parseResult.rows()) {
            if (row.normalizedPhone() != null && normalizedExistingPhones.contains(row.normalizedPhone())) {
                issues.add(new BulkMemberImportIssue(
                    row.rowNumber(),
                    "phone",
                    "Phone number already exists for this gym."
                ));
            }
        }

        MemberLimitService.MemberLimitSummary usage = memberLimitService.usage(organization);
        if (organization.getOrganizationType() == OrganizationType.DEMO
            && !usage.unlimited()
            && usage.usedMembers() + parseResult.rows().size() > usage.memberLimit()) {
            issues.add(new BulkMemberImportIssue(
                null,
                "file",
                "This import would exceed the demo member sandbox limit. Available slots: "
                    + Math.max(usage.remainingSlots(), 0)
                    + "."
            ));
        }

        if (!issues.isEmpty()) {
            return new BulkMemberImportResponse(parseResult.totalRows(), 0, BULK_IMPORT_LIMIT, sortIssues(issues));
        }

        List<Member> members = parseResult.rows().stream()
            .map(row -> toImportedMember(row, organization))
            .toList();
        memberRepository.saveAll(members);

        auditLogService.log(
            "MEMBER_BULK_IMPORTED",
            "Bulk imported " + members.size() + " members for organization id=" + organizationId,
            AuditCategory.MEMBER,
            "MEMBER"
        );
        auditLogService.logOrganizationActivity(
            "ADMIN_BULK_IMPORTED_MEMBERS",
            "Bulk imported " + members.size() + " members",
            AuditActivityScope.ADMIN_ACTIVITY,
            AuditEventStatus.INFO,
            "MEMBER",
            organizationId,
            organization.getName()
        );

        return new BulkMemberImportResponse(parseResult.totalRows(), members.size(), BULK_IMPORT_LIMIT, List.of());
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public void softDeleteMember(Long memberId) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        Member member = memberRepository.findByIdAndOrganizationIdAndDeletedFalse(memberId, orgId)
            .orElseThrow(() -> new NotFoundException("Member not found"));
        member.setDeleted(true);
        member.setDeletedAt(OffsetDateTime.now(APP_ZONE));

        auditLogService.log(
            "MEMBER_SOFT_DELETED",
            "Soft deleted member " + member.getFullName() + " (id=" + member.getId() + ")",
            AuditCategory.MEMBER,
            "MEMBER"
        );
        auditLogService.logOrganizationActivity(
            "ADMIN_DELETED_MEMBER",
            "Admin deleted member " + member.getFullName(),
            AuditActivityScope.ADMIN_ACTIVITY,
            AuditEventStatus.INFO,
            "MEMBER",
            orgId,
            member.getFullName()
        );
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
        Map<Long, BigDecimal> walletBalanceByMemberId = memberCreditLedgerService.balancesForMembers(orgId, memberIds);
        Map<Long, BigDecimal> appliedCreditByMembershipId = membershipIds.isEmpty()
            ? Map.of()
            : memberCreditLedgerService.appliedCreditForMemberships(membershipIds);
        Map<Long, List<MemberPayment>> paymentsByMembershipId = membershipIds.isEmpty()
            ? Map.of()
            : memberPaymentRepository.findByMembershipIdInAndDeletedFalse(membershipIds).stream()
                .collect(Collectors.groupingBy(payment -> payment.getMembership().getId()));

        return members.stream()
            .map(member -> toDirectoryItem(
                member,
                currentMembershipByMemberId.get(member.getId()),
                walletBalanceByMemberId.getOrDefault(member.getId(), BigDecimal.ZERO),
                appliedCreditByMembershipId,
                paymentsByMembershipId,
                reminderDays
            ))
            .toList();
    }

    public MemberDirectorySummary memberDirectorySummary(Long orgId, int reminderDays) {
        LocalDate today = LocalDate.now(APP_ZONE);
        return new MemberDirectorySummary(
            memberRepository.countByOrganizationIdAndDeletedFalse(orgId),
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatus(orgId, MemberStatus.ACTIVE),
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatusIn(orgId, INACTIVE_MEMBER_STATUSES),
            memberMembershipRepository.countByOrganizationIdAndDeletedFalseAndStatus(orgId, MembershipStatus.ACTIVE),
            memberMembershipRepository.countExpiringSoon(orgId, today),
            memberMembershipRepository.countExpiredOn(orgId, today),
            memberMembershipRepository.countOverdueBefore(orgId, today),
            toMemberLimitResponse(memberLimitService.usage(
                organizationRepository.findByIdAndDeletedFalse(orgId)
                    .orElseThrow(() -> new NotFoundException("Organization not found"))
            ))
        );
    }

    private MemberLimitResponse toMemberLimitResponse(MemberLimitService.MemberLimitSummary summary) {
        Long orgId = requireOrgId(SecurityUtils.currentUser());
        return new MemberLimitResponse(
            summary.plan(),
            summary.usedMembers(),
            memberRepository.countByOrganizationIdAndDeletedFalseAndStatusIn(orgId, INACTIVE_MEMBER_STATUSES),
            summary.memberLimit(),
            summary.unlimited(),
            summary.remainingSlots(),
            summary.usagePercent()
        );
    }

    public MemberDirectoryItem toDirectoryItem(
        Member member,
        MemberMembership membership,
        BigDecimal walletBalance,
        Map<Long, BigDecimal> appliedCreditByMembershipId,
        Map<Long, List<MemberPayment>> paymentsByMembershipId,
        int reminderDays
    ) {
        return new MemberDirectoryItem(
            member.getId(),
            member.getOrganization().getId(),
            member.getFullName(),
            member.getPhone(),
            member.getStatus(),
            membership == null ? null : toCurrentMembershipSummary(
                membership,
                paymentsByMembershipId.getOrDefault(membership.getId(), List.of()),
                appliedCreditByMembershipId.getOrDefault(membership.getId(), BigDecimal.ZERO),
                walletBalance,
                reminderDays
            )
        );
    }

    private CurrentMembershipSummary toCurrentMembershipSummary(
        MemberMembership membership,
        List<MemberPayment> payments,
        BigDecimal appliedCreditAmount,
        BigDecimal walletBalance,
        int reminderDays
    ) {
        BigDecimal paidAmount = membershipLifecycleService.successfulPaymentTotal(payments);
        BigDecimal pendingAmount = membership.getPlanAmount().subtract(paidAmount.add(appliedCreditAmount)).max(BigDecimal.ZERO);
        PaymentStatus paymentStatus = membershipLifecycleService.resolveCoverageStatus(
            membership.getPlanAmount(),
            paidAmount.add(appliedCreditAmount)
        );
        return new CurrentMembershipSummary(
            membership.getId(),
            membership.getDurationMonths(),
            membership.getPlanAmount(),
            membership.getStartDate(),
            membership.getEndDate(),
            membership.getStatus(),
            membershipLifecycleService.isExpiringSoon(LocalDate.now(APP_ZONE), membership.getEndDate(), reminderDays),
            paymentStatus,
            paidAmount,
            appliedCreditAmount,
            pendingAmount,
            walletBalance
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

    private List<BulkMemberImportIssue> sortIssues(List<BulkMemberImportIssue> issues) {
        return issues.stream()
            .sorted(Comparator
                .comparing((BulkMemberImportIssue issue) -> issue.rowNumber() == null ? Integer.MIN_VALUE : issue.rowNumber())
                .thenComparing(BulkMemberImportIssue::field)
                .thenComparing(BulkMemberImportIssue::message))
            .toList();
    }

    private Member toImportedMember(ParsedImportRow row, Organization organization) {
        Member member = new Member();
        member.setOrganization(organization);
        member.setFullName(row.fullName());
        member.setPhone(row.phone());
        member.setStatus(MemberStatus.ACTIVE);
        applyMemberStatusSideEffects(member);
        return member;
    }

    private CsvImportParseResult parseImportRows(String content) {
        List<ParsedImportRow> rows = new ArrayList<>();
        List<BulkMemberImportIssue> issues = new ArrayList<>();
        int totalRows = 0;
        boolean headerSkipped = false;

        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String rawLine = lines[index];
            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }

            CsvLineParseResult parsedLine = parseCsvLine(rawLine);
            if (parsedLine.hasUnmatchedQuote()) {
                totalRows++;
                issues.add(new BulkMemberImportIssue(index + 1, "row", "CSV row contains unmatched quotes."));
                continue;
            }

            if (!headerSkipped && rows.isEmpty() && looksLikeHeader(parsedLine.columns())) {
                headerSkipped = true;
                continue;
            }

            totalRows++;
            if (parsedLine.columns().size() != 2) {
                issues.add(new BulkMemberImportIssue(
                    index + 1,
                    "row",
                    "Each row must contain exactly 2 columns: full name and phone number."
                ));
                continue;
            }

            String fullName = normalizeFullName(parsedLine.columns().get(0));
            String phone = normalizePhone(parsedLine.columns().get(1));
            rows.add(new ParsedImportRow(index + 1, fullName, phone));
        }

        return new CsvImportParseResult(rows, issues, totalRows);
    }

    private CsvLineParseResult parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (character == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        columns.add(current.toString());
        return new CsvLineParseResult(columns, inQuotes);
    }

    private boolean looksLikeHeader(List<String> columns) {
        if (columns.size() != 2) {
            return false;
        }
        String first = normalizeHeader(columns.get(0));
        String second = normalizeHeader(columns.get(1));
        return Set.of("fullname", "membername", "name").contains(first)
            && Set.of("phone", "phonenumber", "mobilenumber", "mobile").contains(second);
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase().replaceAll("[\\s_\\-]+", "");
    }

    private String normalizeFullName(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", "");
        return trimmed.isBlank() ? null : trimmed;
    }

    private String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
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
                member.setDeactivatedAt(OffsetDateTime.now(APP_ZONE));
            }
        } else {
            member.setDeactivatedAt(null);
            if (member.getStatus() != MemberStatus.DEACTIVATED) {
                member.setDeactivationReason(null);
            }
        }
    }

    private record ParsedImportRow(
        int rowNumber,
        String fullName,
        String phone
    ) {
        private String normalizedPhone() {
            return phone;
        }
    }

    private record CsvLineParseResult(
        List<String> columns,
        boolean hasUnmatchedQuote
    ) {
    }

    private record CsvImportParseResult(
        List<ParsedImportRow> rows,
        List<BulkMemberImportIssue> issues,
        int totalRows
    ) {
    }
}
