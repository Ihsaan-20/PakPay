package com.example.pakpay.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "linked_bank_accounts")
@Data
public class LinkedBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 80)
    private String bankName;

    @Column(name = "logo_key", length = 40)
    private String logoKey;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "account_title", nullable = false, length = 120)
    private String accountTitle;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt = LocalDateTime.now();
}
