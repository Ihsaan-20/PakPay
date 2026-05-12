package com.example.pakpay.dto;

import java.math.BigDecimal;

public record UserWalletDTO(
    String fullName,
    String mobileNumber,
    String email,
    String transactionPin,
    String walletAccountNumber,
    BigDecimal balance
) {}