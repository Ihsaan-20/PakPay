package com.example.pakpay.repository;

import com.example.pakpay.entity.UserLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserLimitRepository extends JpaRepository<UserLimit, Long> {
    Optional<UserLimit> findByMobileNumber(String mobileNumber);
}