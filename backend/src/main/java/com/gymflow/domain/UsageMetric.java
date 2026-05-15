package com.gymflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

@Entity
@Getter
@Setter
@Table(name = "usage_metrics")
public class UsageMetric extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int whatsappMessagesSent;

    @Column(nullable = false)
    private int emailsSent;

    @Column(nullable = false)
    private int activeMembers;

    @Column(nullable = false)
    private int activeAdmins;

    @Column(nullable = false)
    private String subscriptionStatus;

    public static UsageMetric empty(Organization organization, YearMonth ym) {
        UsageMetric metric = new UsageMetric();
        metric.organization = organization;
        metric.year = ym.getYear();
        metric.month = ym.getMonthValue();
        metric.whatsappMessagesSent = 0;
        metric.emailsSent = 0;
        metric.activeMembers = 0;
        metric.activeAdmins = 0;
        metric.subscriptionStatus = "ACTIVE";
        return metric;
    }

    public void incrementWhatsappMessagesSent() {
        this.whatsappMessagesSent++;
    }

}
