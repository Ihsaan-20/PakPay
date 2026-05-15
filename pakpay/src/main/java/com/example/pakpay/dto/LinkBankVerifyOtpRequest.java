package com.example.pakpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LinkBankVerifyOtpRequest(
    @NotNull(message = "OTP request id is required")
    Long otpRequestId,

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    String otp
) {}
