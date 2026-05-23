package com.gymflow.service;

import com.gymflow.domain.MemberMembership;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrganizationDigestBuilderService {

    public DigestContent buildDigest(Organization organization, OrganizationSetting setting, List<MemberMembership> memberships) {
        String brandingName = hasText(setting.getBrandingName()) ? setting.getBrandingName() : organization.getName();
        String memberList = buildMemberList(memberships);
        String whatsapp = buildWhatsappDigest(memberList, memberships.size());
        String emailSubject = "Membership Renewal Summary";
        String email = buildEmailDigest(brandingName, memberships);
        return new DigestContent(brandingName, whatsapp, emailSubject, email, memberships.size(), memberList);
    }

    private String buildWhatsappDigest(String memberList, int memberCount) {
        StringBuilder builder = new StringBuilder("BillMyGym Alert\n\nExpired or overdue memberships:\n\n");
        builder.append(memberList);
        builder.append("\n\nTotal pending: ").append(memberCount);
        return builder.toString();
    }

    private String buildMemberList(List<MemberMembership> memberships) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < memberships.size(); index++) {
            MemberMembership membership = memberships.get(index);
            builder.append(index + 1)
                .append(". ")
                .append(membership.getMember().getFullName())
                .append(" | ")
                .append(membershipStatusSummary(membership))
                .append('\n');
        }
        return builder.toString().trim();
    }

    private String buildEmailDigest(String brandingName, List<MemberMembership> memberships) {
        StringBuilder listBuilder = new StringBuilder();
        for (MemberMembership membership : memberships) {
            listBuilder.append("<li>")
                .append(membership.getMember().getFullName())
                .append(" - expires ")
                .append(membership.getEndDate())
                .append(" - ")
                .append(membershipStatusSummary(membership))
                .append("</li>");
        }

        return """
            <html><body>
            <h2>%s</h2>
            <p>Expired or overdue memberships needing follow-up:</p>
            <ul>%s</ul>
            </body></html>
            """.formatted(brandingName, listBuilder);
    }

    private String membershipStatusSummary(MemberMembership membership) {
        long overdueDays = Math.max(1, ChronoUnit.DAYS.between(membership.getEndDate(), LocalDate.now()));
        return "overdue by " + overdueDays + " day" + (overdueDays == 1 ? "" : "s");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record DigestContent(
        String brandingName,
        String whatsappMessage,
        String emailSubject,
        String emailMessage,
        int memberCount,
        String memberList
    ) {
        public DigestContent(String brandingName, String whatsappMessage, String emailSubject, String emailMessage, int memberCount) {
            this(brandingName, whatsappMessage, emailSubject, emailMessage, memberCount, "");
        }
    }
}

