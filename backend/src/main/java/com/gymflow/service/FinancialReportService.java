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
import com.gymflow.domain.enums.MemberStatus;
import com.gymflow.domain.enums.OrganizationType;
import com.gymflow.domain.enums.PaymentStatus;
import com.gymflow.domain.enums.PaymentType;
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

    private final OrganizationRepository organizationRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MemberMembershipRepository memberMembershipRepository;
    private final PlanAccessService planAccessService;

    @Transactional(readOnly = true)
    public FinancialReportResponse financialReport(String period, String range, LocalDate from, LocalDate to) {
        Organization organization = currentOrganization();
        if (!planAccessService.canReceiveMonthlyReports(organization)) {
            throw new ForbiddenException("Upgrade to Pro to unlock this feature");
        }

        ReportWindow window = resolveWindow(period, range, from, to);
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
        BigDecimal monthlyFee = organization.getMonthlyMembershipFee() == null
            ? BigDecimal.ZERO
            : organization.getMonthlyMembershipFee();
        BigDecimal duePayments = monthlyFee.multiply(BigDecimal.valueOf(overdueMembers));
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
            "%s plan, monthly member fee INR %s".formatted(
                reportPlanName(organization),
                monthlyFee.stripTrailingZeros().toPlainString()
            )
        );
    }

    private Map<Long, MemberMembership> latestMembershipsAsOf(List<MemberMembership> memberships, LocalDate snapshotDate) {
        Map<Long, MemberMembership> latestByMember = new HashMap<>();
        for (MemberMembership membership : memberships) {
            if (membership.getMember().isDeleted() || membership.getMember().getStatus() != MemberStatus.ACTIVE) {
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

        for (FinancialReportMonthResponse month : months) {
            totalRevenue = totalRevenue.add(month.totalRevenue());
            admissionRevenue = admissionRevenue.add(month.admissionRevenue());
            membershipRevenue = membershipRevenue.add(month.membershipRevenue());
            duePayments = duePayments.add(month.duePayments());
            expenses = expenses.add(month.expenses());
            admissions += month.admissions();
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
            latest == null ? 0 : latest.overdueMembers()
        );
    }

    private ReportWindow resolveWindow(String period, String range, LocalDate from, LocalDate to) {
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
                endDate = to;
            }
            default -> throw new BadRequestException("Unsupported report range");
        }

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Report start date must be before end date");
        }

        List<YearMonth> months = monthsBetween(startDate, endDate);
        if (months.size() > MAX_REPORT_MONTHS) {
            throw new BadRequestException("Report range cannot exceed 36 months");
        }

        return new ReportWindow(normalizedPeriod, normalizedRange, startDate, endDate);
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
            "Plan Details"
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
                month.planDetails()
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
                << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> /F2 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >> >> >> /Contents %d 0 R >>
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
        int rowsPerPage = 18;
        int pageCount = Math.max(1, (int) Math.ceil(rows.size() / (double) rowsPerPage));
        for (int page = 0; page < pageCount; page++) {
            StringBuilder content = new StringBuilder();
            pdfText(content, "F2", 21, 42, 792, "Financial Report");
            pdfText(content, "F1", 10, 42, 774, report.organizationName() + " | " + report.startDate() + " to " + report.endDate());
            if (report.logoUrl() != null && !report.logoUrl().isBlank()) {
                pdfText(content, "F1", 9, 42, 758, "Logo: " + report.logoUrl());
            }
            pdfText(content, "F1", 9, 420, 792, "Generated " + OffsetDateTime.now(APP_ZONE).toLocalDate());

            if (page == 0) {
                drawSummaryCard(content, 42, 700, "Total Revenue", moneyLabel(report.summary().totalRevenue()));
                drawSummaryCard(content, 182, 700, "Membership", moneyLabel(report.summary().membershipRevenue()));
                drawSummaryCard(content, 322, 700, "Admissions", String.valueOf(report.summary().admissions()));
                drawSummaryCard(content, 462, 700, "Due Payments", moneyLabel(report.summary().duePayments()));
                pdfText(content, "F2", 12, 42, 660, "Monthly Breakdown");
                drawTableHeader(content, 42, 635);
            } else {
                pdfText(content, "F2", 12, 42, 706, "Monthly Breakdown");
                drawTableHeader(content, 42, 681);
            }

            int start = page * rowsPerPage;
            int end = Math.min(start + rowsPerPage, rows.size());
            int y = page == 0 ? 612 : 658;
            for (int index = start; index < end; index++) {
                FinancialReportMonthResponse row = rows.get(index);
                drawTableRow(content, 42, y, row);
                y -= 28;
            }
            pdfText(content, "F1", 9, 42, 38, "Page " + (page + 1) + " of " + pageCount);
            pages.add(content.toString());
        }
        return pages;
    }

    private void drawSummaryCard(StringBuilder content, int x, int y, String label, String value) {
        content.append("0.94 0.98 0.96 rg ").append(x).append(' ').append(y).append(" 118 48 re f\n");
        content.append("0.75 0.86 0.82 RG ").append(x).append(' ').append(y).append(" 118 48 re S\n");
        pdfText(content, "F1", 8, x + 10, y + 31, label);
        pdfText(content, "F2", 13, x + 10, y + 14, value);
    }

    private void drawTableHeader(StringBuilder content, int x, int y) {
        content.append("0.92 0.94 0.96 rg ").append(x).append(' ').append(y).append(" 510 22 re f\n");
        pdfText(content, "F2", 8, x + 8, y + 8, "Month");
        pdfText(content, "F2", 8, x + 86, y + 8, "Total");
        pdfText(content, "F2", 8, x + 154, y + 8, "Admission");
        pdfText(content, "F2", 8, x + 230, y + 8, "Membership");
        pdfText(content, "F2", 8, x + 318, y + 8, "Due");
        pdfText(content, "F2", 8, x + 382, y + 8, "Active");
        pdfText(content, "F2", 8, x + 448, y + 8, "Overdue");
    }

    private void drawTableRow(StringBuilder content, int x, int y, FinancialReportMonthResponse row) {
        content.append("0.82 0.86 0.90 RG ").append(x).append(' ').append(y - 5).append(" 510 1 re f\n");
        pdfText(content, "F1", 8, x + 8, y + 7, row.label());
        pdfText(content, "F1", 8, x + 86, y + 7, moneyLabel(row.totalRevenue()));
        pdfText(content, "F1", 8, x + 154, y + 7, moneyLabel(row.admissionRevenue()));
        pdfText(content, "F1", 8, x + 230, y + 7, moneyLabel(row.membershipRevenue()));
        pdfText(content, "F1", 8, x + 318, y + 7, moneyLabel(row.duePayments()));
        pdfText(content, "F1", 8, x + 382, y + 7, String.valueOf(row.activeMembers()));
        pdfText(content, "F1", 8, x + 448, y + 7, String.valueOf(row.overdueMembers()));
    }

    private void pdfText(StringBuilder content, String font, int size, int x, int y, String text) {
        content.append("BT /")
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
