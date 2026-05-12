package com.example.pakpay.dto;

public record LoginRequest(
    String mobileNumber, // User ab email ki jagah number daalega
    String password
) {}