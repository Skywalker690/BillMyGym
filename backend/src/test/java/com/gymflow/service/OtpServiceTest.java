package com.gymflow.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gymflow.domain.Otp;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @Test
    void verifyDoesNotConsumeOtp() {
        Otp otp = otp("123456");
        when(otpRepository.findByEmailAndOtpCodeAndVerifiedFalseAndExpiresAtAfter(
            eq("admin@gymflow.local"),
            eq("123456"),
            any(LocalDateTime.class)
        )).thenReturn(Optional.of(otp));

        OtpService otpService = new OtpService(otpRepository, emailService);

        assertTrue(otpService.isOtpValid("admin@gymflow.local", "123456"));
        assertFalse(Boolean.TRUE.equals(otp.getVerified()));
    }

    @Test
    void consumeMarksOtpAsUsed() {
        Otp otp = otp("654321");
        when(otpRepository.findByEmailAndOtpCodeAndVerifiedFalseAndExpiresAtAfter(
            eq("admin@gymflow.local"),
            eq("654321"),
            any(LocalDateTime.class)
        )).thenReturn(Optional.of(otp));

        OtpService otpService = new OtpService(otpRepository, emailService);

        assertTrue(otpService.consumeOtp("admin@gymflow.local", "654321"));
        assertTrue(Boolean.TRUE.equals(otp.getVerified()));
    }

    @Test
    void generateAndSendOtpPersistsAndEmailsCode() {
        OtpService otpService = new OtpService(otpRepository, emailService);

        otpService.generateAndSendOtp("admin@gymflow.local");

        verify(otpRepository).deleteByEmail("admin@gymflow.local");
        verify(otpRepository).save(any(Otp.class));
        verify(emailService).sendOtpEmail(
            eq("admin@gymflow.local"),
            matches("\\d{6}")
        );
    }

    private Otp otp(String code) {
        return Otp.builder()
            .email("admin@gymflow.local")
            .otpCode(code)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .verified(false)
            .build();
    }
}
