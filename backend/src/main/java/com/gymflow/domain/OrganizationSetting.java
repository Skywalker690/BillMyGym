package com.gymflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "organization_settings")
public class OrganizationSetting extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(nullable = false)
    private boolean whatsappEnabled = true;

    @Column(nullable = false)
    private boolean emailEnabled = true;

    @Column(nullable = false)
    private int reminderDaysBeforeExpiry = 1;

    private String brandingName;

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public boolean isWhatsappEnabled() {
        return whatsappEnabled;
    }

    public void setWhatsappEnabled(boolean whatsappEnabled) {
        this.whatsappEnabled = whatsappEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public int getReminderDaysBeforeExpiry() {
        return reminderDaysBeforeExpiry;
    }

    public void setReminderDaysBeforeExpiry(int reminderDaysBeforeExpiry) {
        this.reminderDaysBeforeExpiry = reminderDaysBeforeExpiry;
    }

    public String getBrandingName() {
        return brandingName;
    }

    public void setBrandingName(String brandingName) {
        this.brandingName = brandingName;
    }
}
