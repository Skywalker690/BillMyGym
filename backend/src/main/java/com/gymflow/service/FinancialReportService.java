package com.gymflow.service;

import com.gymflow.api.dto.ReportDtos.FinancialReportMonthResponse;
import com.gymflow.api.dto.ReportDtos.FinancialReportResponse;
import com.gymflow.api.dto.ReportDtos.FinancialReportSummary;
import com.gymflow.api.dto.SubscriptionDtos.PlanAccessResponse;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.ForbiddenException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.MemberPayment;
import com.gymflow.domain.Organization;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
import com.gymflow.domain.enums.SubscriptionPlan;
import com.gymflow.repository.AuditLogRepository;
import com.gymflow.repository.MemberMembershipRepository;
import com.gymflow.repository.MemberPaymentRepository;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.security.SecurityUtils;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialReportService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final int MAX_REPORT_MONTHS = 36;
    private static final int GROWTH_REPORT_MONTHS = 6;

    private final OrganizationRepository organizationRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final AuditLogRepository auditLogRepository;
    private final PlanAccessService planAccessService;

    @Transactional(readOnly = true)
    public FinancialReportResponse financialReport(String period, String range, LocalDate from, LocalDate to) {
        Organization organization = currentOrganization();
        if (!planAccessService.canReceiveMonthlyReports(organization)) {
            throw new ForbiddenException("Upgrade to Pro to unlock this feature");
        }

        ReportWindow window = resolveWindow(organization, period, range, from, to);
        List<MemberPayment> payments = memberPaymentRepository
            .findByOrganizationIdAndDeletedFalseOrderByPaymentDateDescCreatedAtDesc(organization.getId());
        List<MemberMembership> memberships = memberMembershipRepository
            .findByOrganizationIdAndDeletedFalse(organization.getId());

        List<FinancialReportMonthResponse> months = monthsBetween(window.startDate(), window.endDate()).stream()
            .map(month -> monthResponse(organization, payments, memberships, month))
            .toList();

        return new FinancialReportResponse(
            window.period(),
            window.range(),
            window.startDate(),
            window.endDate(),
            organization.getName(),
            organization.getLogoUrl(),
            reportPlanName(organization),
            toPlanAccessResponse(planAccessService.summary(organization)),
            summary(months),
            months
        );
    }

    @Transactional(readOnly = true)
    public ReportExport export(String type, String period, String range, LocalDate from, LocalDate to) {
        Organization organization = currentOrganization();
        if (!planAccessService.canExportReports(organization)) {
            throw new ForbiddenException("Upgrade to Pro to unlock this feature");
        }

        FinancialReportResponse report = financialReport(period, range, from, to);
        String normalizedType = normalizeExportType(type);
        String extension = normalizedType.toLowerCase(Locale.ROOT);
        String filename = "financial-report-%s-%s.%s".formatted(
            report.startDate(),
            report.endDate(),
            extension
        );

        if ("CSV".equals(normalizedType)) {
            return new ReportExport(buildCsv(report), "text/csv; charset=UTF-8", filename);
        }
        return new ReportExport(buildPdf(report), "application/pdf", filename);
    }

    private FinancialReportMonthResponse monthResponse(
        Organization organization,
        List<MemberPayment> payments,
        List<MemberMembership> memberships,
        YearMonth month
    ) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDate snapshotDate = month.equals(YearMonth.from(today)) ? today : monthEnd;
        BigDecimal admissionRevenue = BigDecimal.ZERO;
        BigDecimal membershipRevenue = BigDecimal.ZERO;
        Set<Long> admittedMemberIds = new java.util.HashSet<>();

        for (MemberPayment payment : payments) {
            if (payment.getPaymentStatus() != PaymentStatus.PAID || payment.getPaymentDate() == null) {
                continue;
            }
            if (payment.getPaymentDate().isBefore(monthStart) || payment.getPaymentDate().isAfter(monthEnd)) {
                continue;
            }
            if (payment.getPaymentType() == PaymentType.ADMISSION) {
                admissionRevenue = admissionRevenue.add(payment.getAmount());
                if (payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    admittedMemberIds.add(payment.getMember().getId());
                }
            } else if (isMembershipPayment(payment)) {
                membershipRevenue = membershipRevenue.add(payment.getAmount());
            }
        }

        Map<Long, MemberMembership> latestByMember = latestMembershipsAsOf(memberships, snapshotDate);
        long activeMembers = latestByMember.values().stream()
            .filter(membership -> !membership.getEndDate().isBefore(snapshotDate))
            .count();
        long overdueMembers = latestByMember.values().stream()
            .filter(membership -> membership.getEndDate().isBefore(snapshotDate))
            .count();
        long deactivatedMembers = auditLogRepository.countByOrganizationIdAndEventBetween(
            organization.getId(),
            "MEMBER_DEACTIVATED",
            monthStart.atStartOfDay(APP_ZONE).toInstant(),
            month.plusMonths(1).atDay(1).atStartOfDay(APP_ZONE).toInstant()
        );
        BigDecimal monthlyFee = organization.getMonthlyMembershipFee() == null
            ? BigDecimal.ZERO
            : organization.getMonthlyMembershipFee();
        BigDecimal duePayments = month.equals(YearMonth.from(today))
            ? monthlyFee.multiply(BigDecimal.valueOf(overdueMembers))
            : BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        return new FinancialReportMonthResponse(
            month.getYear(),
            month.getMonthValue(),
            month.format(MONTH_LABEL),
            admissionRevenue.add(membershipRevenue),
            admissionRevenue,
            membershipRevenue,
            duePayments,
            expenses,
            admittedMemberIds.size(),
            activeMembers,
            overdueMembers,
            deactivatedMembers,
            "%s plan, monthly member fee INR %s".formatted(
                reportPlanName(organization),
                monthlyFee.stripTrailingZeros().toPlainString()
            )
        );
    }

    private Map<Long, MemberMembership> latestMembershipsAsOf(List<MemberMembership> memberships, LocalDate snapshotDate) {
        Map<Long, MemberMembership> latestByMember = new HashMap<>();
        for (MemberMembership membership : memberships) {
            if (membership.getMember().isDeleted() || !isMemberActiveAtSnapshot(membership.getMember(), snapshotDate)) {
                continue;
            }
            if (membership.getStartDate().isAfter(snapshotDate)) {
                continue;
            }
            latestByMember.merge(
                membership.getMember().getId(),
                membership,
                (left, right) -> latestMembershipComparator().compare(left, right) >= 0 ? left : right
            );
        }
        return latestByMember;
    }

    private Comparator<MemberMembership> latestMembershipComparator() {
        return Comparator.comparing(MemberMembership::getEndDate)
            .thenComparing(MemberMembership::getStartDate)
            .thenComparing(MemberMembership::getCreatedAt);
    }

    private FinancialReportSummary summary(List<FinancialReportMonthResponse> months) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal admissionRevenue = BigDecimal.ZERO;
        BigDecimal membershipRevenue = BigDecimal.ZERO;
        BigDecimal duePayments = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        long admissions = 0L;
        long deactivatedMembers = 0L;

        for (FinancialReportMonthResponse month : months) {
            totalRevenue = totalRevenue.add(month.totalRevenue());
            admissionRevenue = admissionRevenue.add(month.admissionRevenue());
            membershipRevenue = membershipRevenue.add(month.membershipRevenue());
            duePayments = duePayments.add(month.duePayments());
            expenses = expenses.add(month.expenses());
            admissions += month.admissions();
            deactivatedMembers += month.deactivatedMembers();
        }

        FinancialReportMonthResponse latest = months.isEmpty() ? null : months.get(months.size() - 1);
        return new FinancialReportSummary(
            totalRevenue,
            admissionRevenue,
            membershipRevenue,
            duePayments,
            expenses,
            admissions,
            latest == null ? 0 : latest.activeMembers(),
            latest == null ? 0 : latest.overdueMembers(),
            deactivatedMembers
        );
    }

    private boolean isMemberActiveAtSnapshot(com.gymflow.domain.Member member, LocalDate snapshotDate) {
        if (member.getCreatedAt() != null && member.getCreatedAt().toLocalDate().isAfter(snapshotDate)) {
            return false;
        }
        return member.getDeactivatedAt() == null || member.getDeactivatedAt().toLocalDate().isAfter(snapshotDate);
    }

    private ReportWindow resolveWindow(Organization organization, String period, String range, LocalDate from, LocalDate to) {
        SubscriptionPlan plan = planAccessService.resolvePlan(organization);
        if (plan == SubscriptionPlan.GROWTH) {
            return growthWindow();
        }

        String normalizedPeriod = normalizePeriod(period);
        String normalizedRange = normalizeRange(range, normalizedPeriod);
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDate startDate;
        LocalDate endDate;

        switch (normalizedRange) {
            case "CURRENT_MONTH" -> {
                YearMonth month = YearMonth.from(today);
                startDate = month.atDay(1);
                endDate = today;
            }
            case "LAST_MONTH" -> {
                YearMonth month = YearMonth.from(today).minusMonths(1);
                startDate = month.atDay(1);
                endDate = month.atEndOfMonth();
            }
            case "CURRENT_YEAR" -> {
                startDate = LocalDate.of(today.getYear(), 1, 1);
                endDate = today;
            }
            case "CUSTOM" -> {
                if (from == null || to == null) {
                    throw new BadRequestException("Custom reports require a start and end date");
                }
                startDate = from;
                endDate = to.isAfter(today) ? today : to;
            }
            default -> throw new BadRequestException("Unsupported report range");
        }

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Report start date must be on or before end date");
        }

        List<YearMonth> months = monthsBetween(startDate, endDate);
        if (months.size() > MAX_REPORT_MONTHS) {
            throw new BadRequestException("Report range cannot exceed 36 months");
        }

        return new ReportWindow(normalizedPeriod, normalizedRange, startDate, endDate);
    }

    private ReportWindow growthWindow() {
        LocalDate today = LocalDate.now(APP_ZONE);
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate startDate = currentMonth.minusMonths(GROWTH_REPORT_MONTHS - 1L).atDay(1);
        return new ReportWindow("MONTHLY", "LAST_SIX_MONTHS", startDate, today);
    }

    private List<YearMonth> monthsBetween(LocalDate startDate, LocalDate endDate) {
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "MONTHLY";
        }
        String normalized = period.trim().replace("-", "_").toUpperCase(Locale.ROOT);
        if (!"MONTHLY".equals(normalized) && !"YEARLY".equals(normalized)) {
            throw new BadRequestException("Unsupported report period");
        }
        return normalized;
    }

    private String normalizeRange(String range, String period) {
        if (range == null || range.isBlank()) {
            return "YEARLY".equals(period) ? "CURRENT_YEAR" : "CURRENT_MONTH";
        }
        String normalized = range.trim().replace("-", "_").toUpperCase(Locale.ROOT);
        if (!List.of("CURRENT_MONTH", "LAST_MONTH", "CURRENT_YEAR", "CUSTOM").contains(normalized)) {
            throw new BadRequestException("Unsupported report range");
        }
        return normalized;
    }

    private String normalizeExportType(String type) {
        if (type == null || type.isBlank()) {
            return "CSV";
        }
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if (!"CSV".equals(normalized) && !"PDF".equals(normalized)) {
            throw new BadRequestException("Unsupported export type");
        }
        return normalized;
    }

    private boolean isMembershipPayment(MemberPayment payment) {
        return payment.getPaymentType() == PaymentType.MEMBERSHIP
            || payment.getPaymentType() == PaymentType.NEW_MEMBERSHIP
            || payment.getPaymentType() == PaymentType.RENEWAL;
    }

    private String reportPlanName(Organization organization) {
        if (organization.getOrganizationType() == OrganizationType.DEMO) {
            return "DEMO";
        }
        return planAccessService.resolvePlan(organization).name();
    }

    private byte[] buildCsv(FinancialReportResponse report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, "Gym", report.organizationName());
        appendCsvRow(csv, "Period", report.period());
        appendCsvRow(csv, "Date Range", report.startDate() + " to " + report.endDate());
        appendCsvRow(csv, "Plan", report.planName());
        appendCsvRow(csv);
        appendCsvRow(
            csv,
            "Month",
            "Total Revenue",
            "Admission Revenue",
            "Membership Revenue",
            "Admissions",
            "Due Payments",
            "Expenses",
            "Active Members",
            "Overdue Members",
            "Deactivated Members"
        );
        for (FinancialReportMonthResponse month : report.months()) {
            appendCsvRow(
                csv,
                month.label(),
                moneyValue(month.totalRevenue()),
                moneyValue(month.admissionRevenue()),
                moneyValue(month.membershipRevenue()),
                String.valueOf(month.admissions()),
                moneyValue(month.duePayments()),
                moneyValue(month.expenses()),
                String.valueOf(month.activeMembers()),
                String.valueOf(month.overdueMembers()),
                String.valueOf(month.deactivatedMembers())
            );
        }
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private void appendCsvRow(StringBuilder csv, String... values) {
        csv.append(
            java.util.Arrays.stream(values)
                .map(this::escapeCsv)
                .collect(Collectors.joining(","))
        ).append("\r\n");
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n") || normalized.contains("\r")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private byte[] buildPdf(FinancialReportResponse report) {
        List<String> pageContents = buildPdfPages(report);
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder kids = new StringBuilder();
        int pageObjectId = 3;
        for (int index = 0; index < pageContents.size(); index++) {
            if (index > 0) {
                kids.append(' ');
            }
            kids.append(pageObjectId).append(" 0 R");
            pageObjectId += 2;
        }
        objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageContents.size() + " >>");

        int objectId = 3;
        for (String content : pageContents) {
            int contentId = objectId + 1;
            objects.add("""
                << /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> /F2 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >> >> >> /Contents %d 0 R >>
                """.formatted(contentId).trim());
            byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);
            objects.add("<< /Length " + contentBytes.length + " >>\nstream\n" + content + "\nendstream");
            objectId += 2;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(out.size());
            out.writeBytes(("%d 0 obj\n%s\nendobj\n".formatted(index + 1, objects.get(index))).getBytes(StandardCharsets.ISO_8859_1));
        }
        int xrefOffset = out.size();
        out.writeBytes(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
        out.writeBytes("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
        for (Integer offset : offsets) {
            out.writeBytes(("%010d 00000 n \n".formatted(offset)).getBytes(StandardCharsets.ISO_8859_1));
        }
        out.writeBytes(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    private List<String> buildPdfPages(FinancialReportResponse report) {
        List<String> pages = new ArrayList<>();
        List<FinancialReportMonthResponse> rows = report.months();
        List<int[]> ranges = new ArrayList<>();
        int cursor = 0;
        while (cursor < rows.size() || ranges.isEmpty()) {
            int rowsPerPage = ranges.isEmpty() ? 7 : 10;
            int end = Math.min(cursor + rowsPerPage, rows.size());
            ranges.add(new int[] {cursor, end});
            cursor = end;
            if (cursor >= rows.size()) {
                break;
            }
        }

        int pageCount = ranges.size();
        for (int page = 0; page < pageCount; page++) {
            StringBuilder content = new StringBuilder();
            drawPdfHeader(content, report, page + 1, pageCount);

            if (page == 0) {
                drawSummaryCard(content, 36, 458, "Total Revenue", moneyLabel(report.summary().totalRevenue()));
                drawSummaryCard(content, 232, 458, "Membership Revenue", moneyLabel(report.summary().membershipRevenue()));
                drawSummaryCard(content, 428, 458, "Admission Revenue", moneyLabel(report.summary().admissionRevenue()));
                drawSummaryCard(content, 624, 458, "Current Due", moneyLabel(report.summary().duePayments()));
                pdfText(content, "F2", 12, 36, 426, "Monthly Breakdown");
                drawTableHeader(content, 36, 396);
            } else {
                pdfText(content, "F2", 12, 36, 496, "Monthly Breakdown");
                drawTableHeader(content, 36, 466);
            }

            int[] range = ranges.get(page);
            int y = page == 0 ? 366 : 436;
            for (int index = range[0]; index < range[1]; index++) {
                FinancialReportMonthResponse row = rows.get(index);
                drawMonthRow(content, 36, y, row, index % 2 == 0);
                y -= 34;
            }
            pdfText(content, "F1", 9, 36, 34, "All amounts are in INR. Due amount is shown only for the current Indian month.");
            pages.add(content.toString());
        }
        return pages;
    }

    private void drawPdfHeader(StringBuilder content, FinancialReportResponse report, int page, int pageCount) {
        content.append("0.97 0.99 0.98 rg 0 0 842 595 re f\n");
        content.append("0.90 0.96 0.93 rg 0 535 842 60 re f\n");
        pdfText(content, "F2", 20, 36, 560, "Financial Report");
        pdfText(content, "F1", 10, 36, 542, safePdfText(report.organizationName(), 86) + " | " + report.startDate() + " to " + report.endDate());
        pdfText(content, "F1", 9, 666, 560, "Generated " + OffsetDateTime.now(APP_ZONE).toLocalDate());
        pdfText(content, "F1", 9, 742, 542, "Page " + page + " of " + pageCount);
    }

    private void drawSummaryCard(StringBuilder content, int x, int y, String label, String value) {
        content.append("1 1 1 rg ").append(x).append(' ').append(y).append(" 178 54 re f\n");
        content.append("0.83 0.89 0.86 RG ").append(x).append(' ').append(y).append(" 178 54 re S\n");
        pdfText(content, "F1", 8, x + 10, y + 33, label);
        pdfText(content, "F2", 12, x + 10, y + 15, safePdfText(value, 24));
    }

    private void drawTableHeader(StringBuilder content, int x, int y) {
        content.append("0.90 0.95 0.92 rg ").append(x).append(' ').append(y).append(" 770 24 re f\n");
        content.append("0.74 0.82 0.78 RG ").append(x).append(' ').append(y).append(" 770 24 re S\n");
        pdfText(content, "F2", 8, x + 10, y + 8, "Month");
        pdfText(content, "F2", 8, x + 110, y + 8, "Total");
        pdfText(content, "F2", 8, x + 210, y + 8, "Membership");
        pdfText(content, "F2", 8, x + 330, y + 8, "Admission");
        pdfText(content, "F2", 8, x + 450, y + 8, "Due");
        pdfText(content, "F2", 8, x + 560, y + 8, "Admissions");
        pdfText(content, "F2", 8, x + 642, y + 8, "Active");
        pdfText(content, "F2", 8, x + 708, y + 8, "Overdue");
    }

    private void drawMonthRow(StringBuilder content, int x, int y, FinancialReportMonthResponse row, boolean shaded) {
        content.append(shaded ? "1 1 1 rg " : "0.96 0.98 0.97 rg ")
            .append(x)
            .append(' ')
            .append(y)
            .append(" 770 30 re f\n");
        content.append("0.88 0.91 0.89 RG ").append(x).append(' ').append(y).append(" 770 30 re S\n");
        pdfText(content, "F2", 8, x + 10, y + 11, row.label());
        pdfText(content, "F1", 8, x + 110, y + 11, safePdfText(moneyLabel(row.totalRevenue()), 16));
        pdfText(content, "F1", 8, x + 210, y + 11, safePdfText(moneyLabel(row.membershipRevenue()), 16));
        pdfText(content, "F1", 8, x + 330, y + 11, safePdfText(moneyLabel(row.admissionRevenue()), 16));
        pdfText(content, "F1", 8, x + 450, y + 11, safePdfText(moneyLabel(row.duePayments()), 16));
        pdfText(content, "F1", 8, x + 560, y + 11, String.valueOf(row.admissions()));
        pdfText(content, "F1", 8, x + 642, y + 11, String.valueOf(row.activeMembers()));
        pdfText(content, "F1", 8, x + 708, y + 11, String.valueOf(row.overdueMembers()));
    }

    private void pdfText(StringBuilder content, String font, int size, int x, int y, String text) {
        content.append("0.05 0.07 0.12 rg\nBT /")
            .append(font)
            .append(' ')
            .append(size)
            .append(" Tf ")
            .append(x)
            .append(' ')
            .append(y)
            .append(" Td (")
            .append(escapePdf(text))
            .append(") Tj ET\n");
    }

    private String escapePdf(String value) {
        return (value == null ? "" : value)
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)");
    }

    private String safePdfText(String value, int maxLength) {
        String normalized = value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", " ");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String moneyLabel(BigDecimal value) {
        return "INR " + moneyValue(value);
    }

    private String moneyValue(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private PlanAccessResponse toPlanAccessResponse(PlanAccessService.PlanAccessSummary summary) {
        return new PlanAccessResponse(
            summary.canCreateMembers(),
            summary.canViewAdvancedAnalytics(),
            summary.canReceiveMonthlyReports(),
            summary.canExportReports(),
            summary.canUsePremiumFeatures()
        );
    }

    private Organization currentOrganization() {
        Long organizationId = SecurityUtils.currentUser().organizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required");
        }
        return organizationRepository.findByIdAndDeletedFalse(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private record ReportWindow(String period, String range, LocalDate startDate, LocalDate endDate) {
    }

    public record ReportExport(byte[] content, String contentType, String filename) {
    }
}
