package com.example.pakpay.dto;

public record RegisterRequest(
    String fullName,
    String mobileNumber,
    String password,
    String cnic
) {}