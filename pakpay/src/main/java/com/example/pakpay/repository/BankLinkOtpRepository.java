package com.example.pakpay.repository;

import com.example.pakpay.entity.BankLinkOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankLinkOtpRepository extends JpaRepository<BankLinkOtp, Long> {

    Optional<BankLinkOtp> findByIdAndMobileNumber(Long id, String mobileNumber);
}
