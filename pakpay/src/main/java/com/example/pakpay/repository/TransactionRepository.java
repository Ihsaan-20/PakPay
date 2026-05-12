package com.example.pakpay.repository;

import com.example.pakpay.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Resume point: User ki transaction history nikalne ke liye
    List<Transaction> findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(Long senderId, Long receiverId);
    
    // Specific transaction track karne ke liye
    Transaction findByTrxId(String trxId);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}