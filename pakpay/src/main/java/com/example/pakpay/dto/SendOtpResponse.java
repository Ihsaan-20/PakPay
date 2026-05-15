package com.example.pakpay.dto;

public record SendOtpResponse(
    Long otpRequestId,
    String message,
    String demoOtp
) {}
