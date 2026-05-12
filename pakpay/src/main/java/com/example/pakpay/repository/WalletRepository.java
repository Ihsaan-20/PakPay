package com.example.pakpay.repository;


import com.example.pakpay.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // Custom method to find wallet by User ID
    Optional<Wallet> findByUserId(Long userId);
    
    // Custom JPQL Query: Wallet ke userId ko User table ki id se join kar ke mobile match karega
    @Query("SELECT w FROM Wallet w JOIN User u ON w.userId = u.id WHERE u.mobileNumber = :mobile")
    Optional<Wallet> findByMobile(@Param("mobile") String mobile);

}
