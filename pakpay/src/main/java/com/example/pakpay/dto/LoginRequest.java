package com.example.pakpay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(03\\d{9}|92\\d{10})$", message = "Mobile must be 03XXXXXXXXX (11 digits)")
    String mobileNumber,

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    String password
) {}
