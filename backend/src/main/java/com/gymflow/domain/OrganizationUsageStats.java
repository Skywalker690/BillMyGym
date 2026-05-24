package com.gymflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.YearMonth;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "organization_usage_stats",
    indexes = {
        @Index(name = "idx_org_usage_stats_month", columnList = "year, month, deleted"),
        @Index(name = "idx_org_usage_stats_org_month", columnList = "organization_id, year, month, deleted")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uk_org_usage_stats_org_year_month",
        columnNames = {"organization_id", "year", "month"}
    )
)
public class OrganizationUsageStats extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(name = "whatsapp_sent_count", nullable = false)
    private int whatsappSentCount;

    @Column(name = "email_sent_count", nullable = false)
    private int emailSentCount;

    public static OrganizationUsageStats empty(Organization organization, YearMonth yearMonth) {
        OrganizationUsageStats stats = new OrganizationUsageStats();
        stats.organization = organization;
        stats.year = yearMonth.getYear();
        stats.month = yearMonth.getMonthValue();
        return stats;
    }

    public void incrementWhatsappSentCount() {
        whatsappSentCount++;
    }

    public void incrementEmailSentCount() {
        emailSentCount++;
    }
}
