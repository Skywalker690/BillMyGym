package com.gymflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "organization_settings", indexes = {
    @Index(name = "idx_org_settings_org_deleted", columnList = "organization_id, deleted")
})
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
}
