package com.example.pakpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordSendOtpRequest(
    @NotBlank(message = "Mobile number zaroori hai.")
    @Pattern(regexp = "^(03\\d{9}|92\\d{10})$", message = "Mobile 03XXXXXXXXX format mein hona chahiye.")
    String mobileNumber
) {}
