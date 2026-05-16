package com.gymflow.service;

import com.gymflow.api.dto.SettingDtos.SettingsResponse;
import com.gymflow.api.dto.SettingDtos.UpdateSettingsRequest;
import com.gymflow.api.error.BadRequestException;
import com.gymflow.api.error.NotFoundException;
import com.gymflow.domain.Organization;
import com.gymflow.domain.OrganizationSetting;
import com.gymflow.domain.enums.AuditCategory;
import com.gymflow.repository.OrganizationRepository;
import com.gymflow.repository.OrganizationSettingRepository;
import com.gymflow.security.AuthenticatedUser;
import com.gymflow.security.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationSettingService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationSettingRepository settingRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SettingsResponse getCurrentSettings() {
        OrganizationSetting setting = getOrCreateSettings(requireOrgId());
        return toResponse(setting);
    }

    @Transactional
    @CacheEvict(value = {"dashboardReports", "memberReports", "revenueReports"}, allEntries = true)
    public SettingsResponse updateCurrentSettings(UpdateSettingsRequest request) {
        Long organizationId = requireOrgId();
        OrganizationSetting setting = getOrCreateSettings(organizationId);

        if (request.whatsappEnabled() != null) {
            setting.setWhatsappEnabled(request.whatsappEnabled());
        }
        if (request.emailEnabled() != null) {
            setting.setEmailEnabled(request.emailEnabled());
        }
        if (request.reminderDaysBeforeExpiry() != null) {
            setting.setReminderDaysBeforeExpiry(request.reminderDaysBeforeExpiry());
        }
        if (request.brandingName() != null) {
            setting.setBrandingName(request.brandingName());
        }
        if (request.monthlyMembershipFee() != null) {
            setting.getOrganization().setMonthlyMembershipFee(request.monthlyMembershipFee());
        }

        auditLogService.log(
            "SETTINGS_UPDATED",
            "Updated notification settings for organization id=" + organizationId,
            AuditCategory.ORGANIZATION,
            "SETTINGS"
        );
        return toResponse(setting);
    }

    @Transactional
    public OrganizationSetting getOrCreateSettings(Long organizationId) {
        return settingRepository.findByOrganizationIdAndDeletedFalse(organizationId)
            .orElseGet(() -> {
                Organization organization = organizationRepository.findByIdAndDeletedFalse(organizationId)
                    .orElseThrow(() -> new NotFoundException("Organization not found"));
                OrganizationSetting setting = new OrganizationSetting();
                setting.setOrganization(organization);
                setting.setBrandingName(organization.getName());
                return settingRepository.save(setting);
            });
    }

    private SettingsResponse toResponse(OrganizationSetting setting) {
        return new SettingsResponse(
            setting.isWhatsappEnabled(),
            setting.isEmailEnabled(),
            setting.getReminderDaysBeforeExpiry(),
            setting.getBrandingName(),
            setting.getOrganization().getMonthlyMembershipFee()
        );
    }

    private Long requireOrgId() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        if (user.organizationId() == null) {
            throw new BadRequestException("Organization context is required");
        }
        return user.organizationId();
    }
}
