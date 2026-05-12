package com.example.pakpay.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLimit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String mobileNumber;
    @Column(precision = 19, scale = 2)
    private BigDecimal dailySpent; // Aaj kitne paise bhej diye
    
    private LocalDate lastTransactionDate; // Date track karne ke liye
    
    public static final Double DAILY_MAX_LIMIT = 50000.0; // 50,000 PKR limit
}