package com.example.pakpay.dto;

import java.math.BigDecimal;

public record AuthResponse(
	    String token,
	    String refreshToken,
	    String email,
	    String fullName,
	    String mobileNumber,
	    BigDecimal currentBalance,
	    String walletAccountNumber,
	    String isPinSet
	) {}
