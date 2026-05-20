package com.example.pakpay.repository;

import com.example.pakpay.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findTopByMobileNumberAndVerifiedTrueOrderByCreatedAtDesc(String mobileNumber);

    int countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime after);

    Optional<PasswordResetOtp> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);
}
