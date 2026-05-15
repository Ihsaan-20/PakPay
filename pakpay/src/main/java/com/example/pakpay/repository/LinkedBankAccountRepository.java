package com.example.pakpay.repository;

import com.example.pakpay.entity.LinkedBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkedBankAccountRepository extends JpaRepository<LinkedBankAccount, Long> {

    List<LinkedBankAccount> findByMobileNumberAndActiveTrueOrderByLinkedAtDesc(String mobileNumber);

    Optional<LinkedBankAccount> findByIdAndMobileNumberAndActiveTrue(Long id, String mobileNumber);

    boolean existsByMobileNumberAndBankCodeAndAccountNumberAndActiveTrue(
            String mobileNumber, String bankCode, String accountNumber);
}
