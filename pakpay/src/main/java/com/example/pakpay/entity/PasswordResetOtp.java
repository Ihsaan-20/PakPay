package com.example.pakpay.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_otps")
@Data
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "invalid_attempts", nullable = false)
    private int invalidAttempts = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
