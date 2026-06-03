package com.gymflow.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "platform_settings")
public class PlatformSetting extends BaseEntity {

    @Column(nullable = false)
    private boolean referralsEnabled = true;

}