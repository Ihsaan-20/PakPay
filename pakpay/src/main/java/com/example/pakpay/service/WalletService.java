package com.example.pakpay.service;

import com.example.pakpay.dto.TransactionHistoryDTO;
import com.example.pakpay.entity.Transaction;
import com.example.pakpay.entity.TransactionStatus;
import com.example.pakpay.entity.User;
import com.example.pakpay.entity.UserLimit;
import com.example.pakpay.entity.Wallet;
import com.example.pakpay.repository.TransactionRepository;
import com.example.pakpay.repository.UserLimitRepository;
import com.example.pakpay.repository.UserRepository;
import com.example.pakpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;



@Service
@Slf4j
@RequiredArgsConstructor // Automatically injects final fields
public class WalletService {

    private final WalletRepository walletRepo;
    private final TransactionRepository transactionRepo;
    private final UserRepository userRepo;
    private final UserLimitRepository limitRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    public Optional<User> findByMobileNumber(String mobileNumber) {
        return userRepo.findByMobileNumber(mobileNumber);
    }
    
    @Transactional
    public String secureTransfer(String senderMobile, String receiverMobile, BigDecimal amount, String rawPin, String idempotencyKey) {
        
    	// . IDEMPOTENCY CHECK (Sab se pehle)
        Optional<Transaction> existingTxn = transactionRepo.findByIdempotencyKey(idempotencyKey);
        if (existingTxn.isPresent()) {
            return "Duplicate Request! Transaction already successful. Trx ID: " + existingTxn.get().getTrxId();
        }
        
        // . PIN Check & Sender Validation
        User sender = userRepo.findByMobileNumber(senderMobile)
                .orElseThrow(() -> new RuntimeException("Sender account nahi mila!"));

        if (sender.getTransactionPin() == null) {
            throw new RuntimeException("Pehle Transaction PIN set karein!");
        }

        if (!passwordEncoder.matches(rawPin, sender.getTransactionPin())) {
            throw new RuntimeException("Ghalat Transaction PIN dala hai!");
        }

        // . Wallet IDs nikalna (Using custom query in WalletRepository)
        Wallet senderWallet = walletRepo.findByMobile(senderMobile)
                .orElseThrow(() -> new RuntimeException("Sender ka wallet nahi mila!"));
        
        Wallet receiverWallet = walletRepo.findByMobile(receiverMobile)
                .orElseThrow(() -> new RuntimeException("Receiver ka wallet nahi mila!"));

        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new RuntimeException("Apne hi wallet mein paise nahi bhej sakte!");
        }

        // . Balance Check (BigDecimal)
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Bhai balance kam hai! Current: " + senderWallet.getBalance());
        }

        // . Daily Limit Check (50,000 PKR)
        UserLimit limit = limitRepo.findByMobileNumber(senderMobile)
                .orElse(UserLimit.builder()
                        .mobileNumber(senderMobile)
                        .dailySpent(BigDecimal.ZERO)
                        .lastTransactionDate(LocalDate.now())
                        .build());

        // Date check: Agar naya din hai to limit reset karo
        if (!limit.getLastTransactionDate().equals(LocalDate.now())) {
            limit.setDailySpent(BigDecimal.ZERO);
            limit.setLastTransactionDate(LocalDate.now());
        }

        BigDecimal totalSpentToday = limit.getDailySpent().add(amount);
        BigDecimal maxLimit = new BigDecimal("50000.00");

        if (totalSpentToday.compareTo(maxLimit) > 0) {
            BigDecimal remaining = maxLimit.subtract(limit.getDailySpent());
            throw new RuntimeException("Daily limit cross ho gayi! Baqi limit: " + remaining);
        }

        // . Money Transfer (Actual Balance Updates)
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

        // . Transaction Table Entry (Aapki actual Entity ke mutabiq)
        Transaction txn = new Transaction();
        String uniqueTrxId = "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        txn.setTrxId(uniqueTrxId);
        txn.setIdempotencyKey(idempotencyKey);
        txn.setSenderWalletId(senderWallet.getId());
        txn.setReceiverWalletId(receiverWallet.getId());
        txn.setAmount(amount);
        txn.setStatus(TransactionStatus.SUCCESS); // ENUM: SUCCESS
        txn.setType("WALLET_TRANSFER");
        txn.setCreatedAt(LocalDateTime.now());

        // . Limit Update
        limit.setDailySpent(totalSpentToday);

        // . DB Saves
        walletRepo.save(senderWallet);
        walletRepo.save(receiverWallet);
        limitRepo.save(limit);
        transactionRepo.save(txn);
        
        // Sender ko alert bhejna
        if (sender.getEmail() != null) {
            emailService.sendTransactionAlert(sender.getEmail(), "DEBIT", amount, uniqueTrxId);
        }

        // Receiver ko alert bhejna
        User receiver = userRepo.findByMobileNumber(receiverMobile).orElse(null);
        if (receiver != null && receiver.getEmail() != null) {
            emailService.sendTransactionAlert(receiver.getEmail(), "CREDIT", amount, uniqueTrxId);
        }

        return "Mubarak ho! " + amount + " PKR bhej diye gaye. Trx ID: " + uniqueTrxId;
    }
    
    public BigDecimal getBalanceByMobile(String mobile) {
        // 1. Pehle user dhundo
        User user = userRepo.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("User not found with mobile: " + mobile));
        
        // 2. User ki ID se wallet dhundo
        Wallet wallet = walletRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for this user"));
        
        return wallet.getBalance();
    }
    
    public List<TransactionHistoryDTO> getTransactionHistory(String mobileNumber) {
    	
    	
        User user = userRepo.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Hamare repository ka method: find by sender OR receiver
        List<Transaction> transactions = transactionRepo.findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
                user.getId(), user.getId());

        return transactions.stream().map(trx -> TransactionHistoryDTO.builder()
                .trxId(trx.getTrxId())
                .type(trx.getSenderWalletId().equals(user.getId()) ? "SENT" : "RECEIVED")
                .amount(trx.getAmount())
                .status(trx.getStatus().toString())
                .date(trx.getCreatedAt())
                .otherPartyMobile(trx.getSenderWalletId().equals(user.getId()) ? "To Account" : "From Account")
                .build()).collect(Collectors.toList());
    }
    
    public String transferFundsByMobile(String senderMobile, String receiverMobile, BigDecimal amount) {
        // 1. Mobile se User dhundo, User se Wallet ID
        User sender = userRepo.findByMobileNumber(senderMobile)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepo.findByMobileNumber(receiverMobile)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // 2. Ab wahi purana transfer logic call karo
        return transferFunds(sender.getId(), receiver.getId(), amount);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public String transferFunds(Long senderId, Long receiverId, BigDecimal amount) {
        log.info("[START] Transfer: From User {} to User {} | Amount: {}", senderId, receiverId, amount);

        // 1. Fetch & Lock (Optional for now)
        Wallet sender = walletRepo.findByUserId(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        Wallet receiver = walletRepo.findByUserId(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // 2. Validate
        if (sender.getBalance().compareTo(amount) < 0) {
            log.error("[FAILED] Low balance for User: {}", senderId);
            throw new RuntimeException("Incomplete transaction: Low Balance");
        }

        // 3. Update Balances
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        walletRepo.save(sender);
        walletRepo.save(receiver);

        // 4. Record Transaction (The Ledger)
        Transaction txn = new Transaction();
        String uniqueTrxId = "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Transaction trx = new Transaction();
        trx.setTrxId(uniqueTrxId);
        trx.setSenderWalletId(senderId);
        trx.setReceiverWalletId(receiverId);
        trx.setAmount(amount);
        trx.setStatus(TransactionStatus.SUCCESS); // Assuming it always succeeds for now
        transactionRepo.save(trx);

        log.info("[SUCCESS] Trx ID: {}", trx.getTrxId());
        return trx.getTrxId();
    }
}