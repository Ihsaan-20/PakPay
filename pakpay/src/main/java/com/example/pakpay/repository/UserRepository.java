package com.example.pakpay.repository;


import com.example.pakpay.dto.UserWalletDTO;
import com.example.pakpay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMobileNumber(String mobileNumber);
    Optional<User> findByEmail(String email);
    Optional<User> findByCnicEncrypted(String cnicEncrypted);
    boolean existsByMobileNumber(String mobileNumber);
    boolean existsByCnicEncrypted(String cnicEncrypted);
   
    
    @Query("SELECT new com.example.pakpay.dto.UserWalletDTO(u.fullName, u.mobileNumber, u.email, u.transactionPin, w.walletAccountNumber, w.balance) " +
    	       "FROM User u JOIN Wallet w ON u.id = w.userId " +
    	       "WHERE u.mobileNumber = :mobileNumber")
    	Optional<UserWalletDTO> findUserAndWalletDetails(@Param("mobileNumber") String mobileNumber);
}
