package com.example.pakpay.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    private String senderMobile;
    private String receiverMobile;
    private BigDecimal amount;
}