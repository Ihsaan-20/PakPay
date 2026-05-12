package com.example.pakpay.repository;

import com.example.pakpay.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByAccessToken(String token);
    List<UserToken> findAllValidTokensByMobileNumber(String mobileNumber); 
    Optional<UserToken> findByRefreshToken(String refreshToken);
    // Iska query hum custom likh sakte hain ya naming convention use karenge
}