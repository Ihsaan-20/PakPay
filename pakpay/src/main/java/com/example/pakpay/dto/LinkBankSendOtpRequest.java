package com.example.pakpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LinkBankSendOtpRequest(
    @NotBlank(message = "Bank is required")
    String bankCode,

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^\\d{10,20}$", message = "Account number must be 10-20 digits")
    String accountNumber,

    @NotBlank(message = "Account title is required")
    @Size(min = 3, max = 120, message = "Account title must be 3-120 characters")
    String accountTitle
) {}
