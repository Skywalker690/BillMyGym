package com.gymflow.service;

import com.gymflow.api.dto.AuthDtos.LegalPolicyMetadataResponse;
import com.gymflow.domain.User;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class LegalPolicyService {

    public static final String CURRENT_POLICY_VERSION = "v1.0";
    public static final LocalDate CURRENT_EFFECTIVE_DATE = LocalDate.of(2026, 6, 1);
    public static final String APPLICATION_NAME = "BillMyGym";
    public static final String SUPPORT_EMAIL = "support@billmygym.com";
    public static final String LEGAL_EMAIL = "legal@billmygym.com";
    public static final String PRIVACY_EMAIL = "privacy@billmygym.com";
    public static final String COMPANY_ADDRESS = "BillMyGym, Kakkanad, Ernakulam, Kerala, India";

    public boolean requiresConsent(User user) {
        if (user == null) {
            return true;
        }
        return !Boolean.TRUE.equals(user.getTermsAccepted())
            || !Boolean.TRUE.equals(user.getPrivacyAccepted())
            || !CURRENT_POLICY_VERSION.equals(user.getPolicyVersion());
    }

    public String consentMessage(User user) {
        if (user != null && user.getPolicyVersion() != null && !CURRENT_POLICY_VERSION.equals(user.getPolicyVersion())) {
            return "Our legal documents have been updated. Please review and accept the latest version to continue.";
        }
        return "Please review and accept our Terms & Conditions and Privacy Policy to access the application.";
    }

    public LegalPolicyMetadataResponse metadata() {
        return new LegalPolicyMetadataResponse(
            APPLICATION_NAME,
            CURRENT_POLICY_VERSION,
            CURRENT_EFFECTIVE_DATE,
            SUPPORT_EMAIL,
            LEGAL_EMAIL,
            PRIVACY_EMAIL,
            COMPANY_ADDRESS
        );
    }
}
