package com.example.pakpay.dto;

public record LinkedBankAccountDto(
    Long id,
    String bankCode,
    String bankName,
    String logoKey,
    String accountNumberMasked,
    String accountTitle
) {}
