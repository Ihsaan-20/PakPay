package com.example.pakpay.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trx_id", unique = true, nullable = false)
    private String trxId;

    private Long senderWalletId;
    private Long receiverWalletId;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('PENDING', 'SUCCESS', 'FAILED')")
    private TransactionStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Aapke SQL mein 'type' column bhi hai, usay bhi add kar dein warna error ayega
    @Column(name = "type", columnDefinition = "ENUM('WALLET_TRANSFER', 'IBFT', 'BILL_PAYMENT')")
    private String type = "WALLET_TRANSFER";
}