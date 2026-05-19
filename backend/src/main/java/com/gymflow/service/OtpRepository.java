package com.gymflow.service;

import com.gymflow.domain.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByEmailAndOtpCodeAndVerifiedFalseAndExpiresAtAfter(
            String email, String otpCode, Instant currentTime);
    
    void deleteByEmail(String email);
    
    void deleteByExpiresAtBefore(Instant currentTime);
}
