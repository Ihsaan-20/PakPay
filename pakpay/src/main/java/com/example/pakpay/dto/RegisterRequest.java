package com.example.pakpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    String fullName,

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(03\\d{9}|92\\d{10})$", message = "Mobile must be 03XXXXXXXXX (11 digits)")
    String mobileNumber,

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    String password,

    @NotBlank(message = "CNIC is required")
    @Pattern(regexp = "^(\\d{13}|\\d{5}-\\d{7}-\\d)$", message = "CNIC must be 13 digits (e.g. 12345-1234567-1)")
    String cnic
) {}
