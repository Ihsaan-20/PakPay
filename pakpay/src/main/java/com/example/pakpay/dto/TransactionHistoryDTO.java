package com.example.pakpay.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionHistoryDTO {
    private String trxId;
    private String type;
    private BigDecimal amount;
    private String status;
    private LocalDateTime date;
    private String otherPartyMobile; // Kis ko bheje ya kis se aaye
}