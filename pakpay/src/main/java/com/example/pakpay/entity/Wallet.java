package com.example.pakpay.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Data
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    private String currency = "PKR";
    
    @Column(unique = true)
    private String walletAccountNumber; // e.g. PK-PAY-100234

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "ENUM('ACTIVE', 'FROZEN', 'CLOSED')")
    private WalletStatus status = WalletStatus.ACTIVE;
}