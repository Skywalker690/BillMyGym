package com.gymflow.domain;

import com.gymflow.domain.enums.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String district;

    private String logoUrl;
    private String mapLocationLink;
    private String address;

    @Column(nullable = false)
    private String adminFullName;

    @Column(nullable = false)
    private String adminPhoneNumber;

    @Column(nullable = false)
    private String adminEmail;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

}
