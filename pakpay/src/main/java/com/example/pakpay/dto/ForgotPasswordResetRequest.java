package com.example.pakpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordResetRequest(
    @NotBlank(message = "Mobile number zaroori hai.")
    @Pattern(regexp = "^(03\\d{9}|92\\d{10})$", message = "Mobile 03XXXXXXXXX format mein hona chahiye.")
    String mobileNumber,

    @NotBlank(message = "OTP code zaroori hai.")
    @Size(min = 6, max = 6, message = "OTP 6 digits ka hona chahiye.")
    String otpCode,

    @NotBlank(message = "Naya password zaroori hai.")
    @Size(min = 6, max = 100, message = "Password kam az kam 6 characters ka ho.")
    String newPassword
) {}
